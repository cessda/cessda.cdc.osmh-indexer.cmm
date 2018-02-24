package eu.cessda.pasc.oci.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.service.helpers.EmbeddedElasticsearchServer;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Java6BDDAssertions;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static eu.cessda.pasc.oci.data.RecordTestData.getCmmStudyOfLanguageCodeEn;
import static eu.cessda.pasc.oci.data.RecordTestData.getSyntheticCMMStudyInEn;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;


/**
 * Indexer Service test with embedded Elasticsearch
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class IndexerServiceTest extends EmbeddedElasticsearchServer {

  private static final String INDEX_NAME = "cmmstudy_en";
  private static final String INDEX_TYPE = "cmmstudy";
  private IndexerService indexerService;

  @Autowired
  private ObjectMapper mapper;


  @Before
  public void init() {
    startup(ELASTICSEARCH_HOME);
    log.info("Embedded Server initiated");
    ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(getClient());
    indexerService = new IndexerService(elasticsearchTemplate);
  }

//  @After
  public void shutdown() {
    log.info("---------Closing node---------");
    closeNodeResources();
    Java6BDDAssertions.then(this.node.isClosed()).isTrue();
  }

  @Test
  public void shouldSuccessfullyBulkIndexAllCMMStudies() throws IOException, JSONException {

    // Given
    String language = "en";
    List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEn();
    log.info("---------Printing health next---------");
    then(indexerService.printElasticSearchInfo()).isEqualTo("Printed Health");

    // When
    this.indexerService.bulkIndex(studyOfLanguages, language);


    // Then
    SearchResponse response = getClient().prepareSearch(INDEX_NAME)
        .setTypes(INDEX_TYPE).setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(QueryBuilders.matchAllQuery())
        .execute().actionGet();
    then(response.getHits().totalHits()).isEqualTo(1);
    then(response.getHits().getAt(0).getId()).isEqualTo("UK-Data-Service__2305");

    // And Assert full json equality
    final JsonNode actualTree = mapper.readTree(response.getHits().getAt(0).getSourceAsString());
    final JsonNode expectedTree = mapper.readTree(getSyntheticCMMStudyInEn());
    assertEquals(expectedTree.toString(), actualTree.toString(), true);

    log.info("Printing hits");
    Arrays.asList(response.getHits().getHits()).forEach(hit-> log.info(hit.getSourceAsString()));
  }

}