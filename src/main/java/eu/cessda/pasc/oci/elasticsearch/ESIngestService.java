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
package eu.cessda.pasc.oci.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.TimeUtility;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.elasticsearch.client.RequestOptions.DEFAULT;

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

    private final RestHighLevelClient esClient;
    private final ESConfigurationProperties esConfig;
    private final CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter;

    @Autowired
    public ESIngestService(RestHighLevelClient esClient, ESConfigurationProperties esConfig, CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter) {
        this.esClient = esClient;
        this.esConfig = esConfig;
        this.cmmStudyOfLanguageConverter = cmmStudyOfLanguageConverter;
    }

    @Override
    public boolean bulkIndex(Collection<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode) throws IOException {
        var indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);

        if (createIndex(indexName)) {
            log.debug("[{}] Indexing {} studies", indexName, languageCMMStudiesMap.size());

            var bulkIndexQuery = new BulkRequest();

            for (var study : languageCMMStudiesMap) {
                try {
                    var json = cmmStudyOfLanguageConverter.getWriter().writeValueAsBytes(study);
                    var indexRequest = Requests.indexRequest(indexName)
                        .id(study.getId())
                        .source(json, XContentType.JSON);

                    bulkIndexQuery.add(indexRequest);

                    if (bulkIndexQuery.numberOfActions() == INDEX_COMMIT_SIZE) {
                        log.trace("[{}] Bulk Indexing {} studies", indexName, INDEX_COMMIT_SIZE);
                        indexBulkRequest(indexName, bulkIndexQuery);

                        // Clear the bulk request
                        bulkIndexQuery = new BulkRequest();
                    }
                } catch (JsonProcessingException e) {
                    log.warn("[{}] Failed to convert {} into JSON: {}", indexName, study.getId(), e.toString());
                }
            }

            // Commit all remaining studies
            if (bulkIndexQuery.numberOfActions() > 0) {
                log.trace("[{}] Bulk Indexing {} studies", indexName, bulkIndexQuery.numberOfActions());
                indexBulkRequest(indexName, bulkIndexQuery);
            }

            log.debug("[{}] Indexing completed.", indexName);
            return true;
        }

        return false;
    }

    private void indexBulkRequest(String indexName, BulkRequest request) throws IOException {
        var response = esClient.bulk(request, DEFAULT);
        if (response.hasFailures()) {
            for (var item : response.getItems()) {
                if (item.isFailed() && item.getFailure().getCause().getMessage().contains("strict_dynamic_mapping_exception")) {
                    // Attempt updating field mappings
                    var updateMappingRequest = new PutMappingRequest(indexName)
                        .source(ResourceHandler.getResourceAsString(MAPPINGS_JSON), XContentType.JSON);
                    esClient.indices().putMapping(updateMappingRequest, DEFAULT);

                    // Retry indexing with updated mappings.
                    response = esClient.bulk(request, DEFAULT);
                    break;
                }
            }

            if (response.hasFailures()) {
                log.warn("[{}] {}", indexName, response.buildFailureMessage());
            }
        }
    }

    @Override
    public void bulkDelete(Collection<CMMStudyOfLanguage> cmmStudiesToDelete, String languageIsoCode) throws IOException {
        // Set the index
        var indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);

        // Extract the ids from the studies, and add them to the delete query
        var deleteBulkRequest = cmmStudiesToDelete.stream().map(CMMStudyOfLanguage::getId)
            .map(id -> Requests.deleteRequest(indexName).id(id))
            .collect(BulkRequest::new, BulkRequest::add, (a,b) -> a.add(b.requests()));

        // Perform the deletion
        if (deleteBulkRequest.numberOfActions() > 0) {
            indexBulkRequest(indexName, deleteBulkRequest);
        }
    }

    @Override
    public long getTotalHitCount(String language) throws IOException {
        var matchAllCountRequest = new CountRequest(String.format(INDEX_NAME_TEMPLATE, language));
        var response = esClient.count(matchAllCountRequest, DEFAULT);
        return response.getCount();
    }

    @Override
    public ElasticsearchSet<CMMStudyOfLanguage> getAllStudies(String language) {
        log.debug("Getting all studies for language [{}]", language);
        return new ElasticsearchSet<>(
            getSearchRequest("*", new SearchSourceBuilder().query(QueryBuilders.matchAllQuery())),
            esClient,
            cmmStudyOfLanguageConverter.getReader()
        );
    }

    @Override
    public ElasticsearchSet<CMMStudyOfLanguage> getStudiesByRepository(String repository, String language) {
        log.debug("Getting all studies for repository [{}] with language [{}]", repository, language);
        var repositorySearchRequest = getSearchRequest(language,
            new SearchSourceBuilder().query(new TermQueryBuilder("code", repository))
        );
        return new ElasticsearchSet<>(repositorySearchRequest, esClient, cmmStudyOfLanguageConverter.getReader());
    }

    @Override
    public Optional<CMMStudyOfLanguage> getStudy(String id, String language) {
        log.trace("Retrieving study [{}], language [{}]", id, language);

        try {
            var request = Requests.getRequest(String.format(INDEX_NAME_TEMPLATE, language)).id(id);
            var response = esClient.get(request, DEFAULT);

            var sourceAsBytes = response.getSourceAsBytes();

            if (sourceAsBytes != null) {
                return Optional.of(cmmStudyOfLanguageConverter.getReader().readValue(sourceAsBytes));
            }
        } catch (ElasticsearchStatusException e) {
            // This is expected when the index is not available
            if (e.status().equals(RestStatus.NOT_FOUND)) {
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
     * Gets a {@link SearchRequest} for the language specified.
     *
     * @param language the language to get results for.
     */
    private SearchRequest getSearchRequest(String language, SearchSourceBuilder source) {
        return new SearchRequest(String.format(INDEX_NAME_TEMPLATE, language)).source(source);
    }

    @Override
    @SuppressWarnings("java:S1141")
    public Optional<LocalDateTime> getMostRecentLastModified() {

        var request = getSearchRequest("*",
                new SearchSourceBuilder().size(1).sort(LAST_MODIFIED_FIELD, SortOrder.DESC)
        );

        SearchResponse response;
        try {
            response = esClient.search(request, DEFAULT);
        } catch (IOException e) {
            log.error("IO Error when retrieving last modified study: {}", e.toString());
            return Optional.empty();
        }

        var hits = response.getHits().getHits();
        CMMStudyOfLanguage study;
        try {
            if (hits.length != 0) {
                study = cmmStudyOfLanguageConverter.getReader().readValue(hits[0].getSourceRef().streamInput());
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            log.error("Couldn't decode {} into an instance of {}: {}", hits[0].getId(), CMMStudyOfLanguage.class.getName(), e.toString());
            return Optional.empty();
        }

        try {
            var localDateTime = TimeUtility.getLocalDateTime(study.getLastModified());
            return Optional.of(localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0));
        } catch (DateNotParsedException e) {
            log.error("[{}] lastModified field is not a valid ISO date: {}", study.getId(), e.toString());
            return Optional.empty();
        }
    }

    /**
     * Creates an index with the given name. If the index already exists, then no operation is performed.
     *
     * @param indexName the name of the index to create
     * @return {@code true} if the index was created or if the index already existed,
     * {@code false} if an error occurred during index creation
     */
    private boolean createIndex(String indexName) throws IOException {

        if (esClient.indices().exists(new GetIndexRequest(indexName), DEFAULT)) {
            log.debug("[{}] index name already exists, Skipping creation.", indexName);
            return true;
        }

        log.debug("[{}] index name does not exist and will be created", indexName);

        final String settings;
        final String mappings;

        try {

            // Load language specific settings
            var settingsTemplate = ResourceHandler.getResourceAsString("elasticsearch/settings/settings_" + indexName + ".json");
            settings = String.format(settingsTemplate, esConfig.getNumberOfShards(), esConfig.getNumberOfReplicas());

            // Load mappings
            mappings = ResourceHandler.getResourceAsString(MAPPINGS_JSON);

        } catch (IOException e) {
            log.error("[{}] Couldn't load settings for Elasticsearch: {}", indexName, e.toString());
            return false;
        }


        log.trace("[{}] custom index creation: Settings: \n{}\nMappings:\n{}", indexName, settings, mappings);

        // Create the index and set the mappings
        var indexCreationRequest = new CreateIndexRequest(indexName)
            .settings(settings, XContentType.JSON)
            .mapping(mappings, XContentType.JSON);

        try {
            var response = esClient.indices().create(indexCreationRequest, DEFAULT);
            if (response.isAcknowledged()) {
                log.info("[{}] Index created.", indexName);

                // Wait until the index is ready
                esClient.indices().open(Requests.openIndexRequest(indexName), DEFAULT);

                return true;
            } else {
                log.error("[{}] Index creation failed!", indexName);
                return false;
            }
        } catch (ElasticsearchException e) {
          if (e.getMessage().contains("resource_already_exists_exception")) {
              // Index exists, continue
              return true;
          } else {
              throw e;
          }
        } catch (IOException e) {
            log.error("[{}] Index creation failed: {}", indexName, e.toString());
            return false;
        }
    }

}
