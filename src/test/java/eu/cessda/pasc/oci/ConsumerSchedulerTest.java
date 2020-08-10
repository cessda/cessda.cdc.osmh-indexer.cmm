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
package eu.cessda.pasc.oci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.configurations.UtilitiesConfiguration;
import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.harvester.HarvesterConsumerService;
import eu.cessda.pasc.oci.harvester.LanguageDocumentExtractor;
import eu.cessda.pasc.oci.metrics.MicrometerMetrics;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.helpers.DebuggingJMXBean;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.*;
import static eu.cessda.pasc.oci.mock.data.ReposTestData.getEndpoints;
import static org.mockito.Mockito.*;


/**
 * @author moses AT doraventures DOT com
 */
public class ConsumerSchedulerTest extends AbstractSpringTestProfileContext {
    // mock for debug logging
    private final DebuggingJMXBean debuggingJMXBean = mock(DebuggingJMXBean.class);
    private final AppConfigurationProperties appConfigurationProperties = mock(AppConfigurationProperties.class);
    private final IngestService esIndexer = mock(IngestService.class);
    private final LanguageDocumentExtractor extractor = new LanguageDocumentExtractor(appConfigurationProperties);
    private final ObjectMapper objectMapper = new UtilitiesConfiguration(null).objectMapper();
    private final MicrometerMetrics micrometerMetrics = mock(MicrometerMetrics.class);
    // Class under test
    private ConsumerScheduler scheduler;
    private HarvesterConsumerService harvesterConsumerService;

    public ConsumerSchedulerTest() {

        when(debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints()).thenReturn("printed repo info");
        when(debuggingJMXBean.printElasticSearchInfo()).thenReturn("printed ES Info");

        // mock for configuration of our repos
        when(appConfigurationProperties.getEndpoints()).thenReturn(getEndpoints());
        when(appConfigurationProperties.getLanguages()).thenReturn(Arrays.asList("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv"));
    }

  @Test
  public void shouldHarvestAndIngestAllMetadata() throws IOException {
    // mock for our record headers
    harvesterConsumerService = mock(HarvesterConsumerService.class);
    CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
    List<RecordHeader> recordHeaderList = objectMapper.readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE, collectionType);
    when(harvesterConsumerService.listRecordHeaders(any(Repo.class), any())).thenReturn(recordHeaderList);
    // mock record requests from each header
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("998"))).thenReturn(getSyntheticCmmStudy("998"));
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("997"))).thenReturn(getSyntheticCmmStudy("997"));
    // mock for ES bulking
    when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
    when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());

    // Given
    var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
    scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

    // When
    scheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();

    thenVerifyFullRun();
  }

  @Test
  public void shouldHarvestAndIngestAllMetadataForWeeklyRun() throws IOException {

    // mock for our record headers
    harvesterConsumerService = mock(HarvesterConsumerService.class);
    CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
    List<RecordHeader> recordHeaderList = objectMapper.readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE, collectionType);
    when(harvesterConsumerService.listRecordHeaders(any(Repo.class), any())).thenReturn(recordHeaderList);
    // mock record requests from each header
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("998"))).thenReturn(getSyntheticCmmStudy("998"));
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("997"))).thenReturn(getSyntheticCmmStudy("997"));
    // mock for ES bulking
    when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
    when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
    when(esIndexer.getStudy(Mockito.eq("UKDS__998"), Mockito.anyString())).thenReturn(Optional.of(getCmmStudyOfLanguageCodeEnX1().get(0)));

    // Given
    var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
    scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

    // When
    scheduler.weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords();

    thenVerifyFullRun();
  }

  private void thenVerifyFullRun() {
      verify(debuggingJMXBean, times(1)).printElasticSearchInfo();
      verify(debuggingJMXBean, times(1)).printCurrentlyConfiguredRepoEndpoints();
      verifyNoMoreInteractions(debuggingJMXBean);

      verify(appConfigurationProperties, times(1)).getEndpoints();
      verify(appConfigurationProperties, atLeastOnce()).getLanguages();
      verifyNoMoreInteractions(appConfigurationProperties);

      verify(harvesterConsumerService, times(1)).listRecordHeaders(any(Repo.class), any());
      verify(harvesterConsumerService, times(2)).getRecord(any(Repo.class), anyString());
      verifyNoMoreInteractions(harvesterConsumerService);

      // No bulk attempt should have been made for "sv" as it does not have the minimum valid cmm fields
      // 'en', 'fi', 'de' has all minimum fields
      verify(esIndexer, times(3)).bulkIndex(anyList(), anyString());

      // Called for logging purposes
    verify(esIndexer, atLeastOnce()).getStudy(Mockito.anyString(), Mockito.anyString());
    verify(esIndexer, times(1)).getTotalHitCount("*");
    verifyNoMoreInteractions(esIndexer);
  }

  @Test
  public void shouldDoIncrementalHarvestAndIngestionOfNewerRecordsOnly() throws IOException {
    // MOCKS ---------------------------------------------------------------------------------------------------------
    // mock for our record headers
    harvesterConsumerService = mock(HarvesterConsumerService.class);
    CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
    List<RecordHeader> recordHeaderList = objectMapper.readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE, collectionType);
    List<RecordHeader> recordHeaderListIncrement = objectMapper.readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE_WITH_INCREMENT, collectionType);
    when(harvesterConsumerService.listRecordHeaders(any(Repo.class), any()))
            .thenReturn(recordHeaderList) // First call
            .thenReturn(recordHeaderListIncrement); // Second call / Incremental run

    // mock record requests from each header
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("998"))).thenReturn(getSyntheticCmmStudy("998"));
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("997"))).thenReturn(getSyntheticCmmStudy("997"));
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("999"))).thenReturn(getSyntheticCmmStudy("999"));
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("1000"))).thenReturn(getSyntheticCmmStudy("1000"));

    // mock for ES methods
    when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
    when(esIndexer.getMostRecentLastModified()).thenReturn(Optional.of(LocalDateTime.parse("2018-02-20T07:48:38")));
    when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
    when(esIndexer.getStudy(Mockito.eq("UKDS__999"), Mockito.anyString())).thenReturn(Optional.of(getCmmStudyOfLanguageCodeEnX1().get(0)));

    // Given
    var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, micrometerMetrics);
      scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

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
      verify(harvesterConsumerService, times(5)).getRecord(any(Repo.class), anyString());
      verifyNoMoreInteractions(harvesterConsumerService);

      verify(esIndexer, times(1)).getMostRecentLastModified(); // Call by incremental run to get LastModified
      // No bulk attempt should have been made for "sv" as we dont have any records for "sv". We do for 'en', 'fi', 'de'
      verify(esIndexer, times(6)).bulkIndex(anyList(), anyString());

    // Called for logging purposes
    verify(esIndexer, atLeastOnce()).getStudy(Mockito.anyString(), Mockito.anyString());
    verify(esIndexer, times(2)).getTotalHitCount("*");
    verifyNoMoreInteractions(esIndexer);
  }
}
