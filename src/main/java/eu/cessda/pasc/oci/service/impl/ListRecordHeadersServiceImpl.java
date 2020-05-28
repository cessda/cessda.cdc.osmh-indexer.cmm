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

package eu.cessda.pasc.oci.service.impl;

import eu.cessda.pasc.oci.configurations.HandlerConfigurationProperties;
import eu.cessda.pasc.oci.exception.CustomHandlerException;
import eu.cessda.pasc.oci.exception.InternalSystemException;
import eu.cessda.pasc.oci.helpers.ListIdentifiersResponseValidator;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.errors.ErrorStatus;
import eu.cessda.pasc.oci.repository.DaoBase;
import eu.cessda.pasc.oci.service.ListRecordHeadersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static eu.cessda.pasc.oci.helpers.HandlerConstants.RECORD_HEADER;
import static eu.cessda.pasc.oci.helpers.HandlerConstants.STUDY;
import static eu.cessda.pasc.oci.helpers.OaiPmhConstants.*;
import static eu.cessda.pasc.oci.helpers.OaiPmhHelpers.appendListRecordParams;
import static eu.cessda.pasc.oci.helpers.OaiPmhHelpers.appendListRecordResumptionToken;

/**
 * Service implementation to handle Listing Record Headers
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class ListRecordHeadersServiceImpl implements ListRecordHeadersService {

  private final DaoBase daoBase;
  private final HandlerConfigurationProperties config;
  private final DocumentBuilderFactory builderFactory;

  @Autowired
  public ListRecordHeadersServiceImpl(DaoBase daoBase, HandlerConfigurationProperties config,
                                      DocumentBuilderFactory builderFactory) {
    this.daoBase = daoBase;
    this.config = config;
    this.builderFactory = builderFactory;
  }

  @Override
  public List<RecordHeader> getRecordHeaders(URI baseRepoUrl) throws CustomHandlerException {

    URI fullListRecordUrlPath = appendListRecordParams(baseRepoUrl, config.getOaiPmh());
    Document doc;
    try (InputStream recordHeadersXMLString = daoBase.getInputStream(fullListRecordUrlPath)) {
      doc = getDocument(recordHeadersXMLString, fullListRecordUrlPath);
    } catch (IOException e) {
      throw new InternalSystemException("IO error reading input stream", e);
    }

    // We exit if the response has an <error> element
    ErrorStatus errorStatus = ListIdentifiersResponseValidator.validateResponse(doc);
    if (errorStatus.isHasError()) {
      log.debug("Returned response has error message [{}].", errorStatus.getMessage());
      throw new InternalSystemException(errorStatus.getMessage());
    }

    log.info("Parsing record headers for [{}].", baseRepoUrl);
    List<RecordHeader> recordHeaders = retrieveRecordHeaders(new ArrayList<>(), doc, baseRepoUrl);
    log.debug("ParseRecordHeaders Ended:  No more resumption token to process for repo with url [{}].", baseRepoUrl);

    int expectedRecordHeadersCount = getRecordHeadersCount(doc);
    if (expectedRecordHeadersCount != -1) {
      log.info("ParseRecordHeaders retrieved [{}] of [{}] expected record headers count for repo [{}].",
              recordHeaders.size(), expectedRecordHeadersCount, baseRepoUrl
      );
    } else {
      log.info("ParseRecordHeaders retrieved [{}] record headers for [{}].", recordHeaders.size(), baseRepoUrl);
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

  private List<RecordHeader> retrieveRecordHeaders(List<RecordHeader> recordHeaders, Document doc, URI baseRepoUrl)
          throws CustomHandlerException {

    parseRecordHeadersFromDoc(recordHeaders, doc);

    // Now lets check and loop when there is a resumptionToken
    Optional<String> resumptionToken = parseResumptionToken(doc);
    if (resumptionToken.isPresent()) {
      URI repoUrlWithResumptionToken = appendListRecordResumptionToken(baseRepoUrl, resumptionToken.get());
      Document document;
      try (InputStream resumedXMLDoc = daoBase.getInputStream(repoUrlWithResumptionToken)) {
        log.trace("Looping for [{}].", repoUrlWithResumptionToken);
        document = getDocument(resumedXMLDoc, repoUrlWithResumptionToken);
      } catch (IOException e) {
        throw new InternalSystemException("IO error reading input stream", e);
      }
      retrieveRecordHeaders(recordHeaders, document, baseRepoUrl);
    }
    return recordHeaders;
  }

  private void parseRecordHeadersFromDoc(List<RecordHeader> recordHeaders, Document doc) {
    NodeList headers = doc.getElementsByTagName(HEADER_ELEMENT);

    IntStream.range(0, headers.getLength()).mapToObj(headerRowIndex -> headers.item(headerRowIndex).getChildNodes())
            .map(this::parseRecordHeader).forEach(recordHeaders::add);
  }

  private Optional<String> parseResumptionToken(Document doc) {
    // OAI-PMH mandatory resumption tag in response.  Value can be empty to suggest end of list
    NodeList resumptionToken = doc.getElementsByTagName(RESUMPTION_TOKEN_ELEMENT);
    if (resumptionToken.getLength() > 0) {
      Node item = resumptionToken.item(0);
      if (!item.getTextContent().trim().isEmpty()) {
        return Optional.ofNullable(item.getTextContent());
      }
    }
    log.debug("ParseRecordHeaders: Resumption token empty.");
    return Optional.empty();
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

  private Document getDocument(InputStream documentStream, URI fullListRecordUrlPath) throws InternalSystemException {
    try {
      return builderFactory.newDocumentBuilder().parse(documentStream);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      String msg = String.format("Unable to parse repo RecordHeader response bytes for path [%s].", fullListRecordUrlPath);
      throw new InternalSystemException(msg, e);
    }
  }
}
