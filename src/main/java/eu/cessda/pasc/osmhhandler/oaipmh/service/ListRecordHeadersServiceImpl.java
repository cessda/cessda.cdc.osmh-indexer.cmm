package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.dao.ListRecordHeadersDao;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.response.RecordHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.RECORD_HEADER;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.STUDY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendListRecordResumptionToken;
import static eu.cessda.pasc.osmhhandler.oaipmh.models.response.RecordHeader.*;

/**
 * Service implementation to handle Listing Record Headers
 *
 * @author moses@doraventures.com
 */
@Service
@Slf4j
public class ListRecordHeadersServiceImpl implements ListRecordHeadersService {

  @Autowired
  ListRecordHeadersDao listRecordHeadersDao;

  @Autowired
  private DocumentBuilder builder;

  @Override
  public List<RecordHeader> getRecordHeaders(String baseRepoUrl) throws InternalSystemException {

    String recordHeadersXMLString = listRecordHeadersDao.listRecordHeaders(baseRepoUrl);
    Document doc = getDocument(recordHeadersXMLString);

    log.info("ParseRecordHeaders Start:  For {}.", baseRepoUrl);
    List<RecordHeader> recordHeaders = retrieveRecordHeaders(new ArrayList<>(), doc, baseRepoUrl);
    log.info("ParseRecordHeaders End:  No more resumption token to process for {}.", baseRepoUrl);
    log.info("ParseRecordHeaders retrieved {} of {} expected record headers.", recordHeaders.size(), getRecordHeadersCount(doc));
    return recordHeaders;
  }

  private int getRecordHeadersCount(Document doc) throws InternalSystemException {

    NodeList resumptionToken = doc.getElementsByTagName(RESUMPTION_TOKEN_ELEMENT);
    if (resumptionToken.getLength() > 0) {
      Node item = resumptionToken.item(0);
      NamedNodeMap attributes = item.getAttributes();
      for (int attributeIndex = 0; attributeIndex < attributes.getLength(); attributeIndex++) {
        if (attributes.item(attributeIndex).getNodeName().equalsIgnoreCase(COMPLETE_LIST_SIZE)) {
          return Integer.parseInt(attributes.item(attributeIndex).getTextContent());
        }
      }
    }
    // Should not reach here for valid oai-pmh xml responses
    throw new InternalSystemException("Unable to parse RecordHeadersCount from oai-pmh xml response.");
  }

  private List<RecordHeader> retrieveRecordHeaders(List<RecordHeader> recordHeaders, Document doc, String baseRepoUrl)
      throws InternalSystemException {

    parseRecordHeaders(recordHeaders, doc);
    String resumptionToken = parseResumptionToken(doc);
    if (!resumptionToken.isEmpty()) {
      String repoUrlWithResumptionToken = appendListRecordResumptionToken(baseRepoUrl, resumptionToken);
      String resumedXMLDoc = listRecordHeadersDao.listRecordHeadersResumption(repoUrlWithResumptionToken);
      log.info("Looping for [{}].", repoUrlWithResumptionToken);
      retrieveRecordHeaders(recordHeaders, getDocument(resumedXMLDoc), baseRepoUrl);
    }
    return recordHeaders;
  }

  private void parseRecordHeaders(List<RecordHeader> recordHeaders, Document doc) {
    NodeList headers = doc.getElementsByTagName(HEADER_ELEMENT);

    for (int headerRowIndex = 0; headerRowIndex < headers.getLength(); headerRowIndex++) {
      NodeList headerElements = headers.item(headerRowIndex).getChildNodes();
      RecordHeader recordHeader = parseRecordHeader(headerElements);
      recordHeaders.add(recordHeader);
    }
  }

  private String parseResumptionToken(Document doc) {
    // OAI-PMH mandatory resumption tag in response.  Value can be empty to suggest end of list

    NodeList resumptionToken = doc.getElementsByTagName(RESUMPTION_TOKEN_ELEMENT);
    if (resumptionToken.getLength() > 0) {
      Node item = resumptionToken.item(0);
      return item.getTextContent();
    }
    log.debug("ParseRecordHeaders:  Resumption token emtpy.");
    return "";
  }

  private RecordHeader parseRecordHeader(NodeList headerElements) {

    RecordHeaderBuilder recordHeaderBuilder = builder();
    recordHeaderBuilder.recordType(RECORD_HEADER);

    for (int headerElementIndex = 0; headerElementIndex < headerElements.getLength(); headerElementIndex++) {
      String headerElementName = headerElements.item(headerElementIndex).getNodeName();
      String currentHeaderElementValue;

      switch (headerElementName) {
        case IDENTIFIER_ELEMENT:
          currentHeaderElementValue = headerElements.item(headerElementIndex).getTextContent();
          recordHeaderBuilder.identifier(currentHeaderElementValue);
          break;
        case DATESTAMP_ELEMENT:
          currentHeaderElementValue = headerElements.item(headerElementIndex).getTextContent();
          recordHeaderBuilder.lastModified(currentHeaderElementValue);
          break;
        case SET_SPEC_ELEMENT:
          // FixMe: 1 There might be multiple SetSpec: https://www.oaforum.org/tutorial/english/page3.htm#section7
          // FixMe: 2 Depending on feedback from John Shepherdson set record type based on the SetSpec
          // For instance for UKDA - DataCollections = Study
          // For now we assume all setSpec are a Study as UKDA endpoint repo only holds Studies, SAME for others?
          recordHeaderBuilder.type(STUDY);
          break;
        default:
          // nothing to do
          break;
      }
    }
    return recordHeaderBuilder.build();
  }

  private Document getDocument(String docXMLString) throws InternalSystemException {
    try {
      InputStream is = new ByteArrayInputStream(docXMLString.getBytes(UTF_8));
      return builder.parse(is);
    } catch (SAXException | IOException e) {
      String msg = "Unable to parse repo response.";
      log.error(msg);
      throw new InternalSystemException(msg);
    }
  }
}
