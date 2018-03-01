package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.dao.ListRecordHeadersDao;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
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
 * @author moses@doraventures.com
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
    String repoUrl = "www.my-fake-url.com";
    String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLResumptionEmpty();
    given(listRecordHeadersDao.listRecordHeaders(repoUrl)).willReturn(mockRecordHeadersXml);

    // When
    List<RecordHeader> recordHeaders = listRecordHeadersService.getRecordHeaders(repoUrl);

    then(recordHeaders).hasSize(3);
    then(recordHeaders).extracting("identifier").containsOnly("850229", "850232", "850235");
    then(recordHeaders).extracting("lastModified").containsOnly("2017-11-20T10:37:18Z");
    then(recordHeaders).extracting("type").containsOnly("Study");
  }

  @Test
  public void shouldRecursivelyLoopThroughTheOaiPMHResponseResumptionTokenToRetrieveReposCompleteListSize()
      throws CustomHandlerException {

    // Given
    String repoBaseUrl = "www.my-fake-url.com";
    String identifiersXML = RecordHeadersMock.getListIdentifiersXML();

    String resumptionToken01 = "0/3/7/ddi/null/2016-06-01/null";
    String repoUrlWithResumptionToken01 = appendListRecordResumptionToken(repoBaseUrl, resumptionToken01);
    String identifiersXMLWithResumption = RecordHeadersMock.getListIdentifiersXMLWithResumption();

    String resumptionToken02 = "3/6/7/ddi/null/2017-01-01/null";
    String repoUrlWithResumptionToken02 = appendListRecordResumptionToken(repoBaseUrl, resumptionToken02);
    String identifiersXMLWithResumptionLastList = RecordHeadersMock.getListIdentifiersXMLWithResumptionLastList();

    given(listRecordHeadersDao.listRecordHeaders(repoBaseUrl)).willReturn(identifiersXML);

    given(listRecordHeadersDao.listRecordHeadersResumption(repoUrlWithResumptionToken01))
        .willReturn(identifiersXMLWithResumption);

    given(listRecordHeadersDao.listRecordHeadersResumption(repoUrlWithResumptionToken02))
        .willReturn(identifiersXMLWithResumptionLastList);

    // When
    List<RecordHeader> recordHeaders = listRecordHeadersService.getRecordHeaders(repoBaseUrl);

    then(recordHeaders).hasSize(7);
    then(recordHeaders).extracting("identifier")
        .containsOnly("850229", "850232", "850235", "7753", "8300", "8301", "998");
    then(recordHeaders).extracting("lastModified")
        .containsOnly("2017-11-20T10:37:18Z", "2018-01-11T07:43:20Z", "2018-01-11T07:43:39Z");
    then(recordHeaders).extracting("type").containsOnly("Study");
  }

  @Test(expected = ExternalSystemException.class)
  public void shouldThrowExceptionForRecordHeadersInvalidMetadataToken() throws CustomHandlerException {

    // Given
    String repoUrl = "www.my-fake-url.com/WithInvalidToken";
    String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLWithInvalidMetadataTokenError();
    given(listRecordHeadersDao.listRecordHeaders(repoUrl)).willReturn(mockRecordHeadersXml);

    // When
    listRecordHeadersService.getRecordHeaders(repoUrl);
  }
}
