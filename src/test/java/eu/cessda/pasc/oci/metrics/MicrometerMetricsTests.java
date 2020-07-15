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
package eu.cessda.pasc.oci.metrics;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.mock.data.RecordTestData;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import eu.cessda.pasc.oci.models.configurations.Endpoints;
import eu.cessda.pasc.oci.models.configurations.Repo;
import io.micrometer.core.instrument.MeterRegistry;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/**
 * Tests metrics according to https://docs.google.com/spreadsheets/d/1vkqm-9sSHCgskRzKIz1R4B_8uzyJpjy1-rFooQyeDtg/edit
 */
public class MicrometerMetricsTests {

    private final AppConfigurationProperties appConfigurationProperties = Mockito.mock(AppConfigurationProperties.class);
    private final MeterRegistry meterRegistry = Mockito.mock(MeterRegistry.class);

    public MicrometerMetricsTests() {
        var endpoints = Mockito.mock(Endpoints.class);
        Mockito.when(appConfigurationProperties.getLanguages()).thenReturn(Arrays.asList("en", "de", "sv", "cz"));
        Mockito.when(appConfigurationProperties.getEndpoints()).thenReturn(endpoints);
        Mockito.when(endpoints.getRepos()).thenReturn(Arrays.asList(
                ReposTestData.getUKDSRepo(),
                ReposTestData.getGesisEnRepo()
        ));
    }

    @Test
    public void shouldReturnListOfRecordsByLanguage() {

        // When
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, null, meterRegistry);

        // All configured languages should be populated, no exception should be thrown
        for (var language : appConfigurationProperties.getLanguages()) {
            Assert.assertNotNull(micrometerMetrics.getRecordCount(language));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenAnInvalidLanguageIsSpecified() {

        // When
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, null, meterRegistry);

        // Then
        micrometerMetrics.getRecordCount("moon");
    }


    @Test
    public void shouldContainCorrectAmountOfRecordsForEachLanguage() {
        IngestService ingestService = Mockito.mock(IngestService.class);

        // When
        long expected = 42;
        long expectedEmpty = 0;
        String language = "en";
        Mockito.when(ingestService.getTotalHitCount(Mockito.anyString())).thenReturn(expectedEmpty);
        Mockito.when(ingestService.getTotalHitCount(language)).thenReturn(expected);

        // Then
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);
        micrometerMetrics.updateLanguageMetrics();

        // The expected result should be the same as returned from Elasticsearch
        for (var configuredLanguage : appConfigurationProperties.getLanguages()) {
            if (configuredLanguage.equals(language)) {
                Assert.assertEquals(expected, micrometerMetrics.getRecordCount(configuredLanguage).get());
            } else {
                Assert.assertEquals(expectedEmpty, micrometerMetrics.getRecordCount(configuredLanguage).get());
            }
        }
    }

    @Test
    public void shouldSetRecordCountToZeroWhenIndexIsNotFound() {
        IngestService ingestService = Mockito.mock(IngestService.class);

        // When
        String language = "en";
        Mockito.when(ingestService.getTotalHitCount(Mockito.anyString())).thenThrow(new IndexNotFoundException(""));

        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);
        micrometerMetrics.getRecordCount(language).set(23);

        // Then
        micrometerMetrics.updateLanguageMetrics();

        Assert.assertEquals(0L, micrometerMetrics.getRecordCount(language).get());
    }

    @Test
    public void shouldGetTotalRecordsFromElasticsearch() {
        IngestService ingestService = Mockito.mock(IngestService.class);

        // When
        long toBeReturned = 160L;
        Mockito.when(ingestService.getTotalHitCount("*")).thenReturn(toBeReturned);

        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);

        micrometerMetrics.updateTotalRecordsMetric();

        Assert.assertEquals(toBeReturned, micrometerMetrics.getTotalRecords().get());
    }

    @Test
    public void shouldReturnListOfRecordsByRepo() {

        // When
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, null, meterRegistry);

        // All configured repositories should be populated, no exception should be thrown
        for (var repo : appConfigurationProperties.getEndpoints().getRepos()) {
            Assert.assertNotNull(micrometerMetrics.getRecordCount(repo));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenAnInvalidRepoIsSpecified() {

        // When
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, null, meterRegistry);

        // Then
        micrometerMetrics.getRecordCount(new Repo());
    }

    @Test
    public void shouldContainCorrectAmountOfRecordsForEachRepository() throws IOException {
        IngestService ingestService = Mockito.mock(IngestService.class);

        // When
        var testSet = new HashSet<>(RecordTestData.getCmmStudyOfLanguageCodeEnX3());
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);

        // Then
        micrometerMetrics.updateEndpointsRecordsMetric(testSet);

        Assert.assertEquals(3, micrometerMetrics.getRecordCount(ReposTestData.getUKDSRepo()).get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldLogWhenAnUnexpectedEndpointIsFound() throws IOException {
        IngestService ingestService = Mockito.mock(IngestService.class);

        // When
        var testSet = new HashSet<>(RecordTestData.getCmmStudyOfLanguageCodeEnX3());
        AppConfigurationProperties properties = Mockito.mock(AppConfigurationProperties.class);
        var endpoints = Mockito.mock(Endpoints.class);
        Mockito.when(properties.getLanguages()).thenReturn(Arrays.asList("en", "de", "sv", "cz"));
        Mockito.when(properties.getEndpoints()).thenReturn(endpoints);
        Mockito.when(endpoints.getRepos()).thenReturn(Collections.emptyList());

        var micrometerMetrics = new MicrometerMetrics(properties, ingestService, meterRegistry);

        // Then
        micrometerMetrics.updateEndpointsRecordsMetric(testSet);
        micrometerMetrics.getRecordCount(ReposTestData.getUKDSRepo());
    }

    @Test
    public void shouldContainCorrectAmountOfRecordsForEachPublisher() throws IOException {
        IngestService ingestService = Mockito.mock(IngestService.class);

        // When
        var testSet = new HashSet<>(RecordTestData.getCmmStudyOfLanguageCodeEnX3());
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);

        // Then
        micrometerMetrics.updatePublisherRecordsMetric(testSet);

        Publisher publisher = testSet.iterator().next().getPublisher();
        Assert.assertEquals(3, micrometerMetrics.getRecordCount(publisher).get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnInvalidPublisher() {
        IngestService ingestService = Mockito.mock(IngestService.class);
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);

        // Then
        micrometerMetrics.getRecordCount(new Publisher());
    }

    @Test
    public void shouldUpdateAllMetrics() {
        IngestService ingestService = Mockito.mock(IngestService.class);
        Mockito.when(ingestService.getAllStudies("*")).thenReturn(Collections.emptySet());
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);

        // Then
        micrometerMetrics.updateMetrics();
    }
}
