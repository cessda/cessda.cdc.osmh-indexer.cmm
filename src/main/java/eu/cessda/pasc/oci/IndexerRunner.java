/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.elasticsearch.IndexingException;
import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import jakarta.annotation.PreDestroy;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.runAsync;
import static net.logstash.logback.argument.StructuredArguments.value;

@Component
@Slf4j
public class IndexerRunner {

    private final AppConfigurationProperties configurationProperties;
    private final IndexerConsumerService indexer;
    private final PipelineUtilities pipelineUtilities;
    private final IngestService ingestService;

    private final AtomicBoolean indexerRunning = new AtomicBoolean(false);

    public IndexerRunner(AppConfigurationProperties configurationProperties,
                         IndexerConsumerService localHarvesterConsumerService,
                         PipelineUtilities pipelineUtilities,
                         IngestService ingestService) {
        this.configurationProperties = configurationProperties;
        this.indexer = localHarvesterConsumerService;
        this.pipelineUtilities = pipelineUtilities;
        this.ingestService = ingestService;
    }


    /**
     * Starts the harvest.
     *
     * @throws IllegalStateException if a harvest is already running.
     */
    public void executeHarvestAndIngest() {
        if (!indexerRunning.getAndSet(true)) {

            // Load explicitly configured repositories
            var repos = configurationProperties.getEndpoints().getRepos();

            // Store the MDC so that it can be used in the running thread
            var contextMap = MDC.getCopyOfContextMap();

            // Discover repositories by attempting to find pipeline.json instances if a base directory is configured
            try (var repoParsedFromJson = pipelineUtilities.discoverRepositories(configurationProperties.getBaseDirectory())) {
                var futures = Stream.concat(repoParsedFromJson, repos.stream())
                    .map(repo -> runAsync(() -> indexRepository(repo, contextMap))
                        .exceptionally(e -> {
                            log.error("[{}]: Unexpected error occurred when harvesting!", repo.getCode(), e);
                            return null;
                        })
                    ).toArray(CompletableFuture[]::new);

                CompletableFuture.allOf(futures).join();

                log.info("Harvest finished. Summary of the current state:");
                log.info("Total number of records: {}", value("total_cmm_studies", ingestService.getTotalHitCount("*")));
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
     * @param contextMap           the logging context map.
     */
    @SuppressWarnings("try")
    private void indexRepository(Repo repo, Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);

        // Set the MDC so that the record name is attached to all downstream logs
        try (var repoNameClosable = MDC.putCloseable(LoggingConstants.REPO_NAME, repo.getCode())) {
            var startTime = Instant.now();
            log.info("Processing Repo [{}]", repo);
            var langStudies = indexer.getRecords(repo);
            for (var entry : langStudies.entrySet()) {
                try (var langClosable = MDC.putCloseable(LoggingConstants.LANG_CODE, entry.getKey())) {
                    indexRecords(repo, entry.getKey(), entry.getValue());
                } catch (ElasticsearchException e) {
                    log.error("[{}({})] Error communicating with Elasticsearch!", repo.getCode(), entry.getKey(), e);
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
    private void indexRecords(Repo repo, String langIsoCode, List<CMMStudyOfLanguage> cmmStudies) {
        if (indexerRunning.get() && !cmmStudies.isEmpty()) {
            log.info("[{}({})] Indexing...", repo.getCode(), langIsoCode);

            // Discover studies to delete, we do this by creating a HashSet of ids and then comparing what's in the database
            var studyIds = cmmStudies.stream().map(CMMStudyOfLanguage::id).collect(Collectors.toCollection(HashSet::new));
            var studiesToDelete = new ArrayList<CMMStudyOfLanguage>();
            try {
                for (var presentStudy : ingestService.getStudiesByRepository(repo.getCode(), langIsoCode)) {
                    if (!studyIds.contains(presentStudy.id())) {
                        studiesToDelete.add(presentStudy);
                    }
                }
            } catch (ElasticsearchException | UncheckedIOException e) {
                if (!(e instanceof ElasticsearchException) || !e.getMessage().contains("index_not_found_exception")) {
                    log.warn("[{}({})] Couldn't retrieve existing studies for deletions: {}", repo.getCode(), langIsoCode, e.toString());
                }
            }

            // Calculate the amount of changed studies
            var studiesUpdated = getUpdatedStudies(cmmStudies, studiesToDelete.size(), langIsoCode);

            // Perform indexing and deletions
            try {
                ingestService.bulkIndex(cmmStudies, langIsoCode);
                ingestService.bulkDelete(studiesToDelete, langIsoCode);
                log.info("[{}({})] Indexing succeeded: [{}] studies created, [{}] studies deleted, [{}] studies updated.",
                    value(LoggingConstants.REPO_NAME, repo.getCode()),
                    value(LoggingConstants.LANG_CODE, langIsoCode),
                    value("created_cmm_studies", studiesUpdated.studiesCreated),
                    value("deleted_cmm_studies", studiesUpdated.studiesDeleted),
                    value("updated_cmm_studies", studiesUpdated.studiesUpdated)
                );
            } catch (IndexingException e) {
                log.error("[{}({})] Indexing failed: {}: {}",
                    value(LoggingConstants.REPO_NAME, repo.getCode()),
                    value(LoggingConstants.LANG_CODE, langIsoCode),
                    value(LoggingConstants.EXCEPTION_NAME, e.getClass().getName()),
                    value(LoggingConstants.REASON, e.getMessage())
                );
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
    private UpdatedStudies getUpdatedStudies(Collection<CMMStudyOfLanguage> cmmStudies, int studiesToDelete, String language) {

        var studiesCreated = new AtomicInteger(0);
        var studiesUpdated = new AtomicInteger(0);

        cmmStudies.parallelStream().forEach(localStudy -> ingestService.getStudy(localStudy.id(), language)
            .ifPresentOrElse(study -> {
                if (!localStudy.equals(study)) {
                    // The study has been updated
                    studiesUpdated.getAndIncrement();
                }
            },
                // If empty then the study didn't exist in Elasticsearch, and will be created
                studiesCreated::getAndIncrement
            )
        );

        return new UpdatedStudies(studiesCreated.get(), studiesToDelete, studiesUpdated.get());
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
            log.info("Indexing cancelled");
        }
    }
}
