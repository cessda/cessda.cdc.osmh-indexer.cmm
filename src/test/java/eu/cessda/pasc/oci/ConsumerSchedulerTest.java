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
package eu.cessda.pasc.oci;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.harvester.HarvesterConsumerService;
import eu.cessda.pasc.oci.harvester.LanguageExtractor;
import eu.cessda.pasc.oci.metrics.MicrometerMetrics;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.DebuggingJMXBean;
import org.elasticsearch.ElasticsearchException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.*;
import static eu.cessda.pasc.oci.mock.data.ReposTestData.getSingleEndpoint;
import static org.mockito.Mockito.*;


/**
 * Tests related to {@link ConsumerScheduler}
 *
 * @author moses AT doraventures DOT com
 */
public class ConsumerSchedulerTest {
    // mock for debug logging
    private final AppConfigurationProperties appConfigurationProperties = mock(AppConfigurationProperties.class);
    private final IngestService esIndexer = mock(IngestService.class);
    private final LanguageExtractor extractor = new LanguageExtractor(appConfigurationProperties);
    private final MicrometerMetrics micrometerMetrics = mock(MicrometerMetrics.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ConsumerSchedulerTest() {
        // mock for configuration of our repos
        when(appConfigurationProperties.getEndpoints()).thenReturn(getSingleEndpoint());
        when(appConfigurationProperties.getLanguages()).thenReturn(Arrays.asList("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv"));
    }

    private DebuggingJMXBean mockDebuggingJMXBean() throws IOException {
        var debuggingJMXBean = mock(DebuggingJMXBean.class);

        when(debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints()).thenReturn("printed repo info");
        when(debuggingJMXBean.printElasticSearchInfo()).thenReturn("printed ES Info");

        return debuggingJMXBean;
    }

    @Test
    public void shouldHarvestAndIngestAllMetadata() throws IOException {
        // mock for our record headers
        var harvesterConsumerService = mockRecordRequests();
        var debuggingJMXBean = mockDebuggingJMXBean();

        // mock for ES bulking
        when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());

        // Given
        var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();

        thenVerifyFullRun(harvesterConsumerService, debuggingJMXBean);
    }

    @Test
    public void shouldHarvestAndIngestAllMetadataForWeeklyRun() throws IOException {

        // mock for our record headers
        var harvesterConsumerService = mockRecordRequests();
        var debuggingJMXBean = mockDebuggingJMXBean();

        // mock for ES bulking
        when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
        when(esIndexer.getStudy(Mockito.eq("UKDS__998"), Mockito.anyString())).thenReturn(Optional.of(getCmmStudyOfLanguageCodeEnX1().get(0)));

        // Given
        var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords();

        thenVerifyFullRun(harvesterConsumerService, debuggingJMXBean);
    }

    @Test
    public void shouldLogErrorOnException() throws IOException {

        var debuggingJMXBean = mockDebuggingJMXBean();

        // Throw a non-specific exception
        var harvesterConsumerService = mock(HarvesterConsumerService.class);
        when(harvesterConsumerService.listRecordHeaders(any(Repo.class), any()))
            .thenThrow(RuntimeException.class);

        // Given
        var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();

        // Verify that nothing else happened
        verifyNoMoreInteractions(esIndexer);
    }

    /**
     * Creates a mocked {@link HarvesterConsumerService} that responds to header and record requests.
     */
    private HarvesterConsumerService mockRecordRequests() throws IOException {
        var harvesterConsumerService = mock(HarvesterConsumerService.class);
        var collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
        var recordHeaders = objectMapper.<List<RecordHeader>>readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE, collectionType);

        // Mock requests for the repository headers
        when(harvesterConsumerService.listRecordHeaders(any(Repo.class), any()))
            .thenReturn(recordHeaders.stream().map(recordHeader -> new Record(recordHeader, null)));

        // mock record requests from each header
        for (var recordHeader : recordHeaders) {
            when(harvesterConsumerService.getRecord(any(Repo.class), eq(new Record(recordHeader, null))))
                .thenReturn(Optional.of(getSyntheticCmmStudy(recordHeader.getIdentifier())));
        }

        return harvesterConsumerService;
    }

    private void thenVerifyFullRun(HarvesterConsumerService harvesterConsumerService, DebuggingJMXBean debuggingJMXBean) throws IOException {
        verify(debuggingJMXBean, times(1)).printElasticSearchInfo();
        verify(debuggingJMXBean, times(1)).printCurrentlyConfiguredRepoEndpoints();
        verifyNoMoreInteractions(debuggingJMXBean);

        verify(appConfigurationProperties, times(1)).getEndpoints();
        verify(appConfigurationProperties, atLeastOnce()).getLanguages();
        verifyNoMoreInteractions(appConfigurationProperties);

        verify(harvesterConsumerService, times(1)).listRecordHeaders(any(Repo.class), any());
        verify(harvesterConsumerService, times(2)).getRecord(any(Repo.class), any(Record.class));
        verifyNoMoreInteractions(harvesterConsumerService);

        // No bulk attempt should have been made for "sv" as it does not have the minimum valid cmm fields
        // 'en', 'fi', 'de' has all minimum fields
        verify(esIndexer, times(3)).bulkIndex(anyList(), anyString());
        verify(esIndexer, times(3)).bulkDelete(anyList(), anyString());

        // Called for logging purposes
        verify(esIndexer, times(6)).getStudy(Mockito.anyString(), Mockito.anyString());
        verify(esIndexer, times(1)).getTotalHitCount("*");
        verifyNoMoreInteractions(esIndexer);
    }

    @Test
    public void shouldDoIncrementalHarvestAndIngestionOfNewerRecordsOnly() throws IOException {
        // MOCKS ---------------------------------------------------------------------------------------------------------
        var debuggingJMXBean = mockDebuggingJMXBean();
        // mock for our record headers
        var harvesterConsumerService = mock(HarvesterConsumerService.class);
        var collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
        var recordHeaderList = objectMapper.<List<RecordHeader>>readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE, collectionType);
        var recordHeaderListIncrement = objectMapper.<List<RecordHeader>>readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE_WITH_INCREMENT, collectionType);
        when(harvesterConsumerService.listRecordHeaders(any(Repo.class), any()))
            .thenReturn(recordHeaderList.stream().map(recordHeader -> new Record(recordHeader, null))) // First call
            .thenReturn(recordHeaderListIncrement.stream().map(recordHeader -> new Record(recordHeader, null))); // Second call / Incremental run

        // mock record requests from each header, a set is used so that each header is only registered once
        var allRecordHeaders = new HashSet<>(recordHeaderList);
        allRecordHeaders.addAll(recordHeaderListIncrement);
        for (var recordHeader : allRecordHeaders) {
            when(harvesterConsumerService.getRecord(any(Repo.class), eq(new Record(recordHeader, null))))
                .thenReturn(Optional.of(getSyntheticCmmStudy(recordHeader.getIdentifier())));
        }

        // mock for ES methods
        when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
        when(esIndexer.getMostRecentLastModified()).thenReturn(Optional.of(LocalDateTime.parse("2018-02-20T07:48:38")));
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
        when(esIndexer.getStudy(Mockito.eq("UKDS__999"), Mockito.anyString())).thenReturn(Optional.of(getCmmStudyOfLanguageCodeEnX1().get(0)));

        // Given
        var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();
        scheduler.dailyIncrementalHarvestAndIngestionAllConfiguredSPsReposRecords();

        verify(debuggingJMXBean, times(2)).printElasticSearchInfo();
        verify(debuggingJMXBean, times(2)).printCurrentlyConfiguredRepoEndpoints();
        verifyNoMoreInteractions(debuggingJMXBean);

        verify(appConfigurationProperties, times(2)).getEndpoints();
        verify(appConfigurationProperties, atLeastOnce()).getLanguages();
        verifyNoMoreInteractions(appConfigurationProperties);

        verify(harvesterConsumerService, times(2)).listRecordHeaders(any(Repo.class), any());
        // Expects 5 GetRecord call 2 from Full run and 3 from incremental run (minuses old lastModified record)
        verify(harvesterConsumerService, times(5)).getRecord(any(Repo.class), any(Record.class));
        verifyNoMoreInteractions(harvesterConsumerService);

        verify(esIndexer, times(1)).getMostRecentLastModified(); // Call by incremental run to get LastModified
        // No bulk attempt should have been made for "sv" as we dont have any records for "sv". We do for 'en', 'fi', 'de'
        verify(esIndexer, times(6)).bulkIndex(anyList(), anyString());
        verify(esIndexer, times(6)).bulkDelete(anyList(), anyString());

        // Called for logging purposes
        verify(esIndexer, atLeastOnce()).getStudy(Mockito.anyString(), Mockito.anyString());
        verify(esIndexer, times(2)).getTotalHitCount("*");
        verifyNoMoreInteractions(esIndexer);
    }

    @Test
    public void shouldHandleElasticsearchExceptions() throws IOException {
        // mock for our record headers
        var harvesterConsumerService = mockRecordRequests();
        var debuggingJMXBean = mockDebuggingJMXBean();

        // mock for ES bulking
        when(esIndexer.bulkIndex(anyList(), anyString())).thenThrow(new ElasticsearchException("Mocked"));
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());

        // Given
        var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords();

        // Verify that the mock was called
        verify(esIndexer, times(6)).getStudy(Mockito.anyString(), Mockito.anyString());
        verify(esIndexer, times(1)).getTotalHitCount("*");
        verify(esIndexer, times(3)).bulkIndex(anyList(), anyString());
        verifyNoMoreInteractions(esIndexer);
    }

    @Test
    public void shouldHandleIOExceptions() throws IOException {
        // mock for our record headers
        var harvesterConsumerService = mockRecordRequests();
        var debuggingJMXBean = mockDebuggingJMXBean();

        // mock for ES bulking
        when(esIndexer.bulkIndex(anyList(), anyString())).thenThrow(IOException.class);
        when(esIndexer.getTotalHitCount("*")).thenThrow(IOException.class);
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());

        // Given
        var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords();

        // Verify that the mock was called
        verify(esIndexer, times(6)).getStudy(Mockito.anyString(), Mockito.anyString());
        verify(esIndexer, times(1)).getTotalHitCount("*");
        verify(esIndexer, times(3)).bulkIndex(anyList(), anyString());
        verifyNoMoreInteractions(esIndexer);
    }
}
