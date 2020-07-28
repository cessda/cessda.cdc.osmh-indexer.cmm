/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.helpers.LoggingConstants;
import eu.cessda.pasc.oci.metrics.Metrics;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import eu.cessda.pasc.oci.service.helpers.LanguageDocumentExtractor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.value;

@Component
@Slf4j
public class HarvesterRunner {

    private final AppConfigurationProperties configurationProperties;
    private final HarvesterConsumerService localHarvester;
    private final IngestService ingestService;
    private final LanguageDocumentExtractor extractor;
    private final Metrics metrics;
    private final HarvesterConsumerService remoteHarvester;

    private final AtomicBoolean indexerRunning = new AtomicBoolean(false);

    public HarvesterRunner(AppConfigurationProperties configurationProperties, HarvesterConsumerService remoteHarvesterConsumerService, HarvesterConsumerService localHarvesterConsumerService,
                           IngestService ingestService, LanguageDocumentExtractor extractor, Metrics metrics) {
        this.configurationProperties = configurationProperties;
        this.localHarvester = localHarvesterConsumerService;
        this.ingestService = ingestService;
        this.extractor = extractor;
        this.metrics = metrics;
        this.remoteHarvester = remoteHarvesterConsumerService;
    }


    /**
     * Starts the harvest.
     *
     * @param lastModifiedDateTime the DateTime to incrementally harvest from, set to null to perform a full harvest.
     * @throws IllegalStateException if a harvest is already running.
     */
    @SuppressWarnings("try")
    public void executeHarvestAndIngest(LocalDateTime lastModifiedDateTime) {
        if (!indexerRunning.getAndSet(true)) {
            try {
                List<Repo> repos = configurationProperties.getEndpoints().getRepos();

                // Store the MDC so that it can be used in the running thread
                Map<String, String> contextMap = MDC.getCopyOfContextMap();
                repos.parallelStream().forEach(repo -> {
                    MDC.setContextMap(contextMap);

                    // Set the MDC so that the record name is attached to all downstream logs
                    try (var repoNameClosable = MDC.putCloseable(LoggingConstants.REPO_NAME, repo.getCode())) {
                        log.info("Processing Repo [{}]", repo);
                        Map<String, List<CMMStudyOfLanguage>> langStudies = getCmmStudiesOfEachLangIsoCodeMap(repo, lastModifiedDateTime);
                        langStudies.forEach((langIsoCode, cmmStudies) -> {
                            try (var langClosable = MDC.putCloseable(LoggingConstants.LANG_CODE, langIsoCode)) {
                                executeBulk(repo, langIsoCode, cmmStudies);
                            }
                        });
                    }
                });
                log.info("Total number of records is {}.", value("total_cmm_studies", ingestService.getTotalHitCount("*")));
                metrics.updateMetrics();
            } catch (ElasticsearchException e) {
                log.error("Harvest failed!: {}", e.toString());
            } finally {
                // Ensure that the running state is always set to false even if an exception is thrown
                indexerRunning.set(false);
            }
        } else {
            throw new IllegalStateException("Indexer is already running");
        }
        // Reset the MDC
        MDC.clear();
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
            log.info("[{}({})] Indexing...", repo.getCode(), langIsoCode);
            if (ingestService.bulkIndex(cmmStudies, langIsoCode)) {
                log.info("[{}({})] Indexing succeeded: [{}] studies created, [{}] studies deleted, [{}] studies updated.",
                        repo.getCode(),
                        langIsoCode,
                        value("created_cmm_studies", studiesUpdated.studiesCreated),
                        value("deleted_cmm_studies", studiesUpdated.studiesDeleted),
                        value("updated_cmm_studies", studiesUpdated.studiesUpdated));
            } else {
                log.error("[{}({})] Indexing failed!", repo.getCode(), langIsoCode);
            }
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
            var esStudyOptional = ingestService.getStudy(remoteStudy.getId(), language);

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

    private Map<String, List<CMMStudyOfLanguage>> getCmmStudiesOfEachLangIsoCodeMap(Repo repo, LocalDateTime lastModifiedDateTime) {

        HarvesterConsumerService harvester;

        // OAI-PMH repositories can be handled by the internal harvester, all other types should be delegated to remote handlers
        if (repo.getHandler().equalsIgnoreCase("OAI-PMH")) {
            harvester = localHarvester;
        } else {
            harvester = remoteHarvester;
        }

        List<RecordHeader> recordHeaders = harvester.listRecordHeaders(repo, lastModifiedDateTime);

        List<CMMStudy> presentCMMStudies = recordHeaders.stream()
                // If the harvest is cancelled, prevent the retrieval of any more records
                .filter(recordHeader -> indexerRunning.get())
                .map(recordHeader -> harvester.getRecord(repo, recordHeader.getIdentifier()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        log.info("[{}] Retrieved [{}] studies from [{}] header entries. [{}] studies couldn't be retrieved.",
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value("present_cmm_record", presentCMMStudies.size()),
                value("total_cmm_record", recordHeaders.size()),
                value("cmm_records_rejected", recordHeaders.size() - presentCMMStudies.size()));

        return extractor.mapLanguageDoc(presentCMMStudies, repo);
    }

    @Value
    private static class UpdatedStudies {
        int studiesCreated;
        int studiesDeleted;
        int studiesUpdated;
    }
}
