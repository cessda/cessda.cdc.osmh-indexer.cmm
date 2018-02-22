package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.dao.HarvesterDao;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static eu.cessda.pasc.oci.data.RecordHeadersTestData.LIST_RECORDER_HEADERS_BODY_EXAMPLE;
import static eu.cessda.pasc.oci.data.ReposTestData.getUKDSRepo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultConsumerServiceTest {

  @Mock
  private HarvesterDao harvesterDao;

  @InjectMocks
  @Autowired
  DefaultConsumerService consumerService;

  @Test
  public void shouldReturnASuccessfulResponse() throws ExternalSystemException {

    when(harvesterDao.listRecordHeaders(anyString())).thenReturn(LIST_RECORDER_HEADERS_BODY_EXAMPLE);
    Repo repo = getUKDSRepo();

    List<RecordHeader> recordHeaders = consumerService.listRecorderHeadersBody(repo);
    assertThat(recordHeaders).hasSize(2);
    recordHeaders.forEach(System.out::println);
  }
}