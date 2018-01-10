package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static eu.cessda.pasc.osmhhandler.oaipmh.mock.data.RecordHeadersMock.getListIdentifiersXML;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ListRecordHeadersDaoImplTest {

  @Autowired
  RestTemplate restTemplate;

  @Autowired
  ListRecordHeadersDao listRecordHeadersDao;

  private MockRestServiceServer server;

  @Before
  public void setUp() {
    server = MockRestServiceServer.bindTo(restTemplate).build();
  }


  @Test
  public void shouldReturnXmlPayloadOfRecordHeadersFromRemoteRepository() throws InternalSystemException {

    // Given
    String expected_url= "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";

    server.expect(once(), requestTo(expected_url))
        .andExpect(method(GET))
        .andRespond(withSuccess(getListIdentifiersXML(), MediaType.TEXT_XML));

    // When
    String recordHeadersXML = listRecordHeadersDao.listRecordHeaders("https://oai.ukdataservice.ac.uk:8443/oai/provider");

    System.out.println("Actual: " + recordHeadersXML);

    then(recordHeadersXML).isNotNull();
    then(recordHeadersXML).isNotEmpty();
    then(recordHeadersXML).contains(getListIdentifiersXML());
  }
}