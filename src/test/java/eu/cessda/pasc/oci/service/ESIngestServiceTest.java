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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.service.helpers.EmbeddedElasticsearchServer;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Java6BDDAssertions;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static eu.cessda.pasc.oci.data.RecordTestData.*;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;


/**
 * Indexer Service test with embedded Elasticsearch
 *
 * @author moses AT doravenetures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class ESIngestServiceTest extends EmbeddedElasticsearchServer{

  private IngestService eSIndexerService; // Class under test
  private static final String INDEX_NAME = "cmmstudy_en";
  private static final String INDEX_TYPE = "cmmstudy";

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private ESConfigurationProperties esConfigProp;

  @Autowired
  private FileHandler fileHandler;

  @Before
  public void init() {
    startup(ELASTICSEARCH_HOME);
    log.info("Embedded Server initiated");
    ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(getClient());
    eSIndexerService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp);
  }

  @After
  public void shutdown() {
    log.info("---------Closing node---------");
    closeNodeResources();
    Java6BDDAssertions.then(this.node.isClosed()).isTrue();
  }

  @Test
  public void shouldSuccessfullyBulkIndexAllCMMStudies() throws IOException, JSONException {

    // Given
    final JsonNode expectedTree = mapper.readTree(getSyntheticCMMStudyOfLanguageEn());
    String language = "en";
    List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX1();

    // When
    boolean isSuccessful = this.eSIndexerService.bulkIndex(studyOfLanguages, language);

    // Then
    then(isSuccessful).isTrue();
    SearchResponse response = getClient().prepareSearch(INDEX_NAME)
        .setTypes(INDEX_TYPE)
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(QueryBuilders.matchAllQuery())
        .execute()
        .actionGet();
    then(response.getHits().totalHits()).isEqualTo(1);
    then(response.getHits().getAt(0).getId()).isEqualTo("UK-Data-Service__2305");

    // And Assert full json equality
    final JsonNode actualTree = mapper.readTree(response.getHits().getAt(0).getSourceAsString());
    assertEquals(expectedTree.toString(), actualTree.toString(), true);
  }

  @Test
  public void shouldRetrieveTheMostRecentLastModifiedDate() throws IOException {

    // Given
    String language = "en";
    List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
    boolean isSuccessful = this.eSIndexerService.bulkIndex(studyOfLanguages, language);
    then(isSuccessful).isTrue();
    SearchResponse response = getClient().prepareSearch(INDEX_NAME)
        .setTypes(INDEX_TYPE)
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(QueryBuilders.matchAllQuery())
        .addSort("lastModified", SortOrder.DESC)
        .execute()
        .actionGet();

    // And state is as expected
    then(response.getHits().totalHits()).isEqualTo(3);
    then(response.getHits().getAt(0).getId()).isEqualTo("UK-Data-Service__2305");
    then(response.getHits().getAt(1).getId()).isEqualTo("UK-Data-Service__999");
    then(response.getHits().getAt(2).getId()).isEqualTo("UK-Data-Service__1000");

    // When
    Optional<LocalDateTime> mostRecentLastModified = this.eSIndexerService.getMostRecentLastModified();

    // Then
    then(mostRecentLastModified.orElse(null)).isEqualByComparingTo(LocalDateTime.parse("2017-11-17T00:00:00"));
  }
}