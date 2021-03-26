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

import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.TimeUtility;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
    private static final String INDEX_NAME_TEMPLATE = INDEX_TYPE + "_%s";

    /**
     * The amount of studies to BulkIndex at once
     */
    private static final int INDEX_COMMIT_SIZE = 500;

    private final ElasticsearchTemplate esTemplate;
    private final ESConfigurationProperties esConfig;
    private final CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter;

    @Autowired
    public ESIngestService(ElasticsearchTemplate esTemplate, ESConfigurationProperties esConfig, CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter) {
        this.esTemplate = esTemplate;
        this.esConfig = esConfig;
        this.cmmStudyOfLanguageConverter = cmmStudyOfLanguageConverter;
    }

    @Override
    public boolean bulkIndex(Collection<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode) {
        var indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);

        if (createIndex(indexName)) {

            // Allocate all space needed upfront, this avoids unnecessary resizes
            var queries = new ArrayList<IndexQuery>(Integer.min(languageCMMStudiesMap.size(), INDEX_COMMIT_SIZE));

            log.debug("[{}] Indexing...", indexName);

            for (CMMStudyOfLanguage cmmStudyOfLanguage : languageCMMStudiesMap) {
                var indexQuery = getIndexQuery(cmmStudyOfLanguage, indexName);
                queries.add(indexQuery);
                if (queries.size() == INDEX_COMMIT_SIZE) {
                    log.trace("[{}] Bulk Indexing {} studies", indexName, INDEX_COMMIT_SIZE);
                    executeBulk(queries);
                    queries.clear();
                }
            }

            // Commit all remaining studies
            if (!queries.isEmpty()) {
                log.trace("[{}] Bulk Indexing {} studies", indexName, queries.size());
                executeBulk(queries);
            }

            log.debug("[{}] Indexing completed.", indexName);
            return true;
        }

        return false;
    }

    @Override
    public void bulkDelete(Collection<CMMStudyOfLanguage> cmmStudiesToDelete, String languageIsoCode) {

        var deleteQuery = new DeleteQuery();

        // Set the index
        var indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);
        deleteQuery.setIndex(indexName);

        // Extract the ids from the studies, and add them to the delete query
        var studyIds = cmmStudiesToDelete.stream().map(CMMStudyOfLanguage::getId).toArray(String[]::new);
        deleteQuery.setType(INDEX_TYPE);
        deleteQuery.setQuery(QueryBuilders.idsQuery().addIds(studyIds));

        // Perform the deletion
        esTemplate.delete(deleteQuery);
    }

    @Override
    public long getTotalHitCount(String language) {
        var matchAllSearchRequest = getMatchAllSearchRequest(language).setSize(0);
        var response = matchAllSearchRequest.get();
        var hits = response.getHits();
        return hits.getTotalHits();
    }

    @Override
    public ElasticsearchSet<CMMStudyOfLanguage> getAllStudies(String language) {
        log.debug("Getting all studies for language [{}]", language);
        return new ElasticsearchSet<>(getMatchAllSearchRequest("*"), esTemplate.getClient(), cmmStudyOfLanguageConverter.getReader());
    }

    @Override
    public Optional<CMMStudyOfLanguage> getStudy(String id, String language) {
        log.trace("Retrieving study [{}], language [{}]", id, language);

        try {
            var response = esTemplate.getClient().prepareGet()
                    .setIndex(String.format(INDEX_NAME_TEMPLATE, language))
                    .setType(INDEX_TYPE)
                    .setId(id)
                    .get();

            var sourceAsBytes = response.getSourceAsBytes();

            if (sourceAsBytes != null) {
                return Optional.of(cmmStudyOfLanguageConverter.getReader().readValue(sourceAsBytes));
            }
        } catch (IndexNotFoundException e) {
            // This is expected when the index is not available
            log.trace("Index for language [{}] not found: {}", language, e.toString());
        } catch (IOException e) {
            log.error("Failed to retrieve study [{}]: {}", id, e.toString());
        }

        return Optional.empty();
    }

    /**
     * Gets a match all {@link SearchRequestBuilder} for the language specified.
     *
     * @param language the language to get results for.
     */
    private SearchRequestBuilder getMatchAllSearchRequest(String language) {
        return esTemplate.getClient().prepareSearch(String.format(INDEX_NAME_TEMPLATE, language))
                .setTypes(INDEX_TYPE)
                .setQuery(QueryBuilders.matchAllQuery());
    }

    @Override
    public Optional<LocalDateTime> getMostRecentLastModified() {

        SearchResponse response = getMatchAllSearchRequest("*")
                .addSort(LAST_MODIFIED_FIELD, SortOrder.DESC)
                .setSize(1)
                .get();

        SearchHit[] hits = response.getHits().getHits();
        try {
            if (hits.length != 0) {
                CMMStudyOfLanguage study = cmmStudyOfLanguageConverter.getReader().readValue(hits[0].getSourceRef().streamInput());
                try {
                    var localDateTime = TimeUtility.getLocalDateTime(study.getLastModified());
                    return Optional.of(localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0));
                } catch (DateNotParsedException e) {
                    log.error("[{}] lastModified field is not a valid ISO date", study.getId());
                }
            }
        } catch (IOException e) {
            log.error("Couldn't decode {} into an instance of {}: {}", hits[0].getId(), CMMStudyOfLanguage.class.getName(), e.toString());
        }
        return Optional.empty();
    }

    /**
     * Creates an {@link IndexQuery} for the given {@link CMMStudyOfLanguage} and sets the target index to
     * the given index name.
     *
     * @param cmmStudyOfLanguage the study to index
     * @param indexName          the index to save the study to
     */
    private IndexQuery getIndexQuery(CMMStudyOfLanguage cmmStudyOfLanguage, String indexName) {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(cmmStudyOfLanguage.getId());
        indexQuery.setObject(cmmStudyOfLanguage);
        indexQuery.setIndexName(indexName);
        indexQuery.setType(INDEX_TYPE);
        return indexQuery;
    }

    /**
     * Creates an index with the given name. If the index already exists, then no operation is performed.
     *
     * @param indexName the name of the index to create
     * @return {@code true} if the index was created or if the index already existed,
     * {@code false} if an error occurred during index creation
     */
    private boolean createIndex(String indexName) {

        if (esTemplate.indexExists(indexName)) {
            log.debug("[{}] index name already exists, Skipping creation.", indexName);
            return true;
        }

        log.debug("[{}] index name does not exist and will be created", indexName);

        try {

            // Load language specific settings
            var settingsTemplate = ResourceHandler.getResourceAsString(String.format("elasticsearch/settings/settings_%s.json", indexName));
            var settings = String.format(settingsTemplate, esConfig.getNumberOfShards(), esConfig.getNumberOfReplicas());

            // Load mappings
            var mappings = ResourceHandler.getResourceAsString(String.format("elasticsearch/mappings/mappings_%s.json", INDEX_TYPE));

            log.trace("[{}] custom index creation: Settings: \n{}\nMappings:\n{}", indexName, settings, mappings);

            // Create the index and set the mappings
            if (esTemplate.createIndex(indexName, settings) && esTemplate.putMapping(indexName, INDEX_TYPE, mappings)) {
                log.info("[{}] Index created.", indexName);
                return true;
            } else {
                log.error("[{}] Index creation failed!", indexName);
                esTemplate.deleteIndex(indexName);
                return false;
            }

        } catch (IOException e) {
            log.error("[{}] Couldn't load settings for Elasticsearch: {}", indexName, e.toString());
        }

        return false;
    }

    /**
     * Index the given list of {@link IndexQuery}s into Elasticsearch.
     *
     * @param queries the queries to index
     */
    private void executeBulk(List<IndexQuery> queries) {
        try {
            esTemplate.bulkIndex(queries);
        } catch (ElasticsearchException e) {
            var failedDocuments = new StringBuilder();
            e.getFailedDocuments().forEach((key, value) -> failedDocuments.append(String.format("%nFailed to index id [%s]: [%s]", key, value)));
            log.error("Failed to index all documents: {}: {}", e.toString(), failedDocuments);
        }
    }
}
