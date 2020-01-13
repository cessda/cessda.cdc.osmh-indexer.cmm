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

package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
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

import static eu.cessda.pasc.osmhhandler.oaipmh.mock.data.RecordHeadersMock.getListIdentifiersXML;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ListRecordHeadersDaoImplTest {

  @Autowired
  RestTemplate restTemplateWithNoSSLVerification;

  @Autowired
  ListRecordHeadersDao listRecordHeadersDao;

  private MockRestServiceServer server;

  @Before
  public void setUp() {
    server = MockRestServiceServer.bindTo(restTemplateWithNoSSLVerification).build();
  }


  @Test
  public void shouldReturnXmlPayloadOfRecordHeadersFromRemoteRepository() throws CustomHandlerException {

    // Given
    String fullListRecordHeadersUrl= "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";

    server.expect(once(), requestTo(fullListRecordHeadersUrl))
        .andExpect(method(GET))
        .andRespond(withSuccess(getListIdentifiersXML(), MediaType.TEXT_XML));

    // When
    String recordHeadersXML = listRecordHeadersDao.listRecordHeaders(fullListRecordHeadersUrl);

    System.out.println("Actual: " + recordHeadersXML);

    then(recordHeadersXML).isNotNull();
    then(recordHeadersXML).isNotEmpty();
    then(recordHeadersXML).contains(getListIdentifiersXML());
  }

  @Test
  public void shouldReturnXmlPayloadOfGivenSpecSetRecordHeadersFromRemoteRepository() throws CustomHandlerException {

    // Given
    String fullListRecordHeadersUrl= "http://services.fsd.uta.fi/v0/oai?verb=ListIdentifiers&metadataPrefix=oai_ddi25&set=study_groups:energia";

    server.expect(once(), requestTo(fullListRecordHeadersUrl))
        .andExpect(method(GET))
        .andRespond(withSuccess(getListIdentifiersXML(), MediaType.TEXT_XML));

    // When
    String recordHeadersXML = listRecordHeadersDao.listRecordHeaders(fullListRecordHeadersUrl);

    System.out.println("Actual: " + recordHeadersXML);

    then(recordHeadersXML).isNotNull();
    then(recordHeadersXML).isNotEmpty();
    then(recordHeadersXML).contains(getListIdentifiersXML());
  }
}