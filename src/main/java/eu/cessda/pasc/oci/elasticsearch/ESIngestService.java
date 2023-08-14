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
package eu.cessda.pasc.oci.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.*;
import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.TimeUtility;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
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

    /**
     * The amount of studies to BulkIndex at once
     */
    private static final int INDEX_COMMIT_SIZE = 500;

    private final ElasticsearchClient esClient;
    private final ESConfigurationProperties esConfig;

    @Autowired
    public ESIngestService(ElasticsearchClient esClient, ESConfigurationProperties esConfig) {
        this.esClient = esClient;
        this.esConfig = esConfig;
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
            var response = esClient.bulk(request);
            if (response.errors()) {
                for (var item : response.items()) {
                    if (item.error() != null && "strict_dynamic_mapping_exception".equals(item.error().type())) {
                        // Attempt updating field mappings
                        var updateMappingRequest = new PutMappingRequest.Builder()
                            .withJson(ResourceHandler.getResourceAsStream(MAPPINGS_JSON))
                            .index(indexName)
                            .build();

                        esClient.indices().putMapping(updateMappingRequest);

                        // Retry indexing with updated mappings.
                        response = esClient.bulk(request);
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
        var response = esClient.count(matchAllCountRequest);
        return response.count();
    }

    @Override
    public ElasticsearchSet<CMMStudyOfLanguage> getAllStudies(String language) {
        log.debug("Getting all studies for language [{}]", language);
        return new ElasticsearchSet<>(
            getSearchRequest("*", new MatchAllQuery.Builder().build()._toQuery()),
            esClient,
            CMMStudyOfLanguage.class
        );
    }

    @Override
    public ElasticsearchSet<CMMStudyOfLanguage> getStudiesByRepository(String repository, String language) {
        log.debug("Getting all studies for repository [{}] with language [{}]", repository, language);
        var repositorySearchRequest = getSearchRequest(language,
            new TermQuery.Builder().field("code").value(repository).build()._toQuery()
        );
        return new ElasticsearchSet<>(repositorySearchRequest, esClient, CMMStudyOfLanguage.class);
    }

    @Override
    public Optional<CMMStudyOfLanguage> getStudy(String id, String language) {
        log.trace("Retrieving study [{}], language [{}]", id, language);

        try {
            var request = new co.elastic.clients.elasticsearch.core.GetRequest.Builder()
                .index(String.format(INDEX_NAME_TEMPLATE, language)).id(id).build();
            var response = esClient.get(request, CMMStudyOfLanguage.class);

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
    public Optional<LocalDateTime> getMostRecentLastModified() {

        var request = new SearchRequest.Builder().size(1).sort(
            new SortOptions.Builder().field(
                new FieldSort.Builder().field(LAST_MODIFIED_FIELD).order(SortOrder.Desc).build()
            ).build()
        ).build();

        SearchResponse<CMMStudyOfLanguage> response;
        try {
            response = esClient.search(request, CMMStudyOfLanguage.class);
        } catch (IOException e) {
            log.error("IO Error when retrieving last modified study: {}", e.toString());
            return Optional.empty();
        }

        CMMStudyOfLanguage study = null;

        var hits = response.hits().hits();
        if (!hits.isEmpty()) {
            study = hits.get(0).source();
        }
        if (study == null) {
            return Optional.empty();
        }

        try {
            var localDateTime = TimeUtility.getLocalDateTime(study.lastModified());
            return Optional.of(localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0));
        } catch (DateNotParsedException e) {
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
            if (esClient.indices().exists(ExistsRequest.of(r -> r.index(indexName))).value()) {
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
            var settingsTemplate = ResourceHandler.getResourceAsString("elasticsearch/settings/settings_" + indexName + ".json");
            var settingsString = String.format(settingsTemplate, esConfig.getNumberOfShards(), esConfig.getNumberOfReplicas());
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
            var response = esClient.indices().create(indexCreationRequest);
            if (response.acknowledged()) {
                log.info("[{}] Index created.", indexName);

                // Wait until the index is ready
                esClient.indices().open(new OpenRequest.Builder().index(indexName).build());
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
}
