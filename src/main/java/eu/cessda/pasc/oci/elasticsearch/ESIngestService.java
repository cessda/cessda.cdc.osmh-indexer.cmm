/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.ReindexRequest;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.TimeUtility;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.configurations.ElasticsearchConfiguration;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.service.DebuggingJMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for triggering harvesting and Metadata ingestion to the search engine
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class ESIngestService implements IngestService {

    private static final String LAST_MODIFIED_FIELD = "lastModified";
    private static final String INDEX_TYPE = "cmmstudy";
    private static final String MAPPINGS_JSON = "elasticsearch/mappings/mappings_" + INDEX_TYPE + ".json";
    private static final String INDEX_NAME_TEMPLATE = INDEX_TYPE + "_%s";
    private static final String REINDEX_THEMES_DIR = "elasticsearch/themes";

    /**
     * The amount of studies to BulkIndex at once
     */
    private static final int INDEX_COMMIT_SIZE = 500;

    private final ESConfigurationProperties esConfigProps;

    private ElasticsearchClient client = null;
    private final Supplier<ElasticsearchClient> esClient;

    @Autowired
    public ESIngestService(
        ElasticsearchConfiguration elasticsearchConfiguration,
        ESConfigurationProperties esConfigProps,
        ObjectMapper objectMapper
    ) {
        this.esConfigProps = esConfigProps;
        this.esClient = () -> {
            if (this.client == null
                // Reset the client if the underlying HTTPClient is not running
                || !((RestClientTransport) this.client._transport()).restClient().isRunning()) {
                this.client = elasticsearchConfiguration.elasticsearchClient();
                log.info("Started Elasticsearch REST client");
            }
            return this.client;
        };
    }

    public ESIngestService(ElasticsearchClient esClient, ESConfigurationProperties esConfigProps) {
        this.esConfigProps = esConfigProps;
        this.client = esClient;
        this.esClient = () -> this.client;
    }

    private ElasticsearchClient esClient() {
        return this.esClient.get();
    }

    @Bean
    public DebuggingJMXBean debuggingJMXBean() {
        return new DebuggingJMXBean(esClient());
    }

    @Override
    public void bulkIndex(Collection<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode) throws IndexingException {
        var indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);

        createIndex(indexName);

        log.debug("[{}] Indexing {} studies", indexName, languageCMMStudiesMap.size());

        var operationList = new ArrayList<BulkOperation>();

        for (var study : languageCMMStudiesMap) {
            var indexRequest = new IndexOperation.Builder<CMMStudyOfLanguage>()
                .index(indexName)
                .id(study.id())
                .document(study)
                .build();

            operationList.add(new BulkOperation(indexRequest));

            if (operationList.size() == INDEX_COMMIT_SIZE) {
                log.trace("[{}] Bulk Indexing {} studies", indexName, INDEX_COMMIT_SIZE);
                var bulkRequest = new BulkRequest.Builder().operations(operationList).build();
                indexBulkRequest(indexName, bulkRequest);

                // Clear the bulk request
                operationList.clear();
            }
        }

        // Commit all remaining studies
        if (!operationList.isEmpty()) {
            log.trace("[{}] Bulk Indexing {} studies", indexName, operationList.size());
            var bulkRequest = new BulkRequest.Builder().operations(operationList).build();
            indexBulkRequest(indexName, bulkRequest);
        }

        log.debug("[{}] Indexing completed.", indexName);
    }

    private void indexBulkRequest(String indexName, BulkRequest request) throws IndexingException {
        try {
            var response = this.esClient().bulk(request);
            if (response.errors()) {
                for (var item : response.items()) {
                    if (item.error() != null && "strict_dynamic_mapping_exception".equals(item.error().type())) {
                        // Attempt updating field mappings
                        var updateMappingRequest = new PutMappingRequest.Builder()
                            .withJson(ResourceHandler.getResourceAsStream(MAPPINGS_JSON))
                            .index(indexName)
                            .build();

                        esClient().indices().putMapping(updateMappingRequest);

                        // Retry indexing with updated mappings.
                        response = esClient().bulk(request);
                        break;
                    }
                }

                if (response.errors()) {
                    log.warn("[{}] {}", indexName, response.items().stream()
                        .map(BulkResponseItem::error)
                        .filter(Objects::nonNull)
                        .map(ErrorCause::toString)
                        .collect(Collectors.joining(", ")));
                }
            }
        } catch (ElasticsearchException | IOException e) {
            throw new IndexingException(e);
        }
    }

    @Override
    public void bulkDelete(Collection<CMMStudyOfLanguage> cmmStudiesToDelete, String languageIsoCode) throws IndexingException {
        // Set the index
        var indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);

        // Extract the ids from the studies, and add them to the delete query
        var deleteRequests = cmmStudiesToDelete.stream()
            .map(CMMStudyOfLanguage::id)
            .map(id -> new DeleteOperation.Builder().index(indexName).id(id).build())
            .map(BulkOperation::new)
            .toList();

        // Perform the deletion
        if (!deleteRequests.isEmpty()) {
            var deleteBulkRequest = new BulkRequest.Builder().operations(deleteRequests).build();
            indexBulkRequest(indexName, deleteBulkRequest);
        }
    }

    @Override
    public long getTotalHitCount(String language) throws IOException {
        var matchAllCountRequest = new CountRequest.Builder().index(String.format(INDEX_NAME_TEMPLATE, language)).build();
        var response = esClient().count(matchAllCountRequest);
        return response.count();
    }

    @Override
    public ElasticsearchSet<CMMStudyOfLanguage> getAllStudies(String language) {
        log.debug("Getting all studies for language [{}]", language);
        return new ElasticsearchSet<>(
            getSearchRequest("*", new MatchAllQuery.Builder().build()._toQuery()),
            esClient(),
            CMMStudyOfLanguage.class
        );
    }

    @Override
    public ElasticsearchSet<CMMStudyOfLanguage> getStudiesByRepository(String repository, String language) {
        log.debug("Getting all studies for repository [{}] with language [{}]", repository, language);
        var repositorySearchRequest = getSearchRequest(language,
            new TermQuery.Builder().field("code").value(repository).build()._toQuery()
        );
        return new ElasticsearchSet<>(repositorySearchRequest, esClient(), CMMStudyOfLanguage.class);
    }

    @Override
    public Optional<CMMStudyOfLanguage> getStudy(String id, String language) {
        log.trace("Retrieving study [{}], language [{}]", id, language);

        try {
            var request = new co.elastic.clients.elasticsearch.core.GetRequest.Builder()
                .index(String.format(INDEX_NAME_TEMPLATE, language)).id(id).build();
            var response = esClient().get(request, CMMStudyOfLanguage.class);

            var source = response.source();

            return Optional.ofNullable(source);
        } catch (ElasticsearchException e) {
            // This is expected when the index is not available
            if (e.status() == 404) {
                log.trace("Index for language [{}] not found: {}", language, e.toString());
            } else {
                throw e;
            }
        } catch (IOException e) {
            log.error("Failed to retrieve study [{}]: {}", id, e.toString());
        }

        return Optional.empty();
    }

    /**
     * Gets a {@link SearchRequest.Builder} for the language specified.
     *
     * @param language the language to get results for.
     */
    private SearchRequest.Builder getSearchRequest(String language, Query query) {
        return new SearchRequest.Builder()
            .index(String.format(INDEX_NAME_TEMPLATE, language))
            .query(query);
    }

    @Override
    public Optional<LocalDate> getMostRecentLastModified() {

        var request = new SearchRequest.Builder().size(1).sort(
            new SortOptions.Builder().field(
                new FieldSort.Builder().field(LAST_MODIFIED_FIELD).order(SortOrder.Desc).build()
            ).build()
        ).build();

        SearchResponse<CMMStudyOfLanguage> response;
        try {
            response = esClient().search(request, CMMStudyOfLanguage.class);
        } catch (IOException e) {
            log.error("IO Error when retrieving last modified study: {}", e.toString());
            return Optional.empty();
        }

        CMMStudyOfLanguage study = null;

        var hits = response.hits().hits();
        if (!hits.isEmpty()) {
            study = hits.getFirst().source();
        }
        if (study == null) {
            return Optional.empty();
        }

        try {
            var localDate = TimeUtility.getTimeFormat(study.lastModified(), LocalDate::from);
            return Optional.of(localDate);
        } catch (DateTimeException e) {
            log.error("[{}] lastModified field is not a valid ISO date: {}", study.id(), e.toString());
            return Optional.empty();
        }
    }

    /**
     * Creates an index with the given name. If the index already exists, then no operation is performed.
     *
     * @param indexName the name of the index to create.
     * @throws IndexingException if an error occurred during index creation.
     */
    private void createIndex(String indexName) throws IndexingException {

        try {
            if (esClient().indices().exists(ExistsRequest.of(r -> r.index(indexName))).value()) {
                log.debug("[{}] index name already exists, Skipping creation.", indexName);
                return;
            }
        } catch (ElasticsearchException | IOException e) {
            throw new IndexingException(e);
        }

        log.debug("[{}] index name does not exist and will be created", indexName);

        final IndexSettings settings;
        final TypeMapping mappings;

        try {

            // Load language specific settings
            String langCode = indexName.substring(indexName.lastIndexOf('_') + 1);
            var settingsTemplate = ResourceHandler.getResourceAsString("elasticsearch/settings/settings_" + INDEX_TYPE + "_" + langCode + ".json");
            var settingsString = String.format(settingsTemplate, esConfigProps.getNumberOfShards(), esConfigProps.getNumberOfReplicas());
            settings = new IndexSettings.Builder().withJson(new StringReader(settingsString)).build();

            // Load mappings
            try (var mappingsStream = ResourceHandler.getResourceAsStream(MAPPINGS_JSON)) {
                mappings = new TypeMapping.Builder().withJson(mappingsStream).build();
            }

        } catch (IOException e) {
            throw new IndexCreationFailedException("Couldn't load settings for Elasticsearch", indexName, e);
        }


        log.trace("[{}] custom index creation: Settings: \n{}\nMappings:\n{}", indexName, settings, mappings);

        // Create the index and set the mappings
        var indexCreationRequest = new CreateIndexRequest.Builder()
            .index(indexName)
            .settings(settings)
            .mappings(mappings)
            .build();

        try {
            var response = esClient().indices().create(indexCreationRequest);
            if (response.acknowledged()) {
                log.info("[{}] Index created.", indexName);

                // Wait until the index is ready
                esClient().indices().open(new OpenRequest.Builder().index(indexName).build());
            } else {
                throw new IndexCreationFailedException(indexName);
            }
        } catch (ElasticsearchException e) {
            // Check if the index already exists
            if (!e.getMessage().contains("resource_already_exists_exception")) {
                throw new IndexCreationFailedException(indexName, e);
            }
        } catch (IOException e) {
            throw new IndexCreationFailedException("Index creation failed", indexName, e);
        }
    }

    /**
     * Performs reindexing for all themes. Searches for theme directories in
     * the resources folder and calls processing for each theme directory found.
     */
    @Override
    public void reindexAllThemes() {
        URL themesUrl = getClass().getClassLoader().getResource(REINDEX_THEMES_DIR);

        if (themesUrl == null) {
            log.info("Themes directory not found, skipping reindexing.");
            return;
        }

        Path themesDir = Paths.get(themesUrl.getPath());

        if (!Files.isDirectory(themesDir)) {
            log.info("Themes directory is not a valid directory, skipping reindexing.");
            return;
        }

        try (Stream<Path> themeDirsStream = Files.list(themesDir)) {
            var themeDirs = themeDirsStream.filter(Files::isDirectory)
                                           .collect(Collectors.toList());

            // If no theme directories found, skip reindexing
            if (themeDirs.isEmpty()) {
                log.info("No theme directories found in themes directory, skipping reindexing.");
                return;
            }

            // Process each theme directory
            for (Path themeDir : themeDirs) {
                processThemeReindexing(themeDir);
            }
        } catch (IOException e) {
            log.error("Error listing theme directories: {}", e.toString());
        }
    }

    /**
     * Processes reindexing for a specific theme directory.Looks for files that
     * start with the prefix "reindex_" and end with ".json" (i.e., reindex query files).
     * For each reindex query file, it extracts the language code and constructs the
     * source and destination index names before triggering the reindexing operation.
     *
     * @param themeDir the theme directory to process
     */
    private void processThemeReindexing(Path themeDir) {
        try (Stream<Path> reindexFilesStream = Files.list(themeDir)) {
            // Filter and collect the files that match the criteria
            var reindexFiles = reindexFilesStream
                .filter(file -> file.getFileName().toString().startsWith("reindex_") && file.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());

            // If no reindex query files are found, log and skip
            if (reindexFiles.isEmpty()) {
                log.info("No reindex query files found in theme directory: {}", themeDir.getFileName());
                return;
            }

            // Process each reindex query file found
            for (Path reindexFile : reindexFiles) {
                try {
                    String filename = reindexFile.getFileName().toString();

                    // Extract language code from the filename (e.g., "reindex_abc_en.json" -> "en")
                    String langCode = filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf('.'));

                    // Set the source index by using INDEX_NAME_TEMPLATE and extracted language code
                    String sourceIndex = String.format(INDEX_NAME_TEMPLATE, langCode);

                    // Set the destination index by removing the "reindex_" prefix and ".json" extension from filename
                    String destinationIndex = filename.substring("reindex_".length(), filename.lastIndexOf(".json"));

                    log.debug("Running reindexing from {} to {} using query file {}", sourceIndex, destinationIndex, reindexFile.toAbsolutePath());

                    // Construct the path to the query JSON file and initiate the reindexing operation
                    String queryJsonFilePath = Paths.get(REINDEX_THEMES_DIR, themeDir.getFileName().toString(), reindexFile.getFileName().toString()).toString();
                    reindex(sourceIndex, destinationIndex, queryJsonFilePath);
                } catch (Exception e) {
                    log.error("Error processing reindex file {}: {}", reindexFile.toAbsolutePath(), e.toString());
                }
            }
        } catch (IOException e) {
            log.error("Error reading reindex query files in theme directory: {}: {}", themeDir.getFileName(), e.toString());
        }
    }

    /**
     * Performs the reindexing operation from a source index to a destination index
     * using the query defined in the given JSON file.
     *
     * @param sourceIndex the name of the source index
     * @param destinationIndex the name of the destination index
     * @param queryJsonFilePath the file path to the JSON query used for reindexing
     * @throws IndexingException if an error occurs during the reindexing process
     */
    @Override
    public void reindex(String sourceIndex, String destinationIndex, String queryJsonFilePath) throws IndexingException {
        try {
            // Check that the source index exists
            if (!esClient().indices().exists(ExistsRequest.of(r -> r.index(sourceIndex))).value()) {
                log.info("[{}] Source index does not exist, skipping reindexing.", destinationIndex);
                return;
            }

            ElasticsearchClient client = esClient();

            log.info("[{}] Reindexing started.", destinationIndex);

            // Delete index if it already exists
            try {
                if (esClient().indices().exists(ExistsRequest.of(r -> r.index(destinationIndex))).value()) {
                    client.indices().delete(d -> d.index(destinationIndex));
                    log.info("[{}] index already existed. Index deleted.", destinationIndex);
                }
            } catch (ElasticsearchException | IOException e) {
                throw new IndexingException(e);
            }

            // Create the destination index
            createIndex(destinationIndex);

            // Load the reindex query from JSON file
            try (InputStream queryStream = getClass().getClassLoader().getResourceAsStream(queryJsonFilePath)) {
                if (queryStream == null) {
                    log.error("Query file not found: {}", queryJsonFilePath);
                    return;
                }

                // Build the reindex request using the source and destination indices and the query JSON
                ReindexRequest reindexRequest = new ReindexRequest.Builder()
                    .source(s -> s.index(sourceIndex).withJson(queryStream))
                    .dest(d -> d.index(destinationIndex))
                    .build();

                // Execute the reindex operation
                ReindexResponse response = client.reindex(reindexRequest);

                // Log the number of documents successfully reindexed
                long totalReindexed = response.created();
                log.info("[{}] Reindexing completed. Total documents reindexed: {}.", destinationIndex, totalReindexed);
            } catch (IOException e) {
                log.error("Error loading reindex query from file: {}", queryJsonFilePath);
            }
        } catch (IOException | ElasticsearchException e) {
            log.error("Error executing reindex operation from {} to {} using query file {}: {}",
                    sourceIndex, destinationIndex, queryJsonFilePath, e.toString());
            throw new IndexingException("Error during reindex operation", e);
        }
    }
}
