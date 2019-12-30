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
import eu.cessda.pasc.osmhhandler.oaipmh.helpers.ListIdentifiersResponseValidator;
import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorStatus;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.RECORD_HEADER;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.STUDY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.COMPLETE_LIST_SIZE_ATTR;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.DATESTAMP_ELEMENT;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.HEADER_ELEMENT;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.IDENTIFIER_ELEMENT;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.SET_SPEC_ELEMENT;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendListRecordResumptionToken;

/**
 * Service implementation to handle Listing Record Headers
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class ListRecordHeadersServiceImpl implements ListRecordHeadersService {

  private final ListRecordHeadersDao listRecordHeadersDao;

  private final DocumentBuilder builder;

  @Autowired
  public ListRecordHeadersServiceImpl(ListRecordHeadersDao listRecordHeadersDao, DocumentBuilder builder) {
    this.listRecordHeadersDao = listRecordHeadersDao;
    this.builder = builder;
  }

  @Override
  public List<RecordHeader> getRecordHeaders(String baseRepoUrl) throws CustomHandlerException {

    String recordHeadersXMLString = listRecordHeadersDao.listRecordHeaders(baseRepoUrl);
    Document doc = getDocument(recordHeadersXMLString);

    // We exit if the response has an <error> element
    ErrorStatus errorStatus = ListIdentifiersResponseValidator.validateResponse(doc);
    if (errorStatus.isHasError()) {
      log.debug("Returned response has error message [{}].", errorStatus.getMessage());
      throw new InternalSystemException(errorStatus.getMessage());
    }

    log.info("ParseRecordHeaders Started:  For [{}].", baseRepoUrl);
    List<RecordHeader> recordHeaders = retrieveRecordHeaders(new ArrayList<>(), doc, baseRepoUrl);
    log.info("ParseRecordHeaders Ended:  No more resumption token to process for repo with url [{}].", baseRepoUrl);
    int expectedRecordHeadersCount = getRecordHeadersCount(doc);
    if (expectedRecordHeadersCount != -1) {
      log.info("ParseRecordHeaders retrieved [{}] of [{}] expected record headers count for repo [{}].",
          recordHeaders.size(),
          expectedRecordHeadersCount,
          baseRepoUrl);
    } else if (expectedRecordHeadersCount == -1) {
      log.error("Unable to parse Record header's count from response. Retrieved record content [{}] for SP baseUrl [{}]",
          doc, baseRepoUrl);
    } else {
      log.info("ParseRecordHeaders retrieved [{}] record headers for repo [{}].", recordHeaders.size(), baseRepoUrl);
    }
    return recordHeaders;
  }

  private int getRecordHeadersCount(Document doc) {

    NodeList resumptionToken = doc.getElementsByTagName(RESUMPTION_TOKEN_ELEMENT);
    if (resumptionToken.getLength() > 0) {
      Node item = resumptionToken.item(0);
      NamedNodeMap attributes = item.getAttributes();
      for (int attributeIndex = 0; attributeIndex < attributes.getLength(); attributeIndex++) {
        if (attributes.item(attributeIndex).getNodeName().equalsIgnoreCase(COMPLETE_LIST_SIZE_ATTR)) {
          return Integer.parseInt(attributes.item(attributeIndex).getTextContent());
        }
      }
    }
    // Should not reach here for valid oai-pmh xml responses
    return -1;
  }

  private List<RecordHeader> retrieveRecordHeaders(List<RecordHeader> recordHeaders, Document doc, String baseRepoUrl)
      throws CustomHandlerException {

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

    RecordHeader.RecordHeaderBuilder recordHeaderBuilder = RecordHeader.builder();
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
          // Note:
          // 1 There might be multiple SetSpec: https://www.oaforum.org/tutorial/english/page3.htm#section7
          // 2 Depending on feedback from John Shepherdson set record type based on the SetSpec
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
      InputStream is = new ByteArrayInputStream(docXMLString.getBytes(StandardCharsets.UTF_8));
      return builder.parse(is);
    } catch (NullPointerException | SAXException | IOException e) {
      String msg = "Unable to parse repo RecordHeader response bytes.";
      log.error(msg);
      log.debug(msg + ". Document content [{}].", docXMLString);
      throw new InternalSystemException(msg);
    }
  }
}
