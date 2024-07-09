/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import eu.cessda.pasc.oci.configurations.ElasticsearchConfiguration;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.*;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.getCmmStudyOfLanguageCodeEnX1;
import static eu.cessda.pasc.oci.mock.data.RecordTestData.getCmmStudyOfLanguageCodeEnX3;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


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
    private ESConfigurationProperties esConfigProp;

    @Autowired
    private ElasticsearchConfiguration elasticsearchConfiguration;

    private ElasticsearchClient elasticsearchClient;

    @BeforeAll
    public void setUp() {
        elasticsearchClient = elasticsearchConfiguration.elasticsearchClient();
    }

    /**
     * Reset Elasticsearch after each test
     */
    @After
    public void tearDown() throws IOException {
        try {
            elasticsearchClient.indices().delete(DeleteIndexRequest.of(d -> d.index(INDEX_NAME)));
        } catch (ElasticsearchException e) {
            // ignore all Elasticsearch Exceptions
        }
    }

    @Test
    public void shouldSuccessfullyBulkIndexAllCMMStudies() throws IOException, IndexingException {

        // Given
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX1();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp);

        // Set the id to a random UUID
        var expected = studyOfLanguages.getFirst();

        // When
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        // Then
        elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(INDEX_NAME)));
        var response = elasticsearchClient.search(
            SearchRequest.of(s -> s.index(INDEX_NAME).query(IdsQuery.of(i -> i.values(expected.id()))._toQuery())),
            CMMStudyOfLanguage.class
        );

        // Should return the same document
        then(response.hits().hits()).isNotEmpty();

        // Verify that the document is considered equal
        var actual = response.hits().hits().getFirst().source();
        then(actual).isEqualTo(expected);
    }

    @Test
    public void shouldHandleErrorsWhenIndexing() throws IOException, IndexingException {
        ElasticsearchClient mockClient = spy(elasticsearchClient);

        // Given
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX1();
        ESIngestService ingestService = new ESIngestService(mockClient, esConfigProp);


        var bulkItem = new BulkResponseItem.Builder()
            .index(INDEX_NAME)
            .error(new ErrorCause.Builder().type("mocked_error").build())
            .operationType(OperationType.Index)
            .status(400)
            .build();

        var mappingError = new BulkResponseItem.Builder()
            .index(INDEX_NAME)
            .error(new ErrorCause.Builder().type("strict_dynamic_mapping_exception").build())
            .operationType(OperationType.Index)
            .status(400)
            .build();

        // Create the mock response
        var mockResponse = new BulkResponse.Builder()
            .errors(true)
            .items(List.of(bulkItem, mappingError))
            .took(0)
            .build();

        doReturn(mockResponse).when(mockClient).bulk(any(BulkRequest.class));

        // When
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        // Then
        then(ingestService.getTotalHitCount("*")).isZero();
    }

    @Test
    public void shouldRetrieveTheMostRecentLastModifiedDate() throws IOException, IndexingException {

        // Given
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp);
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(INDEX_NAME)));
        var response = elasticsearchClient.search(
            SearchRequest.of(s -> s.index(INDEX_NAME)
                .query(MatchAllQuery.of(m -> m)._toQuery())
                .sort(SortOptions.of(so -> so.field(
                    FieldSort.of(f -> f.field("lastModified").order(SortOrder.Desc))
                )))),
            CMMStudyOfLanguage.class
        );

        // And state is as expected
        then(response.hits().total()).isNotNull();
        then(response.hits().total().value()).isEqualTo(3);
        then(response.hits().hits().get(0).id()).isEqualTo("UK-Data-Service__2305");
        then(response.hits().hits().get(1).id()).isEqualTo("UK-Data-Service__999");
        then(response.hits().hits().get(2).id()).isEqualTo("UK-Data-Service__1000");

        // When
        Optional<LocalDateTime> mostRecentLastModified = ingestService.getMostRecentLastModified();

        // Then
        then(mostRecentLastModified.orElse(null)).isEqualTo(LocalDateTime.parse("2017-11-17T00:00:00"));
    }

    @Test
    public void shouldReturnEmptyOptionalOnIOExceptions() throws IOException, IndexingException {

        // Given
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();

        var elasticsearchClientSpy = spy(elasticsearchClient);
        var ingestService = new ESIngestService(elasticsearchClientSpy, esConfigProp);

        // Given
        doThrow(IOException.class).when(elasticsearchClientSpy).search(any(SearchRequest.class), eq(CMMStudyOfLanguage.class));

        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(INDEX_NAME)));

        // When
        Optional<LocalDateTime> mostRecentLastModified = ingestService.getMostRecentLastModified();

        // Then
        then(mostRecentLastModified.isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnEmptyOptionalWithNoResults() {

        // Given
        var ingestService = new ESIngestService(elasticsearchClient, esConfigProp);

        // When
        Optional<LocalDateTime> mostRecentLastModified = ingestService.getMostRecentLastModified();

        // Then
        then(mostRecentLastModified.isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnAnIteratorOverAllStudiesInTheSpecifiedIndices() throws IOException, IndexingException {

        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp);

        // Given
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(INDEX_NAME)));

        // Then
        var hitCountPerRepository = ingestService.getAllStudies("*");
        Assert.assertEquals(3, hitCountPerRepository.size());
        Assert.assertTrue(hitCountPerRepository.containsAll(studyOfLanguages));
    }

    @Test(expected = UncheckedIOException.class)
    @SuppressWarnings("ReturnValueIgnored")
    public void shouldThrowUncheckedIOExceptionIfAnIOErrorOccursWhenIterating() throws IOException, IndexingException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        var elasticsearchClientSpy = spy(elasticsearchClient);
        ESIngestService ingestService = new ESIngestService(elasticsearchClientSpy, esConfigProp);

        // Given
        doThrow(IOException.class).when(elasticsearchClientSpy)
            .search(any(SearchRequest.class), eq(CMMStudyOfLanguage.class));
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(INDEX_NAME)));

        // Then
        var hitCountPerRepository = ingestService.getAllStudies("*");
        hitCountPerRepository.iterator(); // Should throw
    }

    @Test
    public void shouldReturnNoStudiesForAnEmptyIndex() throws IndexingException {

        // Setup
        var ingestService = new ESIngestService(elasticsearchClient, esConfigProp);
        ingestService.bulkIndex(Collections.emptyList(), LANGUAGE_ISO_CODE);

        // Then
        var hitCountPerRepository = ingestService.getAllStudies("*");
        assertEquals(0, hitCountPerRepository.size());
        assertFalse(hitCountPerRepository.iterator().hasNext());
    }

    @Test
    public void shouldGetStudy() throws IOException, IndexingException {

        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp);

        // Given
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        // Then - check if all studies are present
        for (var expectedStudy : studyOfLanguages) {
            var study = ingestService.getStudy(expectedStudy.id(), LANGUAGE_ISO_CODE);
            assertEquals(expectedStudy, study.orElseThrow());
        }
    }

    @Test
    public void shouldReturnEmptyOptionalOnInvalidIndex() {

        // Setup
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp);

        // Then
        var study = ingestService.getStudy(UUID.randomUUID().toString(), "moon");

        Assert.assertEquals(Optional.empty(), study);
    }

    @Test
    public void shouldReturnEmptyOptionalIfStudyCannotBeFound() throws IOException, IndexingException {

        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp);

        // Given
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);

        // Then
        var study = ingestService.getStudy(UUID.randomUUID().toString(), LANGUAGE_ISO_CODE);
        assertTrue(study.isEmpty());
    }

    @Test
    public void shouldReturnEmptyOptionalOnIOException() throws IOException, IndexingException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        var elasticsearchClientSpy = spy(elasticsearchClient);
        ESIngestService ingestService = new ESIngestService(elasticsearchClientSpy, esConfigProp);

        // Given
        doThrow(IOException.class).when(elasticsearchClientSpy).get(any(co.elastic.clients.elasticsearch.core.GetRequest.class), eq(CMMStudyOfLanguage.class));
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        var expectedStudy = studyOfLanguages.getFirst();

        // Then
        var study = ingestService.getStudy(expectedStudy.id(), LANGUAGE_ISO_CODE);

        Assert.assertEquals(Optional.empty(), study);
    }

    @Test
    public void shouldDeleteGivenStudies() throws IOException, IndexingException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();
        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp);
        ingestService.bulkIndex(studyOfLanguages, LANGUAGE_ISO_CODE);
        elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(INDEX_NAME)));

        // Given
        var studyToDelete = Collections.singletonList(studyOfLanguages.getFirst());
        ingestService.bulkDelete(studyToDelete, LANGUAGE_ISO_CODE);

        // Then - the study should not be present, but other studies should be
        elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(INDEX_NAME)));
        assertFalse(elasticsearchClient.get(
            GetRequest.of(g -> g.index(INDEX_NAME).id(studyToDelete.getFirst().id())), Void.class
        ).found());

        var response = elasticsearchClient.search(
            SearchRequest.of(s -> s.index(INDEX_NAME)
                .query(MatchAllQuery.of(m -> m)._toQuery())
                .sort(SortOptions.of(so -> so.field(
                    FieldSort.of(f -> f.field("lastModified").order(SortOrder.Desc))
                )))),
            CMMStudyOfLanguage.class
        );

        then(response.hits().total()).isNotNull();
        then(response.hits().total().value()).isEqualTo(2); // Should be two studies
        then(response.hits().hits().stream().map(Hit::id)) // Should not contain the deleted study
            .containsExactlyInAnyOrder(studyOfLanguages.get(1).id(), studyOfLanguages.get(2).id());
    }

    @Test
    @SuppressWarnings("java:S2699") // false positive
    public void shouldReturnAllStudiesBelongingToARepository() throws IOException, IndexingException {
        // Setup
        List<CMMStudyOfLanguage> studyOfLanguages = getCmmStudyOfLanguageCodeEnX3();

        // Study with a different lang code
        var studyWithDifferentRepoCode = getCmmStudyOfLanguageCodeEnX1().getFirst()
            .withId(UUID.randomUUID().toString()).withCode("TEST");

        var studiesToIngest = new ArrayList<>(studyOfLanguages);
        studiesToIngest.add(studyWithDifferentRepoCode);

        ESIngestService ingestService = new ESIngestService(elasticsearchClient, esConfigProp);
        ingestService.bulkIndex(studiesToIngest, LANGUAGE_ISO_CODE);
        elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(INDEX_NAME)));

        // Given
        var repoCode = studyOfLanguages.getFirst().code();

        // Expect 3 studies to be returned
        var studies = ingestService.getStudiesByRepository(repoCode, LANGUAGE_ISO_CODE);
        then(studies).containsAll(studyOfLanguages).doesNotContain(studyWithDifferentRepoCode);

        // Expect 1 study to be returned
        studies = ingestService.getStudiesByRepository("TEST", LANGUAGE_ISO_CODE);
        then(studies).contains(studyWithDifferentRepoCode).doesNotContainAnyElementsOf(studyOfLanguages);
    }
}
