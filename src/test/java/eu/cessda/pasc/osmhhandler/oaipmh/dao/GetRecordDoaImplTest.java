/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Dao Spring Test class for retrieving record's xml
 *
 * @author moses AT doraventures DOT com
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
  public void shouldReturnXMLPayloadOfGivenRecordIdentifierFromGivenRepoURL() throws CustomHandlerException {

    // Given
    String expected_url = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=1683&metadataPrefix=ddi";
    String ddiRecord1683 = CMMStudyTestData.getContent("xml/ddi_record_1683.xml");
    mockRestServiceServer.expect(once(), requestTo(expected_url))
        .andExpect(method(GET))
        .andRespond(withSuccess(ddiRecord1683, MediaType.TEXT_XML));

    // When
    String responseXMLRecord = recordDoa.getRecordXML(expected_url);

    then(responseXMLRecord).isNotEmpty();
    then(responseXMLRecord).isNotBlank();
    then(responseXMLRecord).isEqualToIgnoringCase(ddiRecord1683);
  }

  @Test(expected = ExternalSystemException.class)
  public void shouldThrowExceptionWhenRemoteServerResponseIsNotSuccessful() throws CustomHandlerException {

    // Given
    String expected_url = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=1683&metadataPrefix=ddi";
    mockRestServiceServer.expect(once(), requestTo(expected_url))
        .andExpect(method(GET))
        .andRespond(withBadRequest());

    // When
    recordDoa.getRecordXML(expected_url);
  }
}
