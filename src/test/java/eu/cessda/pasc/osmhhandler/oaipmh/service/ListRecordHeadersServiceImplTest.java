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
package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.dao.ListRecordHeadersDao;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.mock.data.RecordHeadersMock;
import eu.cessda.pasc.osmhhandler.oaipmh.models.response.RecordHeader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendListRecordResumptionToken;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;


/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ListRecordHeadersServiceImplTest {

  @MockBean
  ListRecordHeadersDao listRecordHeadersDao;

  @Autowired
  ListRecordHeadersService listRecordHeadersService;

  @Test
  public void shouldReturnRecordHeadersForGivenRepo() throws CustomHandlerException {

    // Given
    String repoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider";
    String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";
    String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLResumptionEmpty();
    given(listRecordHeadersDao.listRecordHeaders(fullListRecordRepoUrl)).willReturn(mockRecordHeadersXml);

    // When
    List<RecordHeader> recordHeaders = listRecordHeadersService.getRecordHeaders(repoUrl);

    then(recordHeaders).hasSize(3);
    then(recordHeaders).extracting("identifier").containsOnly("850229", "850232", "850235");
    then(recordHeaders).extracting("lastModified").containsOnly("2017-11-20T10:37:18Z");
    then(recordHeaders).extracting("type").containsOnly("Study");
  }

  @Test(expected = InternalSystemException.class)
  public void shouldThrowWhenRequestForHeaderFails() throws CustomHandlerException {

    // Given
    String repoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider";
    String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";
    String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLResumptionTokenNotMockedForInvalid();
    given(listRecordHeadersDao.listRecordHeaders(fullListRecordRepoUrl)).willReturn(mockRecordHeadersXml);

    // When
    listRecordHeadersService.getRecordHeaders(repoUrl);
  }

  @Test
  public void shouldRecursivelyLoopThroughTheOaiPMHResponseResumptionTokenToRetrieveReposCompleteListSize()
      throws CustomHandlerException {

    // Given
    String baseRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider";
    String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";
    String identifiersXML = RecordHeadersMock.getListIdentifiersXML();

    String resumptionToken01 = "0/3/7/ddi/null/2016-06-01/null";
    String repoUrlWithResumptionToken01 = appendListRecordResumptionToken(baseRepoUrl, resumptionToken01);
    String identifiersXMLWithResumption = RecordHeadersMock.getListIdentifiersXMLWithResumption();

    String resumptionToken02 = "3/6/7/ddi/null/2017-01-01/null";
    String repoUrlWithResumptionToken02 = appendListRecordResumptionToken(baseRepoUrl, resumptionToken02);
    String identifiersXMLWithResumptionLastList = RecordHeadersMock.getListIdentifiersXMLWithResumptionLastList();

    given(listRecordHeadersDao.listRecordHeaders(fullListRecordRepoUrl)).willReturn(identifiersXML);

    given(listRecordHeadersDao.listRecordHeadersResumption(repoUrlWithResumptionToken01))
        .willReturn(identifiersXMLWithResumption);

    given(listRecordHeadersDao.listRecordHeadersResumption(repoUrlWithResumptionToken02))
        .willReturn(identifiersXMLWithResumptionLastList);

    // When
    List<RecordHeader> recordHeaders = listRecordHeadersService.getRecordHeaders(baseRepoUrl);

    then(recordHeaders).hasSize(7);
    then(recordHeaders).extracting("identifier")
        .containsOnly("850229", "850232", "850235", "7753", "8300", "8301", "998");
    then(recordHeaders).extracting("lastModified")
        .containsOnly("2017-11-20T10:37:18Z", "2018-01-11T07:43:20Z", "2018-01-11T07:43:39Z");
    then(recordHeaders).extracting("type").containsOnly("Study");
  }

  @Test(expected = InternalSystemException.class)
  public void shouldThrowExceptionForRecordHeadersInvalidMetadataToken() throws CustomHandlerException {

    // Given
    String repoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider";
    String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";

    String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLWithInvalidMetadataTokenError();
    given(listRecordHeadersDao.listRecordHeaders(fullListRecordRepoUrl)).willReturn(mockRecordHeadersXml);

    // When
    listRecordHeadersService.getRecordHeaders(repoUrl);
  }
}
