/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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
import eu.cessda.pasc.oci.harvester.HarvesterConsumerService;
import eu.cessda.pasc.oci.harvester.LanguageExtractor;
import eu.cessda.pasc.oci.metrics.Metrics;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.runAsync;
import static net.logstash.logback.argument.StructuredArguments.value;

@Component
@Slf4j
@SuppressWarnings("unused")
public class HarvesterRunner {

    private final AppConfigurationProperties configurationProperties;
    private final HarvesterConsumerService localHarvester;
    private final IngestService ingestService;
    private final LanguageExtractor extractor;
    private final Metrics metrics;
    private final HarvesterConsumerService remoteHarvester;

    private final AtomicBoolean indexerRunning = new AtomicBoolean(false);

    public HarvesterRunner(AppConfigurationProperties configurationProperties,
                           HarvesterConsumerService remoteHarvesterConsumerService,
                           HarvesterConsumerService localHarvesterConsumerService,
                           IngestService ingestService,
                           LanguageExtractor extractor,
                           Metrics metrics) {
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
     * @param lastModifiedDateTime the {@link LocalDateTime} to incrementally harvest from, set to {@code null} to perform a full harvest.
     * @throws IllegalStateException if a harvest is already running.
     */
    public void executeHarvestAndIngest(LocalDateTime lastModifiedDateTime) {
        if (!indexerRunning.getAndSet(true)) {

            List<Repo> repos = configurationProperties.getEndpoints().getRepos();

            // Store the MDC so that it can be used in the running thread
            var contextMap = MDC.getCopyOfContextMap();

            try {
                var futures = repos.stream()
                    .map(repo -> runAsync(() -> harvestRepository(repo, lastModifiedDateTime, contextMap))
                            .exceptionally(e -> {
                                log.error("Unexpected error occurred when harvesting!", e);
                                return null;
                            })
                    ).toArray(CompletableFuture[]::new);

                CompletableFuture.allOf(futures).join();

                log.info("Harvest finished. Summary of the current state:");
                log.info("Total number of records: {}", value("total_cmm_studies", ingestService.getTotalHitCount("*")));
                metrics.updateMetrics();
            } catch (IOException e) {
                log.error("IO Error when getting the total number of records: {}", e.toString());
            } finally {
                // Ensure that the running state is always set to false even if an exception is thrown
                indexerRunning.set(false);
                MDC.setContextMap(contextMap);
            }
        } else {
            throw new IllegalStateException("Indexer is already running");
        }
    }

    /**
     * Harvest an individual repository.
     *
     * @param repo                 the repository to harvest.
     * @param lastModifiedDateTime the {@link LocalDateTime} to incrementally harvest from, can be {@code null}.
     * @param contextMap           the logging context map.
     */
    @SuppressWarnings("try")
    private void harvestRepository(Repo repo, LocalDateTime lastModifiedDateTime, Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);

        // Set the MDC so that the record name is attached to all downstream logs
        try (var repoNameClosable = MDC.putCloseable(LoggingConstants.REPO_NAME, repo.getCode())) {
            var startTime = Instant.now();
            log.info("Processing Repo [{}]", repo);
            var langStudies = getCmmStudiesOfEachLangIsoCodeMap(repo, lastModifiedDateTime);
            for (var entry : langStudies.entrySet()) {
                try (var langClosable = MDC.putCloseable(LoggingConstants.LANG_CODE, entry.getKey())) {
                    executeBulk(repo, entry.getKey(), entry.getValue());
                } catch (ElasticsearchException e) {
                    log.error("[{}({})] Error communicating with Elasticsearch!: {}", repo.getCode(), entry.getKey(), e.toString());
                }
            }
            log.info("[{}] Repo finished, took {} seconds",
                repo.getCode(),
                value("repository_duration", Duration.between(startTime, Instant.now()).getSeconds())
            );
        } finally {
            // Reset the MDC
            MDC.clear();
        }
    }


    /**
     * Index the given CMMStudies into the Elasticsearch index.
     *
     * @param repo        the source repository.
     * @param langIsoCode the language code.
     * @param cmmStudies  the studies to index.
     */
    private void executeBulk(Repo repo, String langIsoCode, Collection<CMMStudyOfLanguage> cmmStudies) {
        if (indexerRunning.get() && !cmmStudies.isEmpty()) {
            log.info("[{}({})] Indexing...", repo.getCode(), langIsoCode);
            var studiesUpdated = getUpdatedStudies(cmmStudies, langIsoCode);

            // Split the studies into studies to index and studies to delete
            var studiesToIndex = new ArrayList<CMMStudyOfLanguage>();
            var studiesToDelete = new ArrayList<CMMStudyOfLanguage>();
            for (var study : cmmStudies) {
                if (study.isActive()) {
                    studiesToIndex.add(study);
                } else {
                    studiesToDelete.add(study);
                }
            }

            // Perform indexing and deletions
            try {
                if (ingestService.bulkIndex(studiesToIndex, langIsoCode)) {
                    ingestService.bulkDelete(studiesToDelete, langIsoCode);
                    log.info("[{}({})] Indexing succeeded: [{}] studies created, [{}] studies deleted, [{}] studies updated.",
                        repo.getCode(),
                        langIsoCode,
                        value("created_cmm_studies", studiesUpdated.studiesCreated),
                        value("deleted_cmm_studies", studiesUpdated.studiesDeleted),
                        value("updated_cmm_studies", studiesUpdated.studiesUpdated));
                } else {
                    log.error("[{}({})] Indexing failed!", repo.getCode(), langIsoCode);
                }
            } catch (IOException e) {
                log.error("[{}({})] Indexing failed: {}", repo.getCode(), langIsoCode, e.toString());
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

        cmmStudies.parallelStream().forEach(remoteStudy -> ingestService.getStudy(remoteStudy.getId(), language)
            .ifPresentOrElse(study -> {
                if (!remoteStudy.isActive()) {
                    // The study has been deleted
                    studiesDeleted.getAndIncrement();
                } else if (!remoteStudy.equals(study)) {
                    // The study has been updated
                    studiesUpdated.getAndIncrement();
                }
            }, () -> {
                if (remoteStudy.isActive()) {
                    // If empty then the study didn't exist in Elasticsearch, and will be created
                    studiesCreated.getAndIncrement();
                }
            })
        );

        return new UpdatedStudies(studiesCreated.get(), studiesDeleted.get(), studiesUpdated.get());
    }

    private Map<String, List<CMMStudyOfLanguage>> getCmmStudiesOfEachLangIsoCodeMap(Repo repo, LocalDateTime lastModifiedDateTime) {

        final HarvesterConsumerService harvester;

        // OAI-PMH repositories can be handled by the internal harvester, all other types should be delegated to remote handlers
        if (repo.getHandler().equalsIgnoreCase("OAI-PMH") || repo.getHandler().equalsIgnoreCase("NESSTAR")) {
            harvester = localHarvester;
        } else {
            harvester = remoteHarvester;
        }

        try (var recordHeaders = harvester.listRecordHeaders(repo, lastModifiedDateTime)) {

            var studies = new AtomicInteger();

            var collectLanguageCmmStudy = recordHeaders
                .flatMap(recordHeader -> harvester.getRecord(repo, recordHeader).stream()) // Retrieve the record
                .flatMap(cmmStudy -> {
                    // Extract language specific variants of the record
                    var extractedStudies = extractor.extractFromStudy(cmmStudy, repo);
                    if (!extractedStudies.isEmpty()) {
                        studies.getAndIncrement();
                    }
                    return extractedStudies.entrySet().stream();
                }).collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

            log.info("[{}] Retrieved [{}] studies.",
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value("present_cmm_record", studies.get())
            );

            return collectLanguageCmmStudy;
        }
    }

    @Value
    private static class UpdatedStudies {
        int studiesCreated;
        int studiesDeleted;
        int studiesUpdated;
    }

    @PreDestroy
    private void shutdown() {
        if (indexerRunning.getAndSet(false)) {
            log.info("Harvest cancelled");
        }
    }
}
