package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.data.ReposTestData;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import static eu.cessda.pasc.oci.data.RecordTestData.LIST_RECORDER_HEADERS_BODY_EXAMPLE;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;


/**
 * Test for the Harvester
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
public class PascHarvesterDaoTest extends AbstractSpringTestProfileContext {

  @Autowired
  private RestTemplate restTemplate;
  private MockRestServiceServer serverMock;

  @Autowired
  private PascHarvesterDao pascHarvesterDao;


  @Before
  public void setUp() {
    serverMock = MockRestServiceServer.bindTo(restTemplate).build();
  }

  @Test
  public void shouldReturnSuccessfulHttpResponseListRecord() throws ExternalSystemException {

    // Given
    String expected_url = "https://datacatalogue.cessda.eu/osmh-repo/v0/ListRecordHeaders?" +
        "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

    serverMock.expect(once(), requestTo(expected_url))
        .andExpect(method(GET))
        .andRespond(MockRestResponseCreators.withSuccess(LIST_RECORDER_HEADERS_BODY_EXAMPLE, MediaType.APPLICATION_JSON)
        );

    // When
    String recordHeaders = pascHarvesterDao.listRecordHeaders(ReposTestData.getUKDSRepo().getUrl());

    then(recordHeaders).isEqualTo(LIST_RECORDER_HEADERS_BODY_EXAMPLE);
  }

  @Test
  public void shouldReturnSuccessfulHttpResponseForGetRecord() throws ExternalSystemException {

    // Given
    FileHandler fileHandler = new FileHandler();
    String studyNumber = "998";
    String recordUkds998 = fileHandler.getFileWithUtil("record_ukds_998.json");
    String expected_url = "https://datacatalogue.cessda.eu/osmh-repo/v0/GetRecord/CMMStudy/998?" +
        "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

    serverMock.expect(once(), requestTo(expected_url))
        .andExpect(method(GET))
        .andRespond(MockRestResponseCreators.withSuccess(recordUkds998, MediaType.APPLICATION_JSON)
        );

    // When
    String record = pascHarvesterDao.getRecord(ReposTestData.getUKDSRepo().getUrl(), studyNumber);

    then(record).isEqualTo(recordUkds998);
  }
}
