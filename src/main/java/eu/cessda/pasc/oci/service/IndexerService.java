package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
@Service
public class IndexerService {

  private static final String INDEX_NAME_TEMPLATE = "cmmstudy_%s";
  private static final String INDEX_TYPE = "cmmstudy";
  private static final int INDEX_COMMIT_SIZE = 500;
  private ElasticsearchTemplate elasticsearchTemplate;

  @Autowired
  public IndexerService( ElasticsearchTemplate elasticsearchTemplate) {
    this.elasticsearchTemplate = elasticsearchTemplate;
  }

  public void bulkIndex(List<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode) {
    String indexName = String.format(INDEX_NAME_TEMPLATE, languageIsoCode);
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
          elasticsearchTemplate.bulkIndex(queries);
          queries.clear();
          log.info("[{}] Indexing current bulkIndex counter [{}] ", indexName + counter);
        }
        counter++;
      }

      if (!queries.isEmpty()) {
        elasticsearchTemplate.bulkIndex(queries);
      }
      elasticsearchTemplate.refresh(indexName);
      log.info("[{}] BulkIndex completed");
    } catch (Exception e) {
      log.info("[{}] Encountered an exception [{}]", indexName, e.getMessage(), e);
    }
  }

  // For debug, print elastic search details
  String printElasticSearchInfo() {

    log.info("ElasticSearch Client Detail: Starts--");
    Client client = elasticsearchTemplate.getClient();
    Map<String, String> asMap = client.settings().getAsMap();

    asMap.forEach((k, v) -> log.info(k + " = " + v));
    log.info("ElasticSearch Client Details: End--");

    log.info("ElasticSearch Cluster Health Report: Start--");
    ClusterHealthResponse healths = client.admin().cluster().prepareHealth().get();
    client.admin().cluster().prepareHealth().get();
    log.info("Cluster Name [{}]", healths.getClusterName());
    log.info("NumberOfDataNodes [{}]", healths.getNumberOfDataNodes());
    log.info("NumberOfNodes [{}]", healths.getNumberOfNodes());


    log.info("ElasticSearch Cluster Nodes Report: Start--");
    log.info("NumberOfNodes [{}]", healths.getNumberOfNodes());
    for (ClusterIndexHealth health : healths.getIndices().values()) {
      log.info("Current Index [{}]", health.getIndex());
      log.info("NumberOfShards [{}]", health.getNumberOfShards());
      log.info("NumberOfReplicas [{}]", health.getNumberOfReplicas());
      log.info("Status [{}]", health.getStatus());
    }
    log.info("ElasticSearch Cluster Nodes Report: End--");
    log.info("ElasticSearch Cluster Health Report: End--");

    return "Printed Health";
  }
}
