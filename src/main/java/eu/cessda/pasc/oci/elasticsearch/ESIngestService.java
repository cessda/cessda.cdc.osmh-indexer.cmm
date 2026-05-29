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
package eu.cessda.pasc.oci.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    private static final String REINDEXED_DOCUMENT = "Reindexed document [{}] from [{}] to [{}].";

    /**
     * The amount of studies to BulkIndex at once
     */
    private static final int INDEX_COMMIT_SIZE = 500;

    private final ESConfigurationProperties esConfigProps;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

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
                || !((Rest5ClientTransport) this.client._transport()).restClient().isRunning()) {
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

        createIndex(indexName, languageIsoCode);

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
        bulkDeleteInternal(cmmStudiesToDelete, String.format(INDEX_NAME_TEMPLATE, languageIsoCode));
    }

    /**
     * Delete the specified studies from a specified index.
     * <p>
     * This method allows for specifying the index in which items will be deleted.
     * This is an internal method and is not part of the public interface.
     *
     * @param cmmStudiesToDelete the collection of studies to delete.
     * @param indexName          the index to delete from.
     * @throws IndexingException if an error occurs connecting to Elasticsearch.
     */
    private void bulkDeleteInternal(Collection<CMMStudyOfLanguage> cmmStudiesToDelete, String indexName) throws IndexingException {
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

        var request = new SearchRequest.Builder()
            .index(INDEX_TYPE + "_*")
            .size(1)
            .sort(new SortOptions.Builder().field(
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
     * @param langCode the language code to use for the index.
     * @throws IndexingException if an error occurred during index creation.
     */
    private void createIndex(String indexName, String langCode) throws IndexingException {

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
        Resource[] resources;
        try {
            resources = resolver.getResources("classpath*:" + REINDEX_THEMES_DIR + "/**/reindex_*.json");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        var allReindexedIdsPerIndex = new HashMap<String, Set<String>>();

        for (var reindexJson : resources) {
            // Split the theme path into separate elements
            String[] splitPath;
            try {
                splitPath = reindexJson.getURL().getPath().split("/");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            // Theme name is the name of the parent directory where the JSON is found
            String themeName = splitPath[splitPath.length - 2];

            // Get the filename
            String filename = splitPath[splitPath.length - 1];

            try {
                // Process theme file
                Map<String, Set<String>> reindexedIdsPerIndex = processThemeReindexing(reindexJson, themeName, filename);

                for(var entry : reindexedIdsPerIndex.entrySet()) {
                    allReindexedIdsPerIndex.merge(entry.getKey(), entry.getValue(), (a, b) -> {
                        a.addAll(b);
                        return a;
                    });
                }

                // After reindexing is done, clean up stale documents
                log.debug("Reindexing completed for theme directory [{}", themeName);
            } catch (IndexingException e) {
                log.error("[{}]: Error processing reindex file [{}]: {}", themeName, reindexJson, e.toString());
            }
        }

        try {
            // Clean up stale documents
            log.info("Cleaning up stale documents");
            reindexCleanup(allReindexedIdsPerIndex);
        } catch (IndexingException e) {
            log.error("Error cleaning reindex file: {}", e.toString());
        }
    }

    /**
     * Processes reindexing for a specific theme directory. Looks for files that
     * start with the prefix "reindex_" and end with ".json" (i.e., reindex query files).
     * For each reindex query file, it extracts the language code and constructs the
     * source and destination index names before triggering the reindexing operation.
     *
     * @param themeDir the theme JSON to process
     *
     * @return a map of processed document IDs per index
     */
    private Map<String, Set<String>> processThemeReindexing(Resource themeDir, String themeName, String filename) throws IndexingException {
        // Extract language code from the filename (e.g., "reindex_abc_en.json" -> "en")
        String langCode = filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf('.'));

        // Set the source index by using INDEX_NAME_TEMPLATE and extracted language code
        String sourceIndex = String.format(INDEX_NAME_TEMPLATE, langCode);

        log.debug("Running reindexing from [{}] to [{}] using query file [{}]", sourceIndex, themeName, themeDir);

        // Collect reindexed IDs for this reindex operation
        return reindex(sourceIndex, themeName, themeDir);
    }

    /**
     * Performs the reindexing operation from a source index to a destination index matching with the query
     * defined in the given JSON file and then indexing matched studies in all available languages with the same ID.
     *
     * @param sourceIndex the name of the source index
     * @param destinationThemeIndex the theme name for the destination index
     * @param queryJsonFilePath the file path to the JSON query used for reindexing
     * @return a map of reindexed IDs per index, where each entry maps an index to a set of document IDs that were reindexed into that index.
     * @throws IndexingException if an error occurs during the reindexing process
     */
    private Map<String, Set<String>> reindex(String sourceIndex, String destinationThemeIndex, Resource queryJsonFilePath) throws IndexingException {
        Map<String, Set<String>> reindexedIdsPerIndex = new HashMap<>();
        try {

            // Check if the source index exists
            var existsRequest = ExistsRequest.of(r -> r.index(sourceIndex));
            if (!esClient().indices().exists(existsRequest).value()) {
                log.debug("Source index [{}] does not exist, skipping reindexing.", sourceIndex);
                return reindexedIdsPerIndex;
            }

            log.debug("Reindexing started for source [{}] with theme [{}].", sourceIndex, queryJsonFilePath);

            String currentLang = sourceIndex.substring(sourceIndex.lastIndexOf('_') + 1);

            // Maps to track reindex counts and IDs
            int reindexCount = 0;

            // Load the reindex query from the JSON file
            try (InputStream queryStream = queryJsonFilePath.getInputStream()) {

                // Execute search query on the source index
                var searchResponse = new ElasticsearchSet<>(
                    new SearchRequest.Builder().index(sourceIndex).withJson(queryStream),
                    esClient(),
                    CMMStudyOfLanguage.class
                );

                for (CMMStudyOfLanguage source : searchResponse) {

                    // Check if localized destination index exists; create if not
                    var destinationIndex = destinationThemeIndex + "_" + currentLang;
                    createIndex(destinationIndex, currentLang);

                    // Reindex the document into the destination index
                    indexDocument(destinationIndex, source);
                    log.debug(REINDEXED_DOCUMENT, source.id(), sourceIndex, destinationIndex);

                    // Add the identifier to the list of processed documents
                    reindexCount++;
                    reindexedIdsPerIndex.computeIfAbsent(destinationIndex, k -> new HashSet<>()).add(source.id());

                    // Process other language
                    for (String lang : source.langAvailableIn()) {

                        // Skip retrieving the source twice
                        if (currentLang.equals(lang)) {
                            continue;
                        }

                        String localizedSourceIndex = String.format(INDEX_NAME_TEMPLATE, lang);
                        String localizedDestinationIndex = destinationThemeIndex + "_" + lang;

                        // Check if localized destination index exists; create if not
                        createIndex(localizedDestinationIndex, lang);

                        // Fetch the localized document
                        GetResponse<CMMStudyOfLanguage> localizedResponse;
                        try {
                            localizedResponse = esClient().get(r -> r
                                    .index(localizedSourceIndex)
                                    .id(source.id()), // Fetch by document ID
                                CMMStudyOfLanguage.class
                            );
                        } catch (ElasticsearchException e) {
                            if ("index_not_found_exception".equals(e.error().type())) {
                                // skip this language
                                log.warn("[{}] Expected index [{}] not found whilst reindexing [{}]. Did a previous indexing run not complete?", destinationThemeIndex,  localizedSourceIndex, source.id());
                                continue;
                            } else {
                                throw e;
                            }
                        }

                        if (!localizedResponse.found()) {
                            log.warn("[{}] Expected document [{}] not found whilst reindexing [{}]. Did a previous indexing run not complete?", destinationThemeIndex, source.id(), localizedSourceIndex);
                            continue;
                        }

                        // Reindex the localized document into the localized destination index
                        indexDocument(localizedDestinationIndex, localizedResponse.source());
                        log.debug(REINDEXED_DOCUMENT, source.id(), localizedSourceIndex, localizedDestinationIndex);

                        // Track reindex stats and matched IDs
                        reindexCount++;
                        reindexedIdsPerIndex.computeIfAbsent(localizedDestinationIndex, k -> new HashSet<>()).add(source.id());
                    }
                }

                // Log reindex counts for each index
                log.info("[{}] Reindexing completed from source [{}]: {} documents reindexed", destinationThemeIndex, sourceIndex , reindexCount);

            }

        } catch (ElasticsearchException | IOException e) {
            throw new IndexingException(e);
        }

        return reindexedIdsPerIndex;
    }

    private void indexDocument(String index, CMMStudyOfLanguage source) throws IOException {
        IndexRequest<CMMStudyOfLanguage> indexRequest = new IndexRequest.Builder<CMMStudyOfLanguage>()
            .index(index)
            .id(source.id())
            .document(source)
            .build();
        esClient().index(indexRequest);
    }

    /**
     * Deletes stale documents from the indices found in the given reindexedIdsPerIndex.
     * Stale documents are those that were not reindexed, based on the IDs tracked in the reindexing process.
     *
     * @param reindexedIdsPerIndex A map of reindexed IDs per index. Each entry maps an index to a set of document IDs that were reindexed into that index.
     */
    private void reindexCleanup(Map<String, Set<String>> reindexedIdsPerIndex) throws IndexingException {
        try {
            for (Map.Entry<String, Set<String>> entry : reindexedIdsPerIndex.entrySet()) {
                String index = entry.getKey();
                Set<String> reindexedIds = entry.getValue();

                // Refresh the index to ensure up-to-date queries
                esClient().indices().refresh(r -> r.index(index));

                var existingDocs = new ElasticsearchSet<>(
                    new SearchRequest.Builder().index(index),
                    esClient(),
                    CMMStudyOfLanguage.class
                );

                // Create a list of studies to delete
                List<CMMStudyOfLanguage> studiesToDelete = new ArrayList<>();
                for (CMMStudyOfLanguage hit : existingDocs) {
                    // If this ID is not part of the reindexed documents, it's stale
                    if (!reindexedIds.contains(hit.id())) {
                        studiesToDelete.add(hit);
                    }
                }

                // Perform bulk delete
                bulkDeleteInternal(studiesToDelete, index);

                log.info("[{}]: Deleted {} stale docs.", index, studiesToDelete.size());
            }
        } catch (IOException | ElasticsearchException e) {
            throw new IndexingException(e);
        }
    }
}
