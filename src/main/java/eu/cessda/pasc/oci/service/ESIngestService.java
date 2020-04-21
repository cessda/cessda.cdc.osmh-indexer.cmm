/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.helpers.TimeUtility;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

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
    private final FileHandler fileHandler;
    private final ESConfigurationProperties esConfig;
    private final CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter;

    @Autowired
    public ESIngestService(ElasticsearchTemplate esTemplate, FileHandler fileHandler, ESConfigurationProperties esConfig, CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter) {
        this.esTemplate = esTemplate;
        this.fileHandler = fileHandler;
        this.esConfig = esConfig;
        this.cmmStudyOfLanguageConverter = cmmStudyOfLanguageConverter;
    }

    @Override
    public boolean bulkIndex(List<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode) {
        String indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);
        boolean isSuccessful = true;
        int counter = 0;

        if (prepareIndex(indexName)) {
            List<IndexQuery> queries = new ArrayList<>();
            log.info("Indexing [{}] index.", indexName);
            for (CMMStudyOfLanguage cmmStudyOfLanguage : languageCMMStudiesMap) {
                IndexQuery indexQuery = getIndexQuery(indexName, cmmStudyOfLanguage);
                queries.add(indexQuery);
                counter++;
                if (counter % INDEX_COMMIT_SIZE == 0) {
                    executeBulk(queries);
                    queries.clear();
                    log.debug("[{}] Current bulkIndex counter [{}].", indexName, counter);
                }
            }
            if (!queries.isEmpty()) {
                log.debug("[{}] Current bulkIndex counter [{}].", indexName, counter);
                executeBulk(queries);
            }
            esTemplate.refresh(indexName);
            log.info("[{}] BulkIndex completed.", languageIsoCode);
        } else {
            isSuccessful = false;
        }
        return isSuccessful;
    }

    @Override
    public long getTotalHitCount() {
        return getTotalHitCount("*");
    }

    @Override
    public long getTotalHitCount(String language) {
        SearchRequestBuilder matchAllSearchRequest = getMatchAllSearchRequest(language).setSize(0);
        SearchResponse response = matchAllSearchRequest.get();
        SearchHits hits = response.getHits();
        return hits.getTotalHits();
    }

    @Override
    public Map<String, Integer> getHitCountPerRepository() {
        var map = new HashMap<String, Integer>();
        var timeout = new TimeValue(Duration.ofSeconds(60).toMillis());
        SearchResponse response = getMatchAllSearchRequest("*").setScroll(timeout).get();

        do {
            for (SearchHit searchHit : response.getHits().getHits()) {
                try {
                    CMMStudyOfLanguage study = cmmStudyOfLanguageConverter.getReader().readValue(searchHit.sourceRef().array());
                    if (study.getStudyXmlSourceUrl() != null) {
                        String host = URI.create(study.getStudyXmlSourceUrl()).getHost();
                        map.replace(host, map.computeIfAbsent(host, k -> 0) + 1);
                    }
                } catch (IOException e) {
                    log.warn("Couldn't decode {} into an instance of {}", searchHit.getId(), CMMStudyOfLanguage.class.getName());
                }
            }
            if (response.getScrollId() != null) {
                response = esTemplate.getClient().prepareSearchScroll(response.getScrollId()).setScroll(timeout).get();
            } else {
                // The scroll id can be null if no results are returned, break
                break;
            }
            // Sometimes scrolling can cause a null response, end the loop if this is the case
        } while (response != null && response.getHits().getHits().length != 0);

        return Collections.unmodifiableMap(map);
    }

    private SearchRequestBuilder getMatchAllSearchRequest(String language) {
        return esTemplate.getClient().prepareSearch(String.format(INDEX_NAME_TEMPLATE, language))
                .setTypes(INDEX_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchAllQuery());
    }

    @Override
    public Optional<LocalDateTime> getMostRecentLastModified() {

        SearchResponse response = esTemplate.getClient().prepareSearch(String.format(INDEX_NAME_TEMPLATE, "*"))
                .setTypes(INDEX_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchAllQuery())
                .addSort(LAST_MODIFIED_FIELD, SortOrder.DESC)
                .setSize(1)
                .get();

        SearchHit[] hits = response.getHits().getHits();
        try {
            if (hits.length != 0) {
                CMMStudyOfLanguage study = cmmStudyOfLanguageConverter.getReader().readValue(hits[0].sourceRef().array());
                String lastModified = study.getLastModified();
                Optional<LocalDateTime> localDateTimeOpt = TimeUtility.getLocalDateTime(lastModified);
                return localDateTimeOpt
                        .map(localDateTime -> localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0));
            }
        } catch (IOException e) {
            log.warn("Couldn't decode {} into an instance of {}", hits[0].getId(), CMMStudyOfLanguage.class.getName());
        }
        return Optional.empty();
    }

    private IndexQuery getIndexQuery(String indexName, CMMStudyOfLanguage cmmStudyOfLanguage) {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(cmmStudyOfLanguage.getId());
        indexQuery.setObject(cmmStudyOfLanguage);
        indexQuery.setIndexName(indexName);
        indexQuery.setType(INDEX_TYPE);
        return indexQuery;
    }

    private boolean prepareIndex(String indexName) {

        if (esTemplate.indexExists(indexName)) {
            log.debug("[{}] index name already exists, Skipping creation.", indexName);
            return true;
        }

        log.info("[{}] index name does not exist and will be created", indexName);
        return createIndex(indexName);
    }

    private boolean createIndex(String indexName) {
        try {
            String settingsPath = String.format("elasticsearch/settings/settings_%s.json", indexName);
            String settingsTemplate = fileHandler.getFileAsString(settingsPath);
            String settings = String.format(settingsTemplate, esConfig.getNumberOfShards(), esConfig.getNumberOfReplicas());
            String mappingsPath = String.format("elasticsearch/mappings/mappings_%s.json", indexName);
            String mappings = fileHandler.getFileAsString(mappingsPath);

            if (settings.isEmpty() || mappings.isEmpty()) {
                log.warn("[{}] index creation Settings & Mappings must be define for a custom index.", indexName);
                log.warn("[{}] index creation with no custom settings or mappings.", indexName);
                return (esTemplate.createIndex(indexName));
            }

            log.debug("[{}] custom index creation with settings from [{}]. Content [\n{}\n]", indexName, settingsPath, settings);
            if (!esTemplate.createIndex(indexName, settings)) {
                log.error("[{}] custom index failed creation!", indexName);
                return false;
            } else {
                log.info("[{}] custom index created successfully.", indexName);
                log.debug("[{}] index mapping PUT request from [{}] with content [\n{}\n]", indexName, mappingsPath, mappings);
                if (esTemplate.putMapping(indexName, INDEX_TYPE, mappings)) {
                    log.info("[{}] index mapping PUT request for type [{}] was successful.", indexName, INDEX_TYPE);
                    return true;
                } else {
                    log.error("[{}] index mapping PUT request for type [{}] was unsuccessful.", indexName, INDEX_TYPE);
                    return false;
                }
            }
        } catch (IOException e) {
            log.warn("Couldn't load settings for Elasticsearch.", e);
            log.warn("[{}] index creation with no custom settings or mappings.", indexName);
            return (esTemplate.createIndex(indexName));
        }
  }

  private void executeBulk(List<IndexQuery> queries) {
    try {
      esTemplate.bulkIndex(queries);
    } catch (ElasticsearchException e) {
      log.error("BulkIndexing ElasticsearchException with message [{}]", e.getMessage(), e);
      Map<String, String> failedDocs = e.getFailedDocuments();

      if (!failedDocs.isEmpty()) {
        log.error("BulkIndexing failed to index all documents, see errors below alongside documents Ids");
        failedDocs.forEach((key, value) -> log.error("Failed to index Id [{}], message [{}]", key, value));
      }
    }
  }
}
