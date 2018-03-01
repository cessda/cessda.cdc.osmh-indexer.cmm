package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.PaSCOciConfigurationProperties;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
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
  private PaSCOciConfigurationProperties paSCOciConfigurationProperties;
  private DefaultHarvesterConsumerService defaultConsumerService;
  private ESIndexerService esIndexerService;
  private LanguageDocumentExtractor extractor;

  @Autowired
  public ConsumerScheduler(DebuggingJMXBean debuggingJMXBean,
                           PaSCOciConfigurationProperties paSCOciConfigurationProperties,
                           DefaultHarvesterConsumerService consumerService,
                           ESIndexerService esIndexerService,
                           LanguageDocumentExtractor extractor) {
    this.debuggingJMXBean = debuggingJMXBean;
    this.paSCOciConfigurationProperties = paSCOciConfigurationProperties;
    this.defaultConsumerService = consumerService;
    this.esIndexerService = esIndexerService;
    this.extractor = extractor;
  }

  /**
   * run once a day at 9am
   */
  @Scheduled(cron = "0 0 09 * * *")
  @ManagedOperation(description = "Manual Trigger to retrieve(harvest) and Ingest records for Configured All SPs Repos")
  public void harvestAndIngestRecordsForAllConfiguredSPsRepos() {
    Instant startTime = Instant.now();
    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime.toEpochMilli()), ZoneId.systemDefault());
    logStartStatus(date);
    execute();
    logEndStatus(date, startTime);
  }

  private void execute() {
    List<Repo> repos = paSCOciConfigurationProperties.getEndpoints().getRepos();
    repos.forEach(repo -> {
      Map<String, List<CMMStudyOfLanguage>> langStudies = getCmmStudiesOfEachLangIsoCodeMap(repo);
      langStudies.forEach((langIsoCode, cmmStudies) -> {
        log.info("BulkIndexing indexing [{}] index with [{}] CmmStudies", langIsoCode, cmmStudies.size());
        boolean isSuccessful = esIndexerService.bulkIndex(cmmStudies, langIsoCode);
        log.info("BulkIndexing was fully Successful [{}], See logs for Id of any failed document above", isSuccessful);
      });
    });
  }

  private Map<String, List<CMMStudyOfLanguage>> getCmmStudiesOfEachLangIsoCodeMap(Repo repo) {
    log.info("Processing Repo [{}]", repo.toString());
    List<RecordHeader> recordHeaders = defaultConsumerService.listRecorderHeadersBody(repo);

    int recordHeadersSize = recordHeaders.size();
    log.info("Repo returned with [{}] record headers", recordHeadersSize);

    //TODO: disable below line override. For manual test only!
   /* int limitSize = 500;
    if (recordHeadersSize > 1020) {
      log.info("**********************");
      log.info("********************** Test *********************************** Truncating records.");
      log.info("**********************");
      recordHeaders = recordHeaders.stream().skip(1000).limit(limitSize).collect(Collectors.toList());
    }*/

    //or
/*    int limitSize = 500;
    log.info("TEST - Limiting to [" + limitSize + "] record headers");
    recordHeaders = recordHeaders.stream().limit(limitSize).collect(Collectors.toList());*/
    // or end

    List<Optional<CMMStudy>> cMMStudiesOptions = recordHeaders.stream()
        .map(recordHeader -> defaultConsumerService.getRecord(repo, recordHeader.getIdentifier()))
        .collect(Collectors.toList());

    List<Optional<CMMStudy>> presentCMMStudies = cMMStudiesOptions.stream()
        .filter(Optional::isPresent).collect(Collectors.toList());
    String msgTemplate = "There are [{}] presentCMMStudies out of [{}] CMMStudiesOptions from [{}] RecordHeaders";
    log.info(msgTemplate, presentCMMStudies.size(), cMMStudiesOptions.size(), recordHeadersSize);
    return extractor.mapLanguageDoc(cMMStudiesOptions, repo.getName());
  }

  private void logStartStatus(LocalDateTime localDateTime) {
    log.info("Consumer and Ingest All SPs Repos Schedule for once a day : Started at [{}]", localDateTime);
    log.info("Currents state before run:");
    debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints();
    debuggingJMXBean.printElasticSearchInfo();

  }

  private void logEndStatus(LocalDateTime localDateTime, Instant startTime) {
    String formatMsg = "Consumer and Ingest All SPs Repos Schedule for once a day : Ended at [{}], Duration [{}]";
    log.info(formatMsg, localDateTime, Duration.between(startTime, Instant.now()));
  }
}
