package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import eu.cessda.pasc.oci.service.IngestService;
import eu.cessda.pasc.oci.service.helpers.DebuggingJMXBean;
import eu.cessda.pasc.oci.service.helpers.LanguageAvailabilityMapper;
import eu.cessda.pasc.oci.service.helpers.LanguageDocumentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service responsible for triggering Metadata Harvesting and ingestion into the search engine
 *
 * @author moses@doraventures.com
 */
@Component
@ManagedResource
@Slf4j
public class ConsumerScheduler {

  private static final String FULL_RUN = "Full Run";
  private static final String DAILY_INCREMENTAL_RUN = "Daily Incremental Run";
  private DebuggingJMXBean debuggingJMXBean;
  private AppConfigurationProperties configurationProperties;
  private HarvesterConsumerService harvesterConsumerService;
  private IngestService esIndexerService;
  private LanguageDocumentExtractor extractor;
  private LanguageAvailabilityMapper languageAvailabilityMapper;

  @Autowired
  public ConsumerScheduler(DebuggingJMXBean debuggingJMXBean, AppConfigurationProperties configurationProperties,
                           HarvesterConsumerService harvesterConsumerService, IngestService esIndexerService,
                           LanguageDocumentExtractor extractor, LanguageAvailabilityMapper languageAvailabilityMapper) {
    this.debuggingJMXBean = debuggingJMXBean;
    this.configurationProperties = configurationProperties;
    this.harvesterConsumerService = harvesterConsumerService;
    this.esIndexerService = esIndexerService;
    this.extractor = extractor;
    this.languageAvailabilityMapper = languageAvailabilityMapper;
  }

  /**
   * Auto Starts after delay of given time at startup.
   */
  @ManagedOperation(description = "Manual trigger to do a full harvest and ingest run")
  @Scheduled(initialDelayString = "${osmhConsumer.delay.initial}", fixedDelayString = "${osmhConsumer.delay.fixed}")
  public void fullHarvestAndIngestionAllConfiguredSPsReposRecords() {
    Instant startTime = Instant.now();
    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime.toEpochMilli()), ZoneId.systemDefault());
    logStartStatus(date, FULL_RUN);
    executeHarvestAndIngest(null);
    logEndStatus(date, startTime, FULL_RUN);
  }

  /** Use Jenkins job instead

   * Daily Harvest and Ingestion run.

  @ManagedOperation(description = "Manual trigger to do an incremental harvest and ingest")
  @Scheduled(cron = "${osmhConsumer.daily.run}")
  public void dailyIncrementalHarvestAndIngestionAllConfiguredSPsReposRecords() {
    Instant startTime = Instant.now();
    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime.toEpochMilli()), ZoneId.systemDefault());
    logStartStatus(date, DAILY_INCREMENTAL_RUN);
    executeHarvestAndIngest(esIndexerService.getMostRecentLastModified().orElse(null));
    logEndStatus(date, startTime, DAILY_INCREMENTAL_RUN);
  }
  */

  /** Use Jenkins job instead

   * Weekly run.
  @Scheduled(cron = "${osmhConsumer.daily.sunday.run}")
  public void weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords() {
    log.info("Once a Week Full Run. Triggered by cron - STARTED");
    fullHarvestAndIngestionAllConfiguredSPsReposRecords();
    log.info("Once a Week Full Run. Triggered by cron - ENDED");
  }
  */


  private void executeHarvestAndIngest(LocalDateTime lastModifiedDateTime) {
    List<Repo> repos = configurationProperties.getEndpoints().getRepos();
    repos.forEach(repo -> {
      Map<String, List<CMMStudyOfLanguage>> langStudies = getCmmStudiesOfEachLangIsoCodeMap(repo, lastModifiedDateTime);
      langStudies.forEach((langIsoCode, cmmStudies) -> executeBulk(repo, langIsoCode, cmmStudies));
    });
  }


  private void executeBulk(Repo repo, String langIsoCode, List<CMMStudyOfLanguage> cmmStudies) {
    if (cmmStudies.isEmpty()) {
      log.warn("Empty study list! Nothing to BulkIndex. For repo[{}].  LangIsoCode [{}].", repo, langIsoCode);
    } else {
      log.info("BulkIndexing [{}] index with [{}] CmmStudies", langIsoCode, cmmStudies.size());
      boolean isSuccessful = esIndexerService.bulkIndex(cmmStudies, langIsoCode);
      if (isSuccessful) {
        log.info("BulkIndexing was Successful. For repo[{}].  LangIsoCode [{}].", repo, langIsoCode);
      } else {
        log.error("BulkIndexing was UnSuccessful. For repo[{}].  LangIsoCode [{}].", repo, langIsoCode);
      }
    }
  }

  private Map<String, List<CMMStudyOfLanguage>> getCmmStudiesOfEachLangIsoCodeMap(Repo repo, LocalDateTime lastModifiedDateTime) {
    log.info("Processing Repo [{}]", repo.toString());
    List<RecordHeader> recordHeaders = harvesterConsumerService.listRecordHeaders(repo, lastModifiedDateTime);

    int recordHeadersSize = recordHeaders.size();
    log.info("Repo returned with [{}] record headers", recordHeadersSize);

    List<Optional<CMMStudy>> totalCMMStudies = recordHeaders.stream()
        .map(recordHeader -> harvesterConsumerService.getRecord(repo, recordHeader.getIdentifier()))
        .collect(Collectors.toList());

    List<CMMStudy> presentCMMStudies = totalCMMStudies.stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());

    String msgTemplate = "There are [{}] presentCMMStudies out of [{}] totalCMMStudies from [{}] Record Identifiers";
    log.info(msgTemplate, presentCMMStudies.size(), totalCMMStudies.size(), recordHeadersSize);

    presentCMMStudies.forEach(languageAvailabilityMapper::setAvailableLanguages);
    return extractor.mapLanguageDoc(presentCMMStudies, repo.getName());
  }

  private void logStartStatus(LocalDateTime localDateTime, String runDescription) {
    log.info("[{}] Consume and Ingest All SPs Repos : Started at [{}]", runDescription, localDateTime);
    log.info("Currents state before run:");
    debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints();
    debuggingJMXBean.printElasticSearchInfo();
  }

  private void logEndStatus(LocalDateTime localDateTime, Instant startTime, String runDescription) {
    String formatMsg = "[{}] Consume and Ingest All SPs Repos Ended at [{}], Duration [{}]";
    log.info(formatMsg, runDescription, localDateTime, Duration.between(startTime, Instant.now()));
  }
}
