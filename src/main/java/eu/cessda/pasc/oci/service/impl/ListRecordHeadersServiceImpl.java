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
import eu.cessda.pasc.oci.exception.InternalSystemException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.helpers.ListIdentifiersResponseValidator;
import eu.cessda.pasc.oci.helpers.OaiPmhConstants;
import eu.cessda.pasc.oci.helpers.OaiPmhHelpers;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service implementation to handle Listing Record Headers
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class ListRecordHeadersServiceImpl implements ListRecordHeadersService {

  // Messaging and Exceptions
  private static final String RECORD_HEADER = "RecordHeader";
  private static final String STUDY = "Study";

  private final DaoBase daoBase;
  private final HandlerConfigurationProperties config;
  private final DocumentBuilderFactory builderFactory;

  @Autowired
  public ListRecordHeadersServiceImpl(DaoBase daoBase, HandlerConfigurationProperties config, DocumentBuilderFactory builderFactory) {
    this.daoBase = daoBase;
    this.config = config;
    this.builderFactory = builderFactory;
  }

  @Override
  public List<RecordHeader> getRecordHeaders(Repo repo) throws InternalSystemException, OaiPmhException {

    URI fullListRecordUrlPath = OaiPmhHelpers.appendListRecordParams(repo);
    Document doc = getRecordHeadersDocument(fullListRecordUrlPath);

    // We exit if the response has an <error> element
    ListIdentifiersResponseValidator.validateResponse(doc);

    log.info("[{}] Parsing record headers.", repo.getName());
    List<RecordHeader> recordHeaders = retrieveRecordHeaders(doc, repo.getUrl());
    log.debug("[{}] ParseRecordHeaders ended:  No more resumption tokens to process.", repo.getName());

    int expectedRecordHeadersCount = getRecordHeadersCount(doc);
    if (expectedRecordHeadersCount != -1) {
      log.info("[{}] Retrieved [{}] of [{}] expected record headers.",
              repo.getName(), recordHeaders.size(), expectedRecordHeadersCount
      );
    } else {
      log.info("[{}] Retrieved [{}] record headers.", repo.getName(), recordHeaders.size());
    }
    return recordHeaders;
  }

  private int getRecordHeadersCount(Document doc) {

    NodeList resumptionToken = doc.getElementsByTagName(OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT);
    if (resumptionToken.getLength() > 0) {
      Node item = resumptionToken.item(0);
      NamedNodeMap attributes = item.getAttributes();
      for (int attributeIndex = 0; attributeIndex < attributes.getLength(); attributeIndex++) {
        if (attributes.item(attributeIndex).getNodeName().equalsIgnoreCase(OaiPmhConstants.COMPLETE_LIST_SIZE_ATTR)) {
          return Integer.parseInt(attributes.item(attributeIndex).getTextContent());
        }
      }
    }
    // Should not reach here for valid oai-pmh xml responses
    return -1;
  }

  private List<RecordHeader> retrieveRecordHeaders(Document document, URI baseRepoUrl) throws InternalSystemException {

    Optional<String> resumptionToken;
    var recordHeaders = new ArrayList<RecordHeader>();

    do {
      // Parse and add all found record headers
      recordHeaders.addAll(parseRecordHeadersFromDoc(document));

      // Check and loop when there is a resumptionToken
      resumptionToken = parseResumptionToken(document);
      if (resumptionToken.isPresent()) {
        URI repoUrlWithResumptionToken = OaiPmhHelpers.appendListRecordResumptionToken(baseRepoUrl, resumptionToken.get());
        log.trace("Looping for [{}].", repoUrlWithResumptionToken);
        document = getRecordHeadersDocument(repoUrlWithResumptionToken);
      }

    } while (resumptionToken.isPresent());
    return recordHeaders;
  }

  private Document getRecordHeadersDocument(URI repoUrl) throws InternalSystemException {
    try (InputStream resumedXMLDoc = daoBase.getInputStream(repoUrl)) {
      return getDocument(resumedXMLDoc, repoUrl);
    } catch (IOException e) {
      throw new InternalSystemException("IO error reading input stream", e);
    }
  }

  private List<RecordHeader> parseRecordHeadersFromDoc(Document doc) {
    NodeList headers = doc.getElementsByTagName(OaiPmhConstants.HEADER_ELEMENT);

    return IntStream.range(0, headers.getLength()).mapToObj(headerRowIndex -> headers.item(headerRowIndex).getChildNodes())
            .map(this::parseRecordHeader).collect(Collectors.toList());
  }

  private Optional<String> parseResumptionToken(Document doc) {
    // OAI-PMH mandatory resumption tag in response.  Value can be empty to suggest end of list
    NodeList resumptionToken = doc.getElementsByTagName(OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT);
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
        case OaiPmhConstants.IDENTIFIER_ELEMENT:
          currentHeaderElementValue = headerElements.item(headerElementIndex).getTextContent();
          recordHeaderBuilder.identifier(currentHeaderElementValue);
          break;
        case OaiPmhConstants.DATESTAMP_ELEMENT:
          currentHeaderElementValue = headerElements.item(headerElementIndex).getTextContent();
          recordHeaderBuilder.lastModified(currentHeaderElementValue);
          break;
        case OaiPmhConstants.SET_SPEC_ELEMENT:
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
