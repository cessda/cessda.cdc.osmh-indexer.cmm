package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for triggering harvesting and Metadata ingestion to the search engine
 *
 * @author moses@doraventures.com
 */
@Service
@Slf4j
public class ESIndexerService {

  private static final String INDEX_NAME_TEMPLATE = "cmmstudy_%s";
  private static final String INDEX_TYPE = "cmmstudy";
  private static final int INDEX_COMMIT_SIZE = 500;
  private ElasticsearchTemplate elasticsearchTemplate;

  @Autowired
  public ESIndexerService(ElasticsearchTemplate elasticsearchTemplate) {
    this.elasticsearchTemplate = elasticsearchTemplate;
  }

  public boolean bulkIndex(List<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode) {
    String indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);
    boolean isSuccessful = true;
    int counter = 0;
    try {

      if (!elasticsearchTemplate.indexExists(indexName)) {
        elasticsearchTemplate.createIndex(indexName);
      }

      List<IndexQuery> queries = new ArrayList<>();
      for (CMMStudyOfLanguage cmmStudyOfLanguage : languageCMMStudiesMap) {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(cmmStudyOfLanguage.getId());
        indexQuery.setObject(cmmStudyOfLanguage);
        indexQuery.setIndexName(indexName);
        indexQuery.setType(INDEX_TYPE);
        queries.add(indexQuery);
        if (counter % INDEX_COMMIT_SIZE == 0) {
          executeBulk(queries);
          queries.clear();
          log.info("[{}] Indexing current bulkIndex counter [{}] ", indexName + counter);
        }
        counter++;
      }

      if (!queries.isEmpty()) {
        executeBulk(queries);
      }
      elasticsearchTemplate.refresh(indexName);
      log.info("[{}] BulkIndex completed");
    } catch (Exception e) {
      log.error("[{}] Encountered an exception [{}]", indexName, e.getMessage(), e);
      isSuccessful = false;
    }
    return isSuccessful;
  }

  private void executeBulk(List<IndexQuery> queries) {
    try {
      elasticsearchTemplate.bulkIndex(queries);
    } catch (ElasticsearchException e) {
      log.error("BulkIndexing ElasticsearchException with message [{}]", e.getMessage());
      if (log.isInfoEnabled()) {
        log.error("BulkIndexing ElasticsearchException: Printing failed documents' Id and Message", e.getMessage());
        Map<String, String> failedDocs = e.getFailedDocuments();
        failedDocs.forEach((key, value) -> log.info("Id [{}], message [{}]", key, value));
      }
    }
  }
}
