package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.dao.ListRecordHeadersDao;
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
  public void shouldReturnRecordHeadersForGivenRepo() throws InternalSystemException {

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
      throws InternalSystemException {

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




  //TODO: Should catch error in xml response and process accordingly...
  String exampleXMLWithError =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
          "<?xml-stylesheet type='text/xsl' href='oai2.xsl' ?>\n" +
          "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"\n" +
          " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
          " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" +
          " http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n" +
          "    <responseDate>2018-01-12T09:56:40Z</responseDate>\n" +
          "    <request verb=\"ListIdentifiers\" metadataPrefix=\"ddiff\" " +
          "from=\"2017-01-01\">https://oai.ukdataservice.ac.uk:8443/oai/provider</request>\n" +
          "    <error code=\"cannotDisseminateFormat\">This repository has no items available in " +
          "format &#39;ddiff&#39;</error>\n" +
          "</OAI-PMH>";

}
