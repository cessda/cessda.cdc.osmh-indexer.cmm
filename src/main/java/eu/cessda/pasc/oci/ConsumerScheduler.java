package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.DefaultHarvesterConsumerService;
import eu.cessda.pasc.oci.service.IngestService;
import eu.cessda.pasc.oci.service.helpers.DebuggingJMXBean;
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
 * Service responsible for triggering harvesting and Metadata ingestion to the search engine
 *
 * @author moses@doraventures.com
 */
@Component
@ManagedResource
@Slf4j
public class ConsumerScheduler {

  private DebuggingJMXBean debuggingJMXBean;
  private AppConfigurationProperties configurationProperties;
  private DefaultHarvesterConsumerService defaultConsumerService;
  private IngestService esIndexerService;
  private LanguageDocumentExtractor extractor;

  @Autowired
  public ConsumerScheduler(DebuggingJMXBean debuggingJMXBean, AppConfigurationProperties configurationProperties,
                           DefaultHarvesterConsumerService consumerService,
                           IngestService esIndexerService,
                           LanguageDocumentExtractor extractor) {
    this.debuggingJMXBean = debuggingJMXBean;
    this.configurationProperties = configurationProperties;
    this.defaultConsumerService = consumerService;
    this.esIndexerService = esIndexerService;
    this.extractor = extractor;
  }

  /**
   * Auto Starts after delay of 1min at startup
   * <p>
   * TODO: incremental run that start at @Scheduled(cron = "0 10 18 * * *") // Everyday at 02:10am
   */
  @ManagedOperation(description = "Manual Trigger to retrieve(harvest) and Ingest records for Configured All SPs Repos")
  @Scheduled(initialDelay = 60_000L, fixedDelay = 315_360_000_000L) // Auto Starts. Delay of 1min at startup.
  public void harvestAndIngestRecordsForAllConfiguredSPsRepos() {
    Instant startTime = Instant.now();
    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime.toEpochMilli()), ZoneId.systemDefault());
    logStartStatus(date);
    execute();
    logEndStatus(date, startTime);
  }

  private void execute() {
    List<Repo> repos = configurationProperties.getEndpoints().getRepos();
    repos.forEach(repo -> {
      Map<String, List<CMMStudyOfLanguage>> langStudies = getCmmStudiesOfEachLangIsoCodeMap(repo);
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

  private Map<String, List<CMMStudyOfLanguage>> getCmmStudiesOfEachLangIsoCodeMap(Repo repo) {
    log.info("Processing Repo [{}]", repo.toString());
    List<RecordHeader> recordHeaders = defaultConsumerService.listRecordHeaders(repo);

    int recordHeadersSize = recordHeaders.size();
    log.info("Repo returned with [{}] record headers", recordHeadersSize);

    //TODO: disable below line override. For manual test only!
/*
    if (recordHeadersSize >= 1020) {
      int limitSize = 20;
      int skipFirst = 1000;
      log.info("********************** Test, Skipping first [{}] and limiting to [{}]", skipFirst, limitSize);
      recordHeaders = recordHeaders.stream().skip(skipFirst).limit(limitSize).collect(Collectors.toList());
    }
*/

    //or
/*    int limitSize = 500;
    log.info("TEST - Limiting to [" + limitSize + "] record headers");
    recordHeaders = recordHeaders.stream().limit(limitSize).collect(Collectors.toList());*/
    // or end

    List<Optional<CMMStudy>> cMMStudiesOptions = recordHeaders
        .stream()
        .map(recordHeader -> defaultConsumerService.getRecord(repo, recordHeader.getIdentifier()))
        .collect(Collectors.toList());

    List<Optional<CMMStudy>> presentCMMStudies = cMMStudiesOptions
        .stream()
        .filter(Optional::isPresent)
        .collect(Collectors.toList());

    String msgTemplate = "There are [{}] presentCMMStudies out of [{}] CMMStudiesOptions from [{}] RecordHeaders";
    log.info(msgTemplate, presentCMMStudies.size(), cMMStudiesOptions.size(), recordHeadersSize);

    return extractor.mapLanguageDoc(cMMStudiesOptions, repo.getName());
  }

  private void logStartStatus(LocalDateTime localDateTime) {
    log.info("Full Run - Consume and Ingest All SPs Repos : Started at [{}]", localDateTime);
    log.info("Currents state before run:");
    debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints();
    debuggingJMXBean.printElasticSearchInfo();
  }

  private void logEndStatus(LocalDateTime localDateTime, Instant startTime) {
    String formatMsg = "Consume and Ingest All SPs Repos Schedule for once a day : Ended at [{}], Duration [{}]";
    log.info(formatMsg, localDateTime, Duration.between(startTime, Instant.now()));
  }
}
