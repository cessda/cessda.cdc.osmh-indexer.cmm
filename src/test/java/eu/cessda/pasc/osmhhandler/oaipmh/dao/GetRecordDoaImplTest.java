package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.mock.data.CMMStudyTestData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Dao Spring Test class for retrieving record's xml
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GetRecordDoaImplTest {

  @Autowired
  private RestTemplate restTemplateWithNoSSLVerification;

  private MockRestServiceServer mockRestServiceServer;

  @Autowired
  private GetRecordDoa recordDoa;

  @Before
  public void setUp() {
    mockRestServiceServer = MockRestServiceServer.bindTo(restTemplateWithNoSSLVerification).build();
  }

  @Test
  public void shouldReturnXMLPayloadOfGivenRecordIdentifierFromGivenRepoURL() throws ExternalSystemException {

    // Given
    String expected_url = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=1683&metadataPrefix=ddi";
    mockRestServiceServer.expect(once(), requestTo(expected_url))
        .andExpect(method(GET))
        .andRespond(withSuccess(CMMStudyTestData.getDdiRecord1683(), MediaType.TEXT_XML));

    // When
    String responseXMLRecord = recordDoa.getRecordXML("https://oai.ukdataservice.ac.uk:8443/oai/provider", "1683");

    then(responseXMLRecord).isNotEmpty();
    then(responseXMLRecord).isNotBlank();
    then(responseXMLRecord).isEqualToIgnoringCase(CMMStudyTestData.getDdiRecord1683());
  }
}
