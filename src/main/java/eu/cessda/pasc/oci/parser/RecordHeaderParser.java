/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.exception.HarvesterException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.http.HttpClient;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.input.DOMBuilder;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Service implementation to handle Listing Record Headers
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class RecordHeaderParser {

    // Messaging and Exceptions
    private static final String RECORD_HEADER = "RecordHeader";
    private static final String STUDY = "Study";

    private final HttpClient httpClient;
    private final DocumentBuilderFactory builderFactory;
    private final DOMBuilder domBuilder = new DOMBuilder();

    @Autowired
    public RecordHeaderParser(HttpClient httpClient, DocumentBuilderFactory builderFactory) {
        this.httpClient = httpClient;
        this.builderFactory = builderFactory;
    }

    private Document getRecordHeadersDocument(URI repoUrl) throws XMLParseException {
        try (var documentInputStream = httpClient.getInputStream(repoUrl)) {
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
            if (!item.getTextContent().isEmpty()) {
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
            String deletedAttribute = headerNode.getAttributes().getNamedItem(OaiPmhConstants.STATUS_ATTR).getNodeValue();
            recordHeaderBuilder.deleted(OaiPmhConstants.DELETED.equals(deletedAttribute));
        }

        // Parse the elements of the header
        var headerElements = headerNode.getChildNodes();
        for (int i = 0; i < headerElements.getLength(); i++) {
            var headerElement = headerElements.item(i);
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
        }
        return recordHeaderBuilder.build();
    }

    /**
     * Gets a list of record headers from the specified repository. The returned list is immutable.
     * <p>
     * This method uses the {@code ListIdentifiers} verb.
     *
     * @param repo the repository to retrieve records from.
     * @return a list of record headers.
     * @throws OaiPmhException    if the repository returns an OAI-PMH error.
     * @throws XMLParseException  if the XML cannot be parsed, or if an IO error occurs.
     * @throws HarvesterException if the OAI-PMH repository returns a XML document with no {@code <oai>} element.
     */
    @SuppressWarnings({"java:S2095", "resource"}) // This is closed by the calling method
    public Stream<Record> getRecordHeaders(Repo repo) throws HarvesterException {

        log.debug("[{}] Parsing record headers.", repo.getCode());

        if (repo.getUrl() != null) {

            URI fullListRecordUrlPath = OaiPmhHelpers.appendListRecordParams(repo);
            var recordHeadersDocument = getRecordHeadersDocument(fullListRecordUrlPath);

            // Check if the document has
            NodeList oAINode = recordHeadersDocument.getElementsByTagName(OaiPmhConstants.OAI_PMH);

            if (oAINode.getLength() == 0) {
                throw new HarvesterException("Missing OAI element");
            }

            // Exit if the response has an <error> element
            DocElementParser.validateResponse(domBuilder.build(recordHeadersDocument));

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

            return recordHeaders.stream().map(recordHeader -> new Record(recordHeader, null));
        } else if (repo.getPath() != null) {
            try {
                return Files.walk(repo.getPath()).filter(Files::isRegularFile)
                    .flatMap(p -> {
                        try (var inputStream = Files.newInputStream(p)) {
                            return Stream.of(builderFactory.newDocumentBuilder().parse(inputStream));
                        } catch (IOException | ParserConfigurationException | SAXException e) {
                            log.warn("[{}] Couldn't parse {}: {}", repo.getCode(), p, e.toString());
                            return Stream.empty();
                        }
                    }).flatMap(doc -> {
                        var recordHeaders = parseRecordHeadersFromDoc(doc);
                        return recordHeaders.stream().map(recordHeader -> new Record(recordHeader, domBuilder.build(doc)));
                    });
            } catch (IOException e) {
                log.error("[{}] Reading path failed: {}", repo.getCode(), e.toString());
            }
        } else {
            throw new IllegalArgumentException("Repo " + repo.getCode() + " has no URL or path defined");
        }
        return Stream.empty();
    }
}
