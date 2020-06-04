/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.metrics.Metrics;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import eu.cessda.pasc.oci.service.IngestService;
import eu.cessda.pasc.oci.service.helpers.DebuggingJMXBean;
import eu.cessda.pasc.oci.service.helpers.LanguageAvailabilityMapper;
import eu.cessda.pasc.oci.service.helpers.LanguageDocumentExtractor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.value;

/**
 * Service responsible for triggering Metadata Harvesting and ingestion into the search engine
 *
 * @author moses AT doraventures DOT com
 */
@Component
@ManagedResource
@Slf4j
public class ConsumerScheduler {

  private static final String FULL_RUN = "Full Run";
  private static final String DAILY_INCREMENTAL_RUN = "Daily Incremental Run";
  private static final String REPO_NAME = "repo_name";
  private static final String LANG_CODE = "lang_code";
  private static final String REPO_ENDPOINT_URL = "repo_endpoint_url";
  private static final String DEFAULT_CDC_JOB_KEY = "indexer_job_id";
  private static final String DEFAULT_RESPONSE_TOKEN_HEADER = "cdc-";

  private final DebuggingJMXBean debuggingJMXBean;
  private final AppConfigurationProperties configurationProperties;
  private final HarvesterConsumerService harvesterConsumerService;
  private final IngestService esIndexerService;
  private final LanguageDocumentExtractor extractor;
  private final LanguageAvailabilityMapper languageAvailabilityMapper;
  private final Metrics metrics;

  private final AtomicBoolean indexerRunning = new AtomicBoolean(false);

  @Autowired
  public ConsumerScheduler(DebuggingJMXBean debuggingJMXBean, AppConfigurationProperties configurationProperties,
                           HarvesterConsumerService harvesterConsumerService, IngestService esIndexerService,
                           LanguageDocumentExtractor extractor, LanguageAvailabilityMapper languageAvailabilityMapper,
                           Metrics metrics) {
    this.debuggingJMXBean = debuggingJMXBean;
    this.configurationProperties = configurationProperties;
    this.harvesterConsumerService = harvesterConsumerService;
    this.esIndexerService = esIndexerService;
    this.extractor = extractor;
    this.languageAvailabilityMapper = languageAvailabilityMapper;
    this.metrics = metrics;
  }

  /**
   * Auto Starts after delay of given time at startup.
   */
  @ManagedOperation(description = "Manual trigger to do a full harvest and ingest run")
  @Scheduled(initialDelayString = "${osmhConsumer.delay.initial}", fixedDelayString = "${osmhConsumer.delay.fixed}")
  public void fullHarvestAndIngestionAllConfiguredSPsReposRecords() {
    try (var ignored = MDC.putCloseable(ConsumerScheduler.DEFAULT_CDC_JOB_KEY, getJobId())) {
      OffsetDateTime date = OffsetDateTime.now(ZoneId.systemDefault());
      logStartStatus(date, FULL_RUN);
      executeHarvestAndIngest(null);
      logEndStatus(date, FULL_RUN);
    }
  }

  /**
   * Daily Harvest and Ingestion run.
   */
  @ManagedOperation(description = "Manual trigger to do an incremental harvest and ingest")
  @Scheduled(cron = "${osmhConsumer.daily.run}")
  public void dailyIncrementalHarvestAndIngestionAllConfiguredSPsReposRecords() {
    try (var ignored = MDC.putCloseable(ConsumerScheduler.DEFAULT_CDC_JOB_KEY, getJobId())) {
      OffsetDateTime date = OffsetDateTime.now(ZoneId.systemDefault());
      logStartStatus(date, DAILY_INCREMENTAL_RUN);
      executeHarvestAndIngest(esIndexerService.getMostRecentLastModified().orElse(null));
      logEndStatus(date, DAILY_INCREMENTAL_RUN);
    }
  }

  /**
   * Weekly run.
   */
  @Scheduled(cron = "${osmhConsumer.daily.sunday.run}")
  public void weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords() {
    log.info("Once a Week Full Run. Triggered by cron - STARTED");
    fullHarvestAndIngestionAllConfiguredSPsReposRecords();
    log.info("Once a Week Full Run. Triggered by cron - ENDED");
  }

  /**
   * Gets the correlation id of this run
   *
   * @return the correlation id
   */
  private String getJobId() {
    return DEFAULT_RESPONSE_TOKEN_HEADER + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneId.systemDefault()));
  }

  /**
   * Starts the harvest.
   *
   * @param lastModifiedDateTime the DateTime to incrementally harvest from, set to null to perform a full harvest.
   * @throws IllegalStateException if a harvest is already running.
   */
  private void executeHarvestAndIngest(LocalDateTime lastModifiedDateTime) {
    if (!indexerRunning.getAndSet(true)) {
      try {
        List<Repo> repos = configurationProperties.getEndpoints().getRepos();

        // Store the MDC so that it can be used in the running thread
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        repos.parallelStream().forEach(repo -> {
          MDC.setContextMap(contextMap);

          // Set the MDC so that the record name is attached to all downstream logs
          try (var ignored = MDC.putCloseable(REPO_NAME, repo.getName())) {
            Map<String, List<CMMStudyOfLanguage>> langStudies = getCmmStudiesOfEachLangIsoCodeMap(repo, lastModifiedDateTime);
            langStudies.forEach((langIsoCode, cmmStudies) -> executeBulk(repo, langIsoCode, cmmStudies));
          }

          // Reset the MDC
          MDC.clear();
        });

        // Reset the context map - this is needed if the thread was reused
        MDC.setContextMap(contextMap);

        log.info("Total number of records is {}", value("total_cmm_studies", esIndexerService.getTotalHitCount("*")));
      } finally {
        // Ensure that the running state is always set to false even if an exception is thrown
        indexerRunning.set(false);
      }
    } else {
      throw new IllegalStateException("Indexer is already running");
    }
    metrics.updateMetrics();
  }


  /**
   * Index the given CMMStudies into the Elasticsearch index.
   *
   * @param repo        the source repository.
   * @param langIsoCode the language code.
   * @param cmmStudies  the studies to index.
   */
  private void executeBulk(Repo repo, String langIsoCode, List<CMMStudyOfLanguage> cmmStudies) {
    if (!cmmStudies.isEmpty()) {
      var studiesUpdated = getUpdatedStudies(cmmStudies, langIsoCode);
      log.info("BulkIndexing repo [{}] with lang code [{}].",
              value(REPO_NAME, repo.getName()),
              value(LANG_CODE, langIsoCode));
      if (esIndexerService.bulkIndex(cmmStudies, langIsoCode)) {
        log.info("BulkIndexing was successful for repo name [{}] and its corresponding [{}].  LangIsoCode [{}]." +
                        " [{}] studies created, [{}] studies deleted, [{}] studies updated.",
                value(REPO_NAME, repo.getName()),
                value(REPO_ENDPOINT_URL, repo.getUrl()),
                value(LANG_CODE, langIsoCode),
                value("created_cmm_studies", studiesUpdated.studiesCreated),
                value("deleted_cmm_studies", studiesUpdated.studiesDeleted),
                value("updated_cmm_studies", studiesUpdated.studiesUpdated));
      } else {
        log.error("BulkIndexing was unsuccessful for repo name [{}] and its corresponding [{}].  LangIsoCode [{}].",
                value(REPO_NAME, repo.getName()),
                value(REPO_ENDPOINT_URL, repo.getUrl()),
                value(LANG_CODE, langIsoCode));
      }
    } else {
      log.debug("CmmStudies list is empty. Nothing to BulkIndex for repo [{}] with LangIsoCode [{}].",
              value(REPO_NAME, repo.getName()),
              value(LANG_CODE, langIsoCode));
    }
  }

  /**
   * Compares the collection of studies retrieved from remote repositories to the studies stored in Elasticsearch.
   *
   * @param cmmStudies the list of studies, harvested from remote repositories, to compare
   * @param language   the language of the studies
   * @return a {@link UpdatedStudies} describing the amount of created, deleted and updated studies
   */
  private UpdatedStudies getUpdatedStudies(Collection<CMMStudyOfLanguage> cmmStudies, String language) {

    var studiesCreated = new AtomicInteger(0);
    var studiesDeleted = new AtomicInteger(0);
    var studiesUpdated = new AtomicInteger(0);

    cmmStudies.parallelStream().forEach(remoteStudy -> {
      var esStudyOptional = esIndexerService.getStudy(remoteStudy.getId(), language);

      // If empty then the study didn't exist in Elasticsearch, and will be created
      if (esStudyOptional.isEmpty()) {
        if (remoteStudy.isActive()) {
          studiesCreated.getAndIncrement();
        }
      } else {
        if (!remoteStudy.equals(esStudyOptional.get())) {
          // If not equal
          if (remoteStudy.isActive()) {
            // The study has been deleted
            studiesUpdated.getAndIncrement();
          } else {
            // The study has been updated
            studiesDeleted.getAndIncrement();
          }
        }
      }
    });

    return new UpdatedStudies(studiesCreated.get(), studiesDeleted.get(), studiesUpdated.get());
  }

  @Value
  public static class UpdatedStudies {
    int studiesCreated;
    int studiesDeleted;
    int studiesUpdated;
  }

  private Map<String, List<CMMStudyOfLanguage>> getCmmStudiesOfEachLangIsoCodeMap(Repo repo, LocalDateTime lastModifiedDateTime) {
    log.info("Processing Repo [{}]", repo);
    List<RecordHeader> recordHeaders = harvesterConsumerService.listRecordHeaders(repo, lastModifiedDateTime);

    log.info("Repo [{}] returned with [{}] record headers", repo.getName(), recordHeaders.size());

    List<CMMStudy> presentCMMStudies = recordHeaders.stream()
            .map(recordHeader -> harvesterConsumerService.getRecord(repo, recordHeader.getIdentifier()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    log.info("Repo Name [{}] of [{}] Endpoint. There are [{}] presentCMMStudies out of [{}] totalCMMStudies. Therefore CMMStudies duplicated is [{}]",
            value(REPO_NAME, repo.getName()),
            value(REPO_ENDPOINT_URL, repo.getUrl()),
            value("present_cmm_record", presentCMMStudies.size()),
            value("total_cmm_record", recordHeaders.size()),
            value("cmm_records_duplicated", recordHeaders.size() - presentCMMStudies.size()));

    presentCMMStudies.forEach(languageAvailabilityMapper::setAvailableLanguages);
    return extractor.mapLanguageDoc(presentCMMStudies, repo.getName());
  }

  private void logStartStatus(OffsetDateTime localDateTime, String runDescription) {
    log.info("[{}] Consume and Ingest All SPs Repos : Started at [{}]", runDescription, localDateTime);
    log.info("Currents state before run");
    log.info(debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints());
    log.info(debuggingJMXBean.printElasticSearchInfo());
  }

  private void logEndStatus(OffsetDateTime offsetDateTime, String runDescription) {
    String formatMsg = "[{}] Consume and Ingest All SPs Repos Ended at [{}], Duration [{}]";
    log.info(formatMsg, runDescription, offsetDateTime, value("job_duration", Duration.between(offsetDateTime.toInstant(), Instant.now()).getSeconds()));
  }
}
