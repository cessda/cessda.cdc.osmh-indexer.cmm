/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.elasticsearch.IndexingException;
import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
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
    @SuppressWarnings("OverlyBroadCatchBlock")
    public void executeHarvestAndIngest() {
        if (!indexerRunning.getAndSet(true)) {
            // Load explicitly configured repositories
            var repos = configurationProperties.repos();

            // Discover repositories by attempting to find pipeline.json instances if a base directory is configured
            Stream<Repo> repoStream;
            if (configurationProperties.baseDirectory() != null) {
                var discoverRepositories = pipelineUtilities.discoverRepositories(configurationProperties.baseDirectory());
                repoStream = Stream.concat(discoverRepositories, repos.stream());
            } else {
                repoStream = repos.stream();
            }

            try (repoStream) {
                repoStream.forEach(repo -> {
                    try {
                        // Index the repository
                        indexRepository(repo);
                    } catch (Exception e) {
                        // Handle exceptional completion here, this allows failures to be logged as soon as possible
                        log.error("[{}]: Unexpected error occurred when harvesting!", value(LoggingConstants.REPO_NAME, repo.code()), e);
                    }
                });


                log.info("Indexing finished. Summary of the current state:\nTotal number of records: {}",
                    value("total_cmm_studies", ingestService.getTotalHitCount("*"))
                );
            } catch (IOException e) {
                log.warn("Indexing finished. An IO error occurred when getting the total number of records: {}", e.toString());
            } finally {
                // Ensure that the running state is always set to false even if an exception is thrown
                indexerRunning.set(false);
            }
        } else {
            throw new IllegalStateException("Indexer is already running");
        }
    }

    /**
     * Harvest an individual repository.
     *
     * @param repo                 the repository to harvest.
     */
    @SuppressWarnings("try")
    private void indexRepository(Repo repo) {
        var startTime = Instant.now();
        log.info("Processing Repo [{}]{}", repo, keyValue(LoggingConstants.REPO_NAME, repo.code(), ""));
        var langStudies = indexer.getRecords(repo);
        for (var entry : langStudies.entrySet()) {
            var lang = entry.getKey();
            try {
                indexRecords(repo, lang, entry.getValue());
            } catch (IndexingException e) {
                log.error("[{}({})] Indexing failed: {}: {}",
                    value(LoggingConstants.REPO_NAME, repo.code()),
                    value(LoggingConstants.LANG_CODE, lang),
                    value(LoggingConstants.EXCEPTION_NAME, e.getClass().getName()),
                    value(LoggingConstants.REASON, e.getMessage())
                );
            } catch (ElasticsearchException e) {
                log.error("[{}({})] Error communicating with Elasticsearch!",
                    value(LoggingConstants.REPO_NAME, repo.code()),
                    value(LoggingConstants.LANG_CODE, lang), e
                );
            }
        }
        log.info("[{}] Repo finished, took {} seconds",
            value(LoggingConstants.REPO_NAME, repo.code()),
            value("repository_duration", Duration.between(startTime, Instant.now()).toSeconds())
        );
    }


    /**
     * Index the given CMMStudies into the Elasticsearch index.
     *
     * @param repo        the source repository.
     * @param langIsoCode the language code.
     * @param cmmStudies  the studies to index.
     */
    private void indexRecords(Repo repo, String langIsoCode, List<CMMStudyOfLanguage> cmmStudies) throws IndexingException {
        if (indexerRunning.get() && !cmmStudies.isEmpty()) {
            log.info("[{}({})] Indexing...", repo.code(), langIsoCode);

            // Calculate the amount of changed studies
            var studiesUpdated = getUpdatedStudies(cmmStudies, langIsoCode);

            // Discover studies to delete, we do this by creating a HashSet of ids and then comparing what's in the database
            var studyIds = HashSet.<String>newHashSet(cmmStudies.size());
            for (var study : cmmStudies) {
                studyIds.add(study.id());
            }

            var studiesToDelete = new ArrayList<CMMStudyOfLanguage>(cmmStudies.size());
            try {
                for (var presentStudy : ingestService.getStudiesByRepository(repo.code(), langIsoCode)) {
                    if (!studyIds.contains(presentStudy.id())) {
                        studiesToDelete.add(presentStudy);
                    }
                }
            } catch (ElasticsearchException | UncheckedIOException e) {
                if (!(e instanceof ElasticsearchException) || !e.getMessage().contains("index_not_found_exception")) {
                    log.warn("[{}({})] Couldn't retrieve existing studies for deletions: {}",
                        value(LoggingConstants.REPO_NAME, repo.code()),
                        value(LoggingConstants.LANG_CODE, langIsoCode),
                        e.toString()
                    );
                }
            }

            // Perform indexing and deletions
            ingestService.bulkIndex(cmmStudies, langIsoCode);
            ingestService.bulkDelete(studiesToDelete, langIsoCode);

            log.info("[{}({})] Indexing succeeded: {} studies created, {} studies deleted, {} studies updated.",
                value(LoggingConstants.REPO_NAME, repo.code()),
                value(LoggingConstants.LANG_CODE, langIsoCode),
                value("created_cmm_studies", studiesUpdated.studiesCreated),
                value("deleted_cmm_studies", studiesToDelete.size()),
                value("updated_cmm_studies", studiesUpdated.studiesUpdated)
            );
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
        var studiesUpdated = new AtomicInteger(0);

        for (CMMStudyOfLanguage localStudy : cmmStudies) {
            ingestService.getStudy(localStudy.id(), language).ifPresentOrElse(study -> {
                    if (!localStudy.equals(study)) {
                        // The study has been updated
                        studiesUpdated.getAndIncrement();
                    }
                },
                // If empty then the study didn't exist in Elasticsearch, and will be created
                studiesCreated::getAndIncrement
            );
        }

        return new UpdatedStudies(studiesCreated.get(), studiesUpdated.get());
    }

    private record UpdatedStudies(
        int studiesCreated,
        int studiesUpdated
    ) {
    }

    /**
     * Trigger reindexing for all themes and their respective reindex queries.
     *
     * @throws IllegalStateException if reindexing is already running.
     */
    public void executeReindexing() {
        try {
            log.info("Starting reindexing process...");

            ingestService.reindexAllThemes();

            log.info("Reindexing process completed successfully.");
        } catch (IndexingException e) {
            log.error("Error occurred during reindexing: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    private void shutdown() {
        if (indexerRunning.getAndSet(false)) {
            log.info("Indexing cancelled");
        }
    }
}
