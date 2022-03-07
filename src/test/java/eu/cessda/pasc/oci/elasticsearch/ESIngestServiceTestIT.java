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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.*;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.*;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
    private RestHighLevelClient elasticsearchClient;

    private final CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter = new CMMStudyOfLanguageConverter();

    /**
     * Reset Elasticsearch after each test
     */
    @After
    public void tearDown() throws IOException {
        elasticsearchClient.indices().delete(new DeleteIndexRequest("_all"), RequestOptions.DEFAULT);
    }

    @Test
    public void shouldSuccessfullyBulkIndexAllCMMStudies() throws IOException, JSONException {

        // Given
        final JsonNode expectedTree = mapper.readTree(getSyntheticCMMStudyOfLanguageEn());
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX1();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);

        // Set the id to a random UUID
        final String expected = studyOfLanguages.get(0).getId();

        // When
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        // Then
        then(isSuccessful).isTrue();
        elasticsearchClient.indices().refresh(Requests.refreshRequest(INDEX_NAME), RequestOptions.DEFAULT);
        SearchResponse response = elasticsearchClient.search(
            new SearchRequest(INDEX_NAME).types(INDEX_TYPE).source(new SearchSourceBuilder().query(QueryBuilders.idsQuery().addIds(expected))),
            RequestOptions.DEFAULT
        );

        // Should return the same ID
        then(response.getHits()).isNotEmpty();
        then(response.getHits().getAt(0).getId()).isEqualTo(expected);

        // And Assert full json equality
        final JsonNode actualTree = mapper.readTree(response.getHits().getAt(0).getSourceAsString());
        assertEquals(expectedTree.toString(), actualTree.toString(), true);
    }

    @Test
    public void shouldRetrieveTheMostRecentLastModifiedDate() throws IOException {

        // Given
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        then(isSuccessful).isTrue();
        elasticsearchClient.indices().refresh(Requests.refreshRequest(INDEX_NAME), RequestOptions.DEFAULT);
        SearchResponse response = elasticsearchClient.search(
            new SearchRequest(INDEX_NAME).types(INDEX_TYPE).source(
                new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).sort("lastModified", SortOrder.DESC)
            ),
            RequestOptions.DEFAULT
        );

        // And state is as expected
        then(response.getHits().getTotalHits()).isEqualTo(3);
        then(response.getHits().getAt(0).getId()).isEqualTo("UK-Data-Service__2305");
        then(response.getHits().getAt(1).getId()).isEqualTo("UK-Data-Service__999");
        then(response.getHits().getAt(2).getId()).isEqualTo("UK-Data-Service__1000");

        // When
        Optional<LocalDateTime> mostRecentLastModified = ingestService.getMostRecentLastModified();

        // Then
        then(mostRecentLastModified.orElse(null)).isEqualTo(LocalDateTime.parse("2017-11-17T00:00:00"));
    }

    @Test
    public void shouldReturnEmptyOptionalOnIOExceptions() throws IOException {

        // Given
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();

        var objectReader = Mockito.mock(ObjectReader.class);
        var cmmStudyOfLanguageConverterSpy = Mockito.spy(CMMStudyOfLanguageConverter.class);
        var ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverterSpy);

        // Given
        Mockito.when(cmmStudyOfLanguageConverterSpy.getReader()).thenReturn(objectReader);
        Mockito.when(objectReader.readValue(Mockito.any(InputStream.class))).thenThrow(IOException.class);

        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        then(isSuccessful).isTrue();
        elasticsearchClient.indices().refresh(Requests.refreshRequest(INDEX_NAME), RequestOptions.DEFAULT);
        elasticsearchClient.search(
            new SearchRequest(INDEX_NAME).types(INDEX_TYPE).source(
                new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).sort("lastModified", SortOrder.DESC)
            ),
            RequestOptions.DEFAULT
        );

        // When
        Optional<LocalDateTime> mostRecentLastModified = ingestService.getMostRecentLastModified();

        // Then
        then(mostRecentLastModified.isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnEmptyOptionalWithNoResults() {

        // Given
        var ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);

        // When
        Optional<LocalDateTime> mostRecentLastModified = ingestService.getMostRecentLastModified();

        // Then
        then(mostRecentLastModified.isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnAnIteratorOverAllStudiesInTheSpecifiedIndices() throws IOException {

        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);

        // Given
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();
        elasticsearchClient.indices().refresh(Requests.refreshRequest(INDEX_NAME), RequestOptions.DEFAULT);

        // Then
        var hitCountPerRepository = ingestService.getAllStudies("*");
        Assert.assertEquals(3, hitCountPerRepository.size());
        Assert.assertTrue(hitCountPerRepository.containsAll(studyOfLanguages));
    }

    @Test(expected = UncheckedIOException.class)
    public void shouldThrowUncheckedIOExceptionIfAnIOErrorOccursWhenIterating() throws IOException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ObjectReader objectReader = Mockito.mock(ObjectReader.class);
        var cmmStudyOfLanguageConverterSpy = Mockito.spy(CMMStudyOfLanguageConverter.class);
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverterSpy);

        // Given
        Mockito.when(cmmStudyOfLanguageConverterSpy.getReader()).thenReturn(objectReader);
        Mockito.when(objectReader.readValue(Mockito.any(InputStream.class))).thenThrow(IOException.class);
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();
        elasticsearchClient.indices().refresh(Requests.refreshRequest(INDEX_NAME), RequestOptions.DEFAULT);

        // Then
        var hitCountPerRepository = ingestService.getAllStudies("*");
        hitCountPerRepository.iterator().next(); // Should throw
    }

    @Test
    public void shouldReturnNoStudiesForAnEmptyIndex() {

        // Setup
        var ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);

        // Then
        var hitCountPerRepository = ingestService.getAllStudies("*");
        assertEquals(0, hitCountPerRepository.size());
        assertFalse(hitCountPerRepository.iterator().hasNext());
    }

    @Test
    public void shouldGetStudy() throws IOException {

        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);

        // Given
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();

        // Then - check if all studies are present
        for (var expectedStudy : studyOfLanguages) {
            var study = ingestService.getStudy(expectedStudy.getId(), LANGUAGE_ISO_CODE);
            Assert.assertEquals(expectedStudy, study.orElseThrow());
        }
    }

    @Test
    public void shouldReturnEmptyOptionalOnInvalidIndex() {

        // Setup
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);

        // Then
        var study = ingestService.getStudy(UUID.randomUUID().toString(), "moon");

        Assert.assertEquals(Optional.empty(), study);
    }

    @Test
    public void shouldReturnEmptyOptionalIfStudyCannotBeFound() throws IOException {

        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);

        // Given
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();

        // Then
        var study = ingestService.getStudy(UUID.randomUUID().toString(), LANGUAGE_ISO_CODE);
        assertTrue(study.isEmpty());
    }

    @Test
    public void shouldReturnEmptyOptionalOnIOException() throws IOException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ObjectReader objectReader = Mockito.mock(ObjectReader.class);
        var cmmStudyOfLanguageConverterSpy = Mockito.spy(CMMStudyOfLanguageConverter.class);
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverterSpy);

        // Given
        Mockito.when(cmmStudyOfLanguageConverterSpy.getReader()).thenReturn(objectReader);
        Mockito.when(objectReader.readValue(Mockito.any(byte[].class))).thenThrow(IOException.class);
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();
        var expectedStudy = studyOfLanguages.get(0);

        // Then
        var study = ingestService.getStudy(expectedStudy.getId(), LANGUAGE_ISO_CODE);

        Assert.assertEquals(Optional.empty(), study);
    }

    @Test
    public void shouldLogFailedIndexOperations() throws IOException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        var cmmStudyOfLanguageConverterSpy = Mockito.spy(this.cmmStudyOfLanguageConverter);
        var objectWriterSpy = Mockito.spy(this.cmmStudyOfLanguageConverter.getWriter());
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverterSpy);

        // Given
        Mockito.doReturn(objectWriterSpy).when(cmmStudyOfLanguageConverterSpy).getWriter();
        Mockito.doThrow(JsonProcessingException.class).when(objectWriterSpy).writeValueAsBytes(Mockito.any(CMMStudyOfLanguage.class));

        // Then - indexing should report true as Elasticsearch was accessible
        boolean indexingResult = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        Assert.assertTrue(indexingResult);
    }

    @Test
    public void shouldDeleteGivenStudies() throws IOException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp, cmmStudyOfLanguageConverter);
        boolean isSuccessful = ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        then(isSuccessful).isTrue();
        elasticsearchClient.indices().refresh(Requests.refreshRequest(INDEX_NAME), RequestOptions.DEFAULT);

        // Given
        var studyToDelete = Collections.singletonList(studyOfLanguages.get(0));
        ingestService.bulkDelete(studyToDelete, LANGUAGE_ISO_CODE);

        // Then - the study should not be present, but other studies should be
        elasticsearchClient.indices().refresh(Requests.refreshRequest(INDEX_NAME), RequestOptions.DEFAULT);
        assertFalse(elasticsearchClient.get(
            new GetRequest(INDEX_NAME, INDEX_TYPE, studyToDelete.get(0).getId()), RequestOptions.DEFAULT
        ).isExists());

        SearchResponse response = elasticsearchClient.search(
            new SearchRequest(INDEX_NAME).types(INDEX_TYPE).source(
                new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).sort("lastModified", SortOrder.DESC)
            ),
            RequestOptions.DEFAULT
        );

        then(response.getHits().getTotalHits()).isEqualTo(2); // Should be two studies
        then(Arrays.stream(response.getHits().getHits()).map(SearchHit::getId).toArray()) // Should not contain the deleted study
            .containsExactlyInAnyOrder(studyOfLanguages.get(1).getId(), studyOfLanguages.get(2).getId());
    }
}