/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.elasticsearch.ESIngestService;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.*;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;


/**
 * Indexer Service test with embedded Elasticsearch
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class ESIngestServiceTestIT {

    private static final String INDEX_TYPE = "cmmstudy";
    public static final String LANGUAGE_ISO_CODE = "en";
    private static final String INDEX_NAME = INDEX_TYPE + "_" + LANGUAGE_ISO_CODE;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ESConfigurationProperties esConfigProp;

    @Autowired
    private FileHandler fileHandler;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /**
     * Reset Elasticsearch after each test
     */
    @After
    public void tearDown() {
        elasticsearchTemplate.getClient().admin().indices().prepareDelete("_all").get();
    }

    @Test
    public void shouldSuccessfullyBulkIndexAllCMMStudies() throws IOException, JSONException {

        // Given
        final JsonNode expectedTree = mapper.readTree(getSyntheticCMMStudyOfLanguageEn());
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX1();
        ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(this.elasticsearchTemplate.getClient());
        ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, null);

        // Set the id to a random UUID
        final String expected = studyOfLanguages.get(0).getId();

        // When
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        // Then
        then(isSuccessful).isTrue();
        Client client = this.elasticsearchTemplate.getClient();
        SearchResponse response = client.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.idsQuery().addIds(expected))
                .execute()
                .actionGet();

        // Should return the same ID
        then(response.getHits().getAt(0).getId()).isEqualTo(expected);

        // And Assert full json equality
        final JsonNode actualTree = mapper.readTree(response.getHits().getAt(0).getSourceAsString());
        assertEquals(expectedTree.toString(), actualTree.toString(), true);
    }

  @Test
  public void shouldRetrieveTheMostRecentLastModifiedDate() throws IOException {

      // Given
      List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
      ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(this.elasticsearchTemplate.getClient());
      CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter = new CMMStudyOfLanguageConverter(new ObjectMapper());
      ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, cmmStudyOfLanguageConverter);
      boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
      then(isSuccessful).isTrue();
      Client client = this.elasticsearchTemplate.getClient();
      SearchResponse response = client.prepareSearch(INDEX_NAME)
              .setTypes(INDEX_TYPE)
              .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
              .setQuery(QueryBuilders.matchAllQuery())
              .addSort("lastModified", SortOrder.DESC)
              .execute()
              .actionGet();

      // And state is as expected
      then(response.getHits().getTotalHits()).isEqualTo(3);
      then(response.getHits().getAt(0).getId()).isEqualTo("UK-Data-Service__2305");
      then(response.getHits().getAt(1).getId()).isEqualTo("UK-Data-Service__999");
      then(response.getHits().getAt(2).getId()).isEqualTo("UK-Data-Service__1000");

      // When
      Optional<LocalDateTime> mostRecentLastModified = ingestService.getMostRecentLastModified();

      // Then
      then(mostRecentLastModified.orElse(null)).isEqualByComparingTo(LocalDateTime.parse("2017-11-17T00:00:00"));
  }

  @Test
  public void shouldReturnFalseOnIndexCreationFailure() throws IOException {

    //Setup
    ElasticsearchTemplate elasticsearchTemplate = Mockito.mock(ElasticsearchTemplate.class);
    ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, null);
    List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();

    // When
    Mockito.when(elasticsearchTemplate.createIndex(Mockito.anyString(), Mockito.any())).thenReturn(false);

    boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
    Assert.assertFalse(isSuccessful);
  }

  @Test
  public void shouldReturnFalseOnPutMappingFailure() throws IOException {

    //Setup
    ElasticsearchTemplate elasticsearchTemplate = Mockito.mock(ElasticsearchTemplate.class);
    ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, null);
    List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();

    // When
    Mockito.when(elasticsearchTemplate.createIndex(Mockito.anyString(), Mockito.any())).thenReturn(true);
    Mockito.when(elasticsearchTemplate.putMapping(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(false);

    Assert.assertFalse(ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE));
  }

  @Test
  public void shouldCreateAnIndexWithNoSettingsWhenIOExceptionIsThrown() throws IOException {

    //Setup
    FileHandler fileHandlerMock = Mockito.mock(FileHandler.class);
    ElasticsearchTemplate elasticsearchTemplate = Mockito.mock(ElasticsearchTemplate.class);
    ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandlerMock, esConfigProp, null);
      List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();

      // When
      Mockito.when(fileHandlerMock.getFileAsString(Mockito.anyString())).thenThrow(new IOException());
      Mockito.when(elasticsearchTemplate.createIndex(Mockito.anyString())).thenReturn(true);

      Assert.assertTrue(ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE));
      Mockito.verify(elasticsearchTemplate).createIndex(Mockito.anyString());
  }

    @Test
    public void shouldReturnAnIteratorOverAllStudiesInTheSpecifiedIndices() throws IOException {

        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(this.elasticsearchTemplate.getClient());
        CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter = new CMMStudyOfLanguageConverter(new ObjectMapper());
        ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, cmmStudyOfLanguageConverter);

        // Given
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();

        // Then
        var hitCountPerRepository = ingestService.getAllStudies("*");
        Assert.assertEquals(3, hitCountPerRepository.size());
        Assert.assertTrue(hitCountPerRepository.containsAll(studyOfLanguages));
    }

    @Test(expected = UncheckedIOException.class)
    public void shouldThrowUncheckedIOExceptionIfAnIOErrorOccursWhenIterating() throws IOException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(this.elasticsearchTemplate.getClient());
        ObjectReader objectReader = Mockito.mock(ObjectReader.class);
        CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter = Mockito.mock(CMMStudyOfLanguageConverter.class);
        ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, cmmStudyOfLanguageConverter);

        // Given
        Mockito.when(cmmStudyOfLanguageConverter.getReader()).thenReturn(objectReader);
        Mockito.when(objectReader.readValue(Mockito.any(InputStream.class))).thenThrow(new IOException());
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();

        // Then
        var hitCountPerRepository = ingestService.getAllStudies("*");
        hitCountPerRepository.iterator().next(); // Should throw
    }

    @Test
    public void shouldGetStudy() throws IOException {

        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(this.elasticsearchTemplate.getClient());
        CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter = new CMMStudyOfLanguageConverter(new ObjectMapper());
        ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, cmmStudyOfLanguageConverter);

        // Given
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();
        var expectedStudy = studyOfLanguages.get(0);

        // Then
        var study = ingestService.getStudy(expectedStudy.getId(), "en");

        Assert.assertEquals(expectedStudy, study.get());
  }

  @Test
  public void shouldReturnEmptyOptionalOnInvalidIndex() {

    // Setup
      ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(this.elasticsearchTemplate.getClient());
      CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter = new CMMStudyOfLanguageConverter(new ObjectMapper());
    ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, cmmStudyOfLanguageConverter);

    // Then
    var study = ingestService.getStudy(UUID.randomUUID().toString(), "moon");

    Assert.assertEquals(Optional.empty(), study);
  }

  @Test
  public void shouldReturnEmptyOptionalOnIOException() throws IOException {
      // Setup
      List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
      ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(this.elasticsearchTemplate.getClient());
      ObjectReader objectReader = Mockito.mock(ObjectReader.class);
      CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter = Mockito.mock(CMMStudyOfLanguageConverter.class);
      ESIngestService ingestService = new ESIngestService(elasticsearchTemplate, fileHandler, esConfigProp, cmmStudyOfLanguageConverter);

      // Given
      Mockito.when(cmmStudyOfLanguageConverter.getReader()).thenReturn(objectReader);
      Mockito.when(objectReader.readValue(Mockito.any(byte[].class))).thenThrow(new IOException());
      boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
      then(isSuccessful).isTrue();
      var expectedStudy = studyOfLanguages.get(0);

      // Then
      var study = ingestService.getStudy(expectedStudy.getId(), "en");

      Assert.assertEquals(Optional.empty(), study);
  }
}