/**
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.helpers.TimeUtility;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static eu.cessda.pasc.oci.helpers.AppConstants.LAST_MODIFIED_FIELD;

/**
 * Service responsible for triggering harvesting and Metadata ingestion to the search engine
 *
 * @author moses@doraventures.com
 */
@Service
@Slf4j
public class ESIngestService implements IngestService {

  private static final String INDEX_NAME_TEMPLATE = "cmmstudy_%s";
  private static final String INDEX_NAME_PATTERN = "cmmstudy_*";
  private static final String INDEX_TYPE = "cmmstudy";
  private static final int INDEX_COMMIT_SIZE = 500;
  private ElasticsearchTemplate esTemplate;
  private FileHandler fileHandler;
  private ESConfigurationProperties esConfig;

  @Autowired
  public ESIngestService(
      ElasticsearchTemplate esTemplate, FileHandler fileHandler, ESConfigurationProperties esConfig) {

    this.esTemplate = esTemplate;
    this.fileHandler = fileHandler;
    this.esConfig = esConfig;
  }

  public boolean bulkIndex(List<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode) {
    String indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);
    boolean isSuccessful = true;
    int counter = 0;

    if (prepareIndex(indexName, esTemplate, fileHandler)) {
      try {
        List<IndexQuery> queries = new ArrayList<>();
        for (CMMStudyOfLanguage cmmStudyOfLanguage : languageCMMStudiesMap) {
          IndexQuery indexQuery = getIndexQuery(indexName, cmmStudyOfLanguage);
          queries.add(indexQuery);
          if (counter % INDEX_COMMIT_SIZE == 0) {
            executeBulk(queries);
            queries.clear();
            log.info("Indexing [{}] index, current bulkIndex counter [{}] .", indexName, counter);
          }
          counter++;
        }

        if (!queries.isEmpty()) {
          executeBulk(queries);
        }
        esTemplate.refresh(indexName);
        log.info("[{}] BulkIndex completed.", languageIsoCode);
      } catch (Exception e) {
        log.error("[{}] Encountered an exception [{}].", indexName, e.getMessage());
        isSuccessful = false;
      }
    } else {
      isSuccessful = false;
    }
    return isSuccessful;
  }

  @Override
  public Optional<LocalDateTime> getMostRecentLastModified() {

    SearchResponse response = esTemplate.getClient().prepareSearch(INDEX_NAME_PATTERN)
        .setTypes(INDEX_TYPE)
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(QueryBuilders.matchAllQuery())
        .addSort(LAST_MODIFIED_FIELD, SortOrder.DESC)
        .setSize(1)
        .execute()
        .actionGet();

    SearchHit[] hits = response.getHits().getHits();
    if (hits.length != 0) {
      Map<String, Object> source = hits[0].getSource();
      String lastModified = source.get(LAST_MODIFIED_FIELD).toString();
      Optional<LocalDateTime> localDateTimeOpt = TimeUtility.getLocalDateTime(lastModified);
      return localDateTimeOpt
          .map(localDateTime -> localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0));
    } else {
      return Optional.empty();
    }
  }

  private static IndexQuery getIndexQuery(String indexName, CMMStudyOfLanguage cmmStudyOfLanguage) {
    IndexQuery indexQuery = new IndexQuery();
    indexQuery.setId(cmmStudyOfLanguage.getId());
    indexQuery.setObject(cmmStudyOfLanguage);
    indexQuery.setIndexName(indexName);
    indexQuery.setType(INDEX_TYPE);
    return indexQuery;
  }

  private boolean prepareIndex(String indexName, ElasticsearchTemplate elasticsearchTemplate, FileHandler fileHandler) {

    if (elasticsearchTemplate.indexExists(indexName)) {
      log.info("Index [{}] Already Exist, Skipping creation.", indexName);
      return true;
    }

    log.info("[{}] index does not exist.", indexName);
    return createIndex(indexName, elasticsearchTemplate, fileHandler);
  }

  private boolean createIndex(String indexName, ElasticsearchTemplate elasticsearchTemplate, FileHandler fileHandler) {
    String settingsPath = String.format("elasticsearch/settings/settings_%s.json", indexName);
    String settingsTemplate = fileHandler.getFileWithUtil(settingsPath);
    String settings = String.format(settingsTemplate, esConfig.getNumberOfShards(), esConfig.getNumberOfReplicas());
    String mappingsPath = String.format("elasticsearch/mappings/mappings_%s.json", indexName);
    String mappings = fileHandler.getFileWithUtil(mappingsPath);

    if (settings.isEmpty() || mappings.isEmpty()) {
      log.warn("Settings & Mappings must be define for a custom [{}] index creation.", indexName);
      log.warn("Creating [{}] index with no custom settings or mappings.", indexName);
      return (elasticsearchTemplate.createIndex(indexName));
    }

    log.debug("Creating custom [{}] index with settings from [{}]. Content [\n{}\n]", indexName, settingsPath, settings);
    try {
      if (!elasticsearchTemplate.createIndex(indexName, settings)) {
        log.error("Failed custom [{}] index creation!", indexName);
        return false;
      } else {
        log.info("Create custom [{}] index successfully!", indexName);
        log.debug("Putting [{}] index mapping from [{}] with content [\n{}\n]", indexName, mappingsPath, mappings);
        if (elasticsearchTemplate.putMapping(indexName, INDEX_TYPE, mappings)) {
          log.info("Put [{}] index mapping for type [{}] was successful.", indexName, INDEX_TYPE);
          return true;
        } else {
          log.error("Put [{}] index mapping for type [{}] was unsuccessful.", indexName, INDEX_TYPE);
          return false;
        }
      }
    } catch (Exception e) {
      log.error("Custom [{}] Index creation failed. Message [{}]", indexName, e.getMessage(), e);
      return false;
    }
  }

  private void executeBulk(List<IndexQuery> queries) {
    try {
      esTemplate.bulkIndex(queries);
    } catch (ElasticsearchException e) {
      log.error("BulkIndexing ElasticsearchException with message [{}]", e.getMessage());
      log.error("BulkIndexing ElasticsearchException: Printing failed documents' Id and Message");
      Map<String, String> failedDocs = e.getFailedDocuments();

      if (!failedDocs.isEmpty()) {
        log.error("BulkIndexing failed to index all documents, see errors below alongside documents Ids");
        failedDocs.forEach((key, value) -> log.error("Failed to index Id [{}], message [{}]", key, value));
      }
    }
  }
}
