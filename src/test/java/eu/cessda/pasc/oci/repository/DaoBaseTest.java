package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * Test for the DaoBase
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
public class DaoBaseTest extends AbstractSpringTestProfileContext {

  private MockRestServiceServer serverMock;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  @Qualifier("daoBase")
  private DaoBase underTest;

  @Before
  public void setUp() {
    serverMock = MockRestServiceServer.bindTo(restTemplate).build();
  }

  @Test
  public void shouldPostForStringResponse() throws ExternalSystemException {

    // Given
    String expectedUrl = "http://cdc-osmh-repo:9091/v0/ListRecordHeaders?" +
        "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

    serverMock.expect(once(), requestTo(expectedUrl))
        .andExpect(method(GET))
        .andRespond(MockRestResponseCreators.withSuccess(LIST_RECORDER_HEADERS_BODY_EXAMPLE, MediaType.APPLICATION_JSON)
        );

    // When
    String recordHeaders = underTest.postForStringResponse(expectedUrl);

    then(recordHeaders).isEqualTo(LIST_RECORDER_HEADERS_BODY_EXAMPLE);
  }

  @Test(expected = ExternalSystemException.class)
  public void shouldThrowExternalSystemException() throws ExternalSystemException {

    // Given
    String expectedUrl = "http://cdc-osmh-repo:9091/v0/ListRecordHeaders?" +
        "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

    serverMock.expect(once(), requestTo(expectedUrl))
        .andExpect(method(GET))
        .andRespond(MockRestResponseCreators.withBadRequest());

    // When
    underTest.postForStringResponse(expectedUrl);

    // then exception should be thrown.
  }
}