/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.harvester;

import eu.cessda.pasc.oci.exception.HarvesterException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.ListIdentifiersResponseValidator;
import eu.cessda.pasc.oci.parser.OaiPmhConstants;
import eu.cessda.pasc.oci.parser.OaiPmhHelpers;
import eu.cessda.pasc.oci.repository.DaoBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
class RecordHeaderParser {

    // Messaging and Exceptions
    private static final String RECORD_HEADER = "RecordHeader";
    private static final String STUDY = "Study";

    private final DaoBase daoBase;
    private final DocumentBuilderFactory builderFactory;

    @Autowired
    public RecordHeaderParser(DaoBase daoBase, DocumentBuilderFactory builderFactory) {
        this.daoBase = daoBase;
        this.builderFactory = builderFactory;
    }

    /**
     * Gets a list of record headers from the specified repository. The returned list is immutable.
     * <p>
     * This method uses the {@code ListIdentifiers} verb.
     *
     * @param repo the repository to retrieve records from
     * @return a list of record headers
     * @throws OaiPmhException    if the repository returns an OAI-PMH error
     * @throws XMLParseException  if the XML cannot be parsed, or if an IO error occurs
     * @throws HarvesterException if the OAI-PMH repository returns a XML document with no {@code <oai>} element
     */
    List<RecordHeader> getRecordHeaders(Repo repo) throws HarvesterException {

        log.debug("[{}] Parsing record headers.", repo.getCode());

        URI fullListRecordUrlPath = OaiPmhHelpers.appendListRecordParams(repo);
        var recordHeadersDocument = getRecordHeadersDocument(fullListRecordUrlPath);

        // We exit if the response has an <error> element
        ListIdentifiersResponseValidator.validateResponse(recordHeadersDocument);

        Optional<String> resumptionToken;
        var recordHeaders = new ArrayList<RecordHeader>();

        do {
            // Parse and add all found record headers
            recordHeaders.addAll(parseRecordHeadersFromDoc(recordHeadersDocument));

            // Check and loop when there is a resumptionToken
            resumptionToken = parseResumptionToken(recordHeadersDocument);
            if (resumptionToken.isPresent()) {
                URI repoUrlWithResumptionToken = OaiPmhHelpers.appendListRecordResumptionToken(repo.getUrl(), resumptionToken.get());
                log.trace("Looping for [{}].", repoUrlWithResumptionToken);
                recordHeadersDocument = getRecordHeadersDocument(repoUrlWithResumptionToken);
            }

        } while (resumptionToken.isPresent());

        log.debug("[{}] ParseRecordHeaders ended:  No more resumption tokens to process.", repo.getCode());

        return Collections.unmodifiableList(recordHeaders);
    }

    private Document getRecordHeadersDocument(URI repoUrl) throws XMLParseException {
        try (var documentInputStream = daoBase.getInputStream(repoUrl)) {
            return builderFactory.newDocumentBuilder().parse(documentInputStream);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new XMLParseException(repoUrl, e);
        }
    }

    private List<RecordHeader> parseRecordHeadersFromDoc(Document doc) {
        NodeList headers = doc.getElementsByTagName(OaiPmhConstants.HEADER_ELEMENT);

        return IntStream.range(0, headers.getLength()).mapToObj(headers::item)
            .map(this::parseRecordHeader)
            .collect(Collectors.toList());
    }

    private Optional<String> parseResumptionToken(Document doc) {
        // OAI-PMH mandatory resumption tag in response.  Value can be empty to suggest end of list
        NodeList resumptionToken = doc.getElementsByTagName(OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT);
        if (resumptionToken.getLength() > 0) {
            Node item = resumptionToken.item(0);
            if (!item.getTextContent().trim().isEmpty()) {
                return Optional.of(item.getTextContent());
            }
        }
        log.debug("Resumption token empty.");
        return Optional.empty();
    }

    private RecordHeader parseRecordHeader(Node headerNode) {

        var recordHeaderBuilder = RecordHeader.builder();
        recordHeaderBuilder.recordType(RECORD_HEADER);

        // Check if the record is deleted
        if (headerNode.hasAttributes()) {
            recordHeaderBuilder.deleted(OaiPmhConstants.DELETED.equals(headerNode.getAttributes().getNamedItem(OaiPmhConstants.STATUS_ATTR).getNodeValue()));
        }

        var headerElements = headerNode.getChildNodes();
        IntStream.range(0, headerElements.getLength()).mapToObj(headerElements::item).forEach(headerElement -> {
            final String currentHeaderElementValue;
            switch (headerElement.getNodeName()) {
                case OaiPmhConstants.IDENTIFIER_ELEMENT:
                    currentHeaderElementValue = headerElement.getTextContent();
                    recordHeaderBuilder.identifier(currentHeaderElementValue);
                    break;
                case OaiPmhConstants.DATESTAMP_ELEMENT:
                    currentHeaderElementValue = headerElement.getTextContent();
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
        });
        return recordHeaderBuilder.build();
    }
}
