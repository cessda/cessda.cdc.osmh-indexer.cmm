package eu.cessda.pasc.oci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import eu.cessda.pasc.oci.service.IngestService;
import eu.cessda.pasc.oci.service.helpers.DebuggingJMXBean;
import eu.cessda.pasc.oci.service.helpers.LanguageDocumentExtractor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

import static eu.cessda.pasc.oci.data.RecordTestData.LIST_RECORDER_HEADERS_BODY_EXAMPLE;
import static eu.cessda.pasc.oci.data.RecordTestData.getCmmStudy;
import static eu.cessda.pasc.oci.data.ReposTestData.getEndpoints;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


/**
 * @author moses@doraventures.com
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
  ObjectMapper objectMapper;

  // We want this as light weight as possible so mocking everything.
  @Before
  public void setUp() throws IOException {

    // mock for debug logging
    debuggingJMXBean = mock(DebuggingJMXBean.class);
    when(debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints()).thenReturn("printed repo info");
    when(debuggingJMXBean.printElasticSearchInfo()).thenReturn("printed ES Info");

    // mock for configuration of our repos
    appConfigurationProperties = mock(AppConfigurationProperties.class);
    when(appConfigurationProperties.getEndpoints()).thenReturn(getEndpoints());

    // mock for our record headers
    harvesterConsumerService = mock(HarvesterConsumerService.class);
    CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
    List<RecordHeader> recordHeaderList = objectMapper.readValue(LIST_RECORDER_HEADERS_BODY_EXAMPLE, collectionType);
    when(harvesterConsumerService.listRecordHeaders(any(Repo.class), any())).thenReturn(recordHeaderList);

    // mock record requests from each header
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("998"))).thenReturn(getCmmStudy("998"));
    when(harvesterConsumerService.getRecord(any(Repo.class), eq("997"))).thenReturn(getCmmStudy("997"));

    // mock for ES bulking
    esIndexer = mock(IngestService.class);
    when(esIndexer.bulkIndex(anyListOf(CMMStudyOfLanguage.class), anyString())).thenReturn(true);
  }


  @Test
  public void shouldIndexAllMetadataInit() {
    // Given
    scheduler = new ConsumerScheduler(debuggingJMXBean, appConfigurationProperties, harvesterConsumerService, esIndexer, extractor);

    // When
    scheduler.harvestAndIngestRecordsForAllConfiguredSPsRepos();

    verify(debuggingJMXBean, times(1)).printElasticSearchInfo();
    verify(debuggingJMXBean, times(1)).printCurrentlyConfiguredRepoEndpoints();
    verifyNoMoreInteractions(debuggingJMXBean);

    verify(appConfigurationProperties, times(1)).getEndpoints();
    verifyNoMoreInteractions(appConfigurationProperties);

    verify(harvesterConsumerService, times(1)).listRecordHeaders(any(Repo.class), any());
    verify(harvesterConsumerService, times(2)).getRecord(any(Repo.class), anyString());
    verifyNoMoreInteractions(harvesterConsumerService);

    // No bulk attempt should have been made for "sv" as we dont have any records for "sv". We do for 'en', 'fi', 'de'
    verify(esIndexer, times(3)).bulkIndex(anyListOf(CMMStudyOfLanguage.class), anyString());
    verifyNoMoreInteractions(esIndexer);
  }
}