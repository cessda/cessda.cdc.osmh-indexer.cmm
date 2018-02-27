package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.PascOciConfig;
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
//@Component
@ManagedResource
@Slf4j
public class ConsumerScheduler {

  private DebuggingJMXBean debuggingJMXBean;
  private PascOciConfig pascOciConfig;
  private DefaultHarvesterConsumerService defaultConsumerService;
  private ESIndexerService esIndexerService;
  private LanguageDocumentExtractor extractor;

  @Autowired
  public ConsumerScheduler(DebuggingJMXBean debuggingJMXBean,
                           PascOciConfig pascOciConfig,
                           DefaultHarvesterConsumerService consumerService,
                           ESIndexerService esIndexerService,
                           LanguageDocumentExtractor extractor) {
    this.debuggingJMXBean = debuggingJMXBean;
    this.pascOciConfig = pascOciConfig;
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
    long startTimeMillis = System.currentTimeMillis();
    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault());
    logStartStatus(date);
    execute();
    logEndStatus(date, startTimeMillis);
  }

  private void execute() {
    List<Repo> repos = pascOciConfig.getEndpoints().getRepos();
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
    log.info("Processing Rep [{}]", repo.toString());
    List<RecordHeader> recordHeaders = defaultConsumerService.listRecorderHeadersBody(repo);

    //TODO: disable below line override. For manual test only!
//    recordHeaders = recordHeaders.stream().skip(1000).limit(20).collect(Collectors.toList());

    List<Optional<CMMStudy>> cMMStudiesOptions = recordHeaders.parallelStream()
        .map(recordHeader -> defaultConsumerService.getRecord(repo, recordHeader.getIdentifier()))
        .collect(Collectors.toList());
    return extractor.mapLanguageDoc(cMMStudiesOptions, repo.getName());
  }

  private void logStartStatus(LocalDateTime localDateTime) {
    log.info("Consumer and Ingest All SPs Repos Schedule for once a day : Started at [{}]", localDateTime);
    log.info("Currents state before run:");
    debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints();
    debuggingJMXBean.printElasticSearchInfo();

  }

  private void logEndStatus(LocalDateTime localDateTime, long startTimeMillis) {
    long timeTakeMin = (System.currentTimeMillis() - startTimeMillis) / 60_000;
    log.info("Consumer and Ingest All SPs Repos Schedule for once a day : Ended at [{}], Took [{}minutes]",
        localDateTime, timeTakeMin);
  }
}
