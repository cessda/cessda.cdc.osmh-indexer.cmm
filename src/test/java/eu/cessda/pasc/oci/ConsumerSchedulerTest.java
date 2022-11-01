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
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.metrics.MicrometerMetrics;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.RecordHeaderParser;
import eu.cessda.pasc.oci.parser.RecordXMLParser;
import eu.cessda.pasc.oci.service.DebuggingJMXBean;
import org.elasticsearch.ElasticsearchException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.*;
import static eu.cessda.pasc.oci.mock.data.ReposTestData.getSingleEndpoint;
import static eu.cessda.pasc.oci.mock.data.ReposTestData.getUKDSRepo;
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
    private final PipelineUtilities pipelineUtilities = mock(PipelineUtilities.class);
    private final LanguageExtractor extractor = new LanguageExtractor(appConfigurationProperties);
    private final MicrometerMetrics micrometerMetrics = mock(MicrometerMetrics.class);
    private final RecordXMLParser recordXMLParser = mock(RecordXMLParser.class);
    private final RecordHeaderParser recordHeaderParser = mock(RecordHeaderParser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final CollectionType RECORD_HEADER_LIST = objectMapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);

    public ConsumerSchedulerTest() {
        // mock for configuration of our repos
        when(appConfigurationProperties.getEndpoints()).thenReturn(getSingleEndpoint());
        when(appConfigurationProperties.getLanguages()).thenReturn(List.of("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv"));
    }

    private DebuggingJMXBean mockDebuggingJMXBean() throws IOException {
        var debuggingJMXBean = mock(DebuggingJMXBean.class);

        when(debuggingJMXBean.printElasticSearchInfo()).thenReturn("printed ES Info");

        return debuggingJMXBean;
    }

    @Test
    public void shouldHarvestAndIngestAllMetadata() throws IOException, IndexerException {
        // mock for our record headers
        var harvesterConsumerService = mockRecordRequests();
        var debuggingJMXBean = mockDebuggingJMXBean();

        // mock for ES bulking
        when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());

        // Mock requests for indexed repository content
        when(esIndexer.getStudiesByRepository(anyString(), anyString())).thenReturn(Collections.emptySet());

        // Given
        var harvesterRunner = new IndexerRunner(appConfigurationProperties, harvesterConsumerService, pipelineUtilities, esIndexer, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();

        thenVerifyFullRun(debuggingJMXBean);
    }

    @Test
    public void shouldHarvestAndIngestAllMetadataForWeeklyRun() throws IOException, IndexerException {

        // mock for our record headers
        var harvesterConsumerService = mockRecordRequests();
        var debuggingJMXBean = mockDebuggingJMXBean();

        // mock for ES bulking
        when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
        when(esIndexer.getStudy(Mockito.eq("UKDS__998"), Mockito.anyString())).thenReturn(Optional.of(getCmmStudyOfLanguageCodeEnX1().get(0)));

        // Given
        var harvesterRunner = new IndexerRunner(appConfigurationProperties, harvesterConsumerService, pipelineUtilities, esIndexer, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords();

        thenVerifyFullRun(debuggingJMXBean);
    }

    @Test
    public void shouldLogErrorOnException() throws IOException {

        var debuggingJMXBean = mockDebuggingJMXBean();

        // Throw a non-specific exception
        var indexerConsumerService = mock(IndexerConsumerService.class);
        when(indexerConsumerService.getRecords(any(Repo.class), any()))
            .thenThrow(RuntimeException.class);

        // Given
        var harvesterRunner = new IndexerRunner(appConfigurationProperties, indexerConsumerService, pipelineUtilities, esIndexer, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();

        // Verify hit counts were obtained
        verify(esIndexer).getTotalHitCount("*");

        // Verify that nothing else happened
        verifyNoMoreInteractions(esIndexer);
    }

    /**
     * Creates a mocked {@link IndexerConsumerService} that responds to header and record requests.
     */
    private IndexerConsumerService mockRecordRequests() throws IOException, IndexerException {

        var indexerConsumerService = new IndexerConsumerService(extractor, recordHeaderParser, recordXMLParser);
        var recordHeaders = objectMapper.<List<RecordHeader>>readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE, RECORD_HEADER_LIST);

        // Mock requests for the repository headers
        when(recordHeaderParser.getRecordHeaders(any(Repo.class))).thenReturn(recordHeaders);

        // mock record requests from each header
        var ukdsRepo = getUKDSRepo();
        for (var recordHeader : recordHeaders) {
            when(recordXMLParser.getRecord(
                ukdsRepo,
                new Record(recordHeader, new Record.Request(ukdsRepo.getUrl(), ukdsRepo.getPreferredMetadataParam()),null)
            )).thenReturn(Optional.of(getSyntheticCmmStudy(recordHeader.getIdentifier())));
        }

        return indexerConsumerService;
    }

    private void thenVerifyFullRun(DebuggingJMXBean debuggingJMXBean) throws IOException, IndexerException {
        verify(debuggingJMXBean, times(1)).printElasticSearchInfo();
        verifyNoMoreInteractions(debuggingJMXBean);

        verify(appConfigurationProperties, times(1)).getEndpoints();
        verify(appConfigurationProperties, atLeastOnce()).getLanguages();
        verify(appConfigurationProperties, atLeastOnce()).getBaseDirectory();
        verifyNoMoreInteractions(appConfigurationProperties);

        verify(recordHeaderParser, times(1)).getRecordHeaders(any(Repo.class));
        verifyNoMoreInteractions(recordHeaderParser);

        verify(recordXMLParser, times(2)).getRecord(any(Repo.class), any(Record.class));
        verifyNoMoreInteractions(recordXMLParser);

        // No bulk attempt should have been made for "sv" as it does not have the minimum valid cmm fields
        // 'en', 'fi', 'de' has all minimum fields
        verify(esIndexer, times(3)).bulkIndex(anyList(), anyString());
        verify(esIndexer, times(3)).bulkDelete(anyList(), anyString());

        // Called for deletions
        verify(esIndexer, times(3)).getStudiesByRepository(anyString(), anyString());

        // Called for logging purposes
        verify(esIndexer, times(6)).getStudy(Mockito.anyString(), Mockito.anyString());
        verify(esIndexer, times(1)).getTotalHitCount("*");
        verifyNoMoreInteractions(esIndexer);
    }

    @Test
    public void shouldDoIncrementalHarvestAndIngestionOfNewerRecordsOnly() throws IOException, IndexerException {
        // MOCKS ---------------------------------------------------------------------------------------------------------
        var debuggingJMXBean = mockDebuggingJMXBean();
        // mock for our record headers
        var indexerConsumerService = new IndexerConsumerService(extractor, recordHeaderParser, recordXMLParser);
        var recordHeaderList = objectMapper.<List<RecordHeader>>readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE, RECORD_HEADER_LIST);
        var recordHeaderListIncrement = objectMapper.<List<RecordHeader>>readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE_WITH_INCREMENT, RECORD_HEADER_LIST);

        when(recordHeaderParser.getRecordHeaders(any(Repo.class)))
            .thenReturn(recordHeaderList) // First call
            .thenReturn(recordHeaderListIncrement); // Second call / Incremental run

        // mock record requests from each header, a set is used so that each header is only registered once
        var allRecordHeaders = new HashSet<>(recordHeaderList);
        allRecordHeaders.addAll(recordHeaderListIncrement);
        var ukdsRepo = getUKDSRepo();
        for (var recordHeader : allRecordHeaders) {
            when(recordXMLParser.getRecord(ukdsRepo, new Record(recordHeader, new Record.Request(ukdsRepo.getUrl(), ukdsRepo.getPreferredMetadataParam()),null)))
                .thenReturn(Optional.of(getSyntheticCmmStudy(recordHeader.getIdentifier())));
        }

        // mock for ES methods
        when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
        when(esIndexer.getMostRecentLastModified()).thenReturn(Optional.of(LocalDateTime.parse("2018-02-20T07:48:38")));
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
        when(esIndexer.getStudy(Mockito.eq("UKDS__999"), Mockito.anyString())).thenReturn(Optional.of(getCmmStudyOfLanguageCodeEnX1().get(0)));

        // Given
        var harvesterRunner = new IndexerRunner(appConfigurationProperties, indexerConsumerService, pipelineUtilities, esIndexer, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();
        scheduler.dailyIncrementalHarvestAndIngestionAllConfiguredSPsReposRecords();

        verify(debuggingJMXBean, times(2)).printElasticSearchInfo();
        verifyNoMoreInteractions(debuggingJMXBean);

        verify(appConfigurationProperties, times(2)).getEndpoints();
        verify(appConfigurationProperties, atLeastOnce()).getLanguages();
        verify(appConfigurationProperties, times(2)).getBaseDirectory();
        verifyNoMoreInteractions(appConfigurationProperties);

        verify(recordHeaderParser, times(2)).getRecordHeaders(any(Repo.class));
        verifyNoMoreInteractions(recordHeaderParser);

        // Expects 5 GetRecord call 2 from Full run and 3 from incremental run (minuses old lastModified record)
        verify(recordXMLParser, times(5)).getRecord(any(Repo.class), any(Record.class));
        verifyNoMoreInteractions(recordXMLParser);

        verify(esIndexer, times(1)).getMostRecentLastModified(); // Call by incremental run to get LastModified
        // No bulk attempt should have been made for "sv" as we don't have any records for "sv". We do for 'en', 'fi', 'de'
        verify(esIndexer, times(6)).bulkIndex(anyList(), anyString());
        verify(esIndexer, times(6)).bulkDelete(anyList(), anyString());

        // Called for logging purposes
        verify(esIndexer, atLeastOnce()).getStudy(Mockito.anyString(), Mockito.anyString());
        verify(esIndexer, times(2)).getTotalHitCount("*");
        verify(esIndexer, times(6)).getStudiesByRepository(anyString(), anyString());
        verifyNoMoreInteractions(esIndexer);
    }

    @Test
    public void shouldHandleElasticsearchExceptions() throws IOException, IndexerException {
        // mock for our record headers
        var harvesterConsumerService = mockRecordRequests();
        var debuggingJMXBean = mockDebuggingJMXBean();

        // mock for ES bulking
        when(esIndexer.bulkIndex(anyList(), anyString())).thenThrow(new ElasticsearchException("Mocked"));
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());

        // Given
        var harvesterRunner = new IndexerRunner(appConfigurationProperties, harvesterConsumerService, pipelineUtilities, esIndexer, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords();

        // Verify that the mock was called
        verify(esIndexer, times(6)).getStudy(Mockito.anyString(), Mockito.anyString());
        verify(esIndexer, times(1)).getTotalHitCount("*");
        verify(esIndexer, times(3)).bulkIndex(anyList(), anyString());
        verify(esIndexer, times(3)).getStudiesByRepository(anyString(), anyString());
        verifyNoMoreInteractions(esIndexer);
    }

    @Test
    public void shouldHandleIOExceptions() throws IOException, IndexerException {
        // mock for our record headers
        var harvesterConsumerService = mockRecordRequests();
        var debuggingJMXBean = mockDebuggingJMXBean();

        // mock for ES bulking
        when(esIndexer.bulkIndex(anyList(), anyString())).thenThrow(IOException.class);
        when(esIndexer.getTotalHitCount("*")).thenThrow(IOException.class);
        when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());

        // Given
        var harvesterRunner = new IndexerRunner(appConfigurationProperties, harvesterConsumerService, pipelineUtilities, esIndexer, micrometerMetrics);
        var scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

        // When
        scheduler.weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords();

        // Verify that the mock was called
        verify(esIndexer, times(6)).getStudy(Mockito.anyString(), Mockito.anyString());
        verify(esIndexer, times(1)).getTotalHitCount("*");
        verify(esIndexer, times(3)).bulkIndex(anyList(), anyString());
        verify(esIndexer, times(3)).getStudiesByRepository(anyString(), anyString());
        verifyNoMoreInteractions(esIndexer);
    }
}
