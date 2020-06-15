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

package eu.cessda.pasc.oci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.metrics.MicrometerMetrics;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import eu.cessda.pasc.oci.service.IngestService;
import eu.cessda.pasc.oci.service.helpers.DebuggingJMXBean;
import eu.cessda.pasc.oci.service.helpers.LanguageAvailabilityMapper;
import eu.cessda.pasc.oci.service.helpers.LanguageDocumentExtractor;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.*;
import static eu.cessda.pasc.oci.mock.data.ReposTestData.getEndpoints;
import static org.mockito.Mockito.*;


/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
public class ConsumerSchedulerTest extends AbstractSpringTestProfileContext {
  // Class under test
  private ConsumerScheduler scheduler;
  private DebuggingJMXBean debuggingJMXBean;
  private HarvesterConsumerService harvesterConsumerService;
  private AppConfigurationProperties appConfigurationProperties;
  private IngestService esIndexer;

  @Autowired
  private LanguageDocumentExtractor extractor;

  @Autowired
  private LanguageAvailabilityMapper languageAvailabilityMapper;

  @Autowired
  ObjectMapper objectMapper;

  private final MicrometerMetrics micrometerMetrics = mock(MicrometerMetrics.class);
  private final ElasticsearchTemplate esTemplate = mock(ElasticsearchTemplate.class);

  @Before
  public void setUp() {

    // mock for debug logging
    debuggingJMXBean = mock(DebuggingJMXBean.class);
    when(debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints()).thenReturn("printed repo info");
    when(debuggingJMXBean.printElasticSearchInfo()).thenReturn("printed ES Info");

    // mock for configuration of our repos
    appConfigurationProperties = mock(AppConfigurationProperties.class);
    when(appConfigurationProperties.getEndpoints()).thenReturn(getEndpoints());

    // mock the elasticsearch
    Client esClient = mock(Client.class);
    when(esTemplate.getClient()).thenReturn(esClient);
    SearchRequestBuilder builder = mock(SearchRequestBuilder.class);
    when(esClient.prepareSearch(anyString())).thenReturn(builder);
    SearchHits mockSearchHits = mock(SearchHits.class);
    when(mockSearchHits.getTotalHits()).thenReturn(20L);
    SearchResponse response = mock(SearchResponse.class);
    when(response.getHits()).thenReturn(mockSearchHits);
    when(builder.setTypes(anyString())).thenReturn(builder);
    when(builder.setSearchType(any(SearchType.class))).thenReturn(builder);
    when(builder.setQuery(any(MatchAllQueryBuilder.class))).thenReturn(builder);
    when(builder.get()).thenReturn(response);
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
    esIndexer = mock(IngestService.class);
    when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
    when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());

    // Given
    var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, languageAvailabilityMapper, micrometerMetrics);
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
    esIndexer = mock(IngestService.class);
    when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
    when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
    when(esIndexer.getStudy(Mockito.eq("UKDS__998"), Mockito.anyString())).thenReturn(Optional.of(getCmmStudyOfLanguageCodeEnX1().get(0)));

    // Given
    var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, languageAvailabilityMapper, micrometerMetrics);
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
    esIndexer = mock(IngestService.class);
    when(esIndexer.bulkIndex(anyList(), anyString())).thenReturn(true);
    when(esIndexer.getMostRecentLastModified()).thenReturn(Optional.of(LocalDateTime.parse("2018-02-20T07:48:38")));
    when(esIndexer.getStudy(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
    when(esIndexer.getStudy(Mockito.eq("UKDS__999"), Mockito.anyString())).thenReturn(Optional.of(getCmmStudyOfLanguageCodeEnX1().get(0)));

    // Given
    var harvesterRunner = new HarvesterRunner(appConfigurationProperties, harvesterConsumerService, harvesterConsumerService, esIndexer, extractor, languageAvailabilityMapper, micrometerMetrics);
    scheduler = new ConsumerScheduler(debuggingJMXBean, esIndexer, harvesterRunner);

    // When
    scheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();
    scheduler.dailyIncrementalHarvestAndIngestionAllConfiguredSPsReposRecords();

    verify(debuggingJMXBean, times(2)).printElasticSearchInfo();
    verify(debuggingJMXBean, times(2)).printCurrentlyConfiguredRepoEndpoints();
    verifyNoMoreInteractions(debuggingJMXBean);

    verify(appConfigurationProperties, times(2)).getEndpoints();
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
