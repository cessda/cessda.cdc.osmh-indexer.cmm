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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

    @Autowired
    public RecordHeaderParser(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private Document getRecordHeadersDocument(URI repoUrl) throws XMLParseException {
        try (var documentInputStream = httpClient.getInputStream(repoUrl)) {
            return OaiPmhHelpers.getSaxBuilder().build(documentInputStream);
        } catch (IOException | JDOMException e) {
            throw new XMLParseException(repoUrl, e);
        }
    }

    private List<RecordHeader> parseRecordHeadersFromDoc(Document doc) {
        var headers = DocElementParser.getElements(doc, OaiPmhConstants.HEADER_ELEMENT, OaiPmhConstants.OAI_NS);
        return headers.stream().map(this::parseRecordHeader).collect(Collectors.toList());
    }

    private Optional<String> parseResumptionToken(Document doc) {
        // OAI-PMH mandatory resumption tag in response.  Value can be empty to suggest end of list
        var resumptionToken = DocElementParser.getFirstElement(doc, OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT, OaiPmhConstants.OAI_NS);
        return resumptionToken.map(Element::getText).filter(t -> !t.isEmpty());
    }

    private RecordHeader parseRecordHeader(Element headerElement) {

        var recordHeaderBuilder = RecordHeader.builder();
        recordHeaderBuilder.recordType(RECORD_HEADER);

        // Check if the record is deleted
        if (headerElement.hasAttributes()) {
            var deletedAttribute = headerElement.getAttributeValue(OaiPmhConstants.STATUS_ATTR);
            recordHeaderBuilder.deleted(OaiPmhConstants.DELETED.equals(deletedAttribute));
        }

        // Parse the elements of the header
        var childElements = headerElement.getChildren();
        for (var child : childElements) {
            final String currentHeaderElementValue;
            switch (child.getName()) {
                case OaiPmhConstants.IDENTIFIER_ELEMENT:
                    currentHeaderElementValue = child.getText();
                    recordHeaderBuilder.identifier(currentHeaderElementValue);
                    break;
                case OaiPmhConstants.DATESTAMP_ELEMENT:
                    currentHeaderElementValue = child.getText();
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

    private static Optional<Record.Request> parseRequestElement(Repo repo, Document document) {
        return DocElementParser.getFirstElement(document, OaiPmhConstants.REQUEST_ELEMENT, OaiPmhConstants.OAI_NS).map(elem -> {
            var metadataPrefix = elem.getAttributeValue(OaiPmhConstants.METADATA_PREFIX_PARAM_KEY, OaiPmhConstants.OAI_NS);

            URI baseURL = null;
            try {
                baseURL = new URI(elem.getText());
            } catch (URISyntaxException e) {
                log.warn("{}: {} could not be parsed as a URL: {}", repo.getCode(), elem.getText(), e.toString());
            }

            return new Record.Request(baseURL, metadataPrefix);
        });
    }

    /**
     * Gets a stream of record headers from the specified repository.
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

            // Check if the document has an OAI element
            DocElementParser.getFirstElement(recordHeadersDocument, OaiPmhConstants.OAI_PMH, OaiPmhConstants.OAI_NS)
                .orElseThrow(() -> new HarvesterException("Missing OAI element"));

            // Exit if the response has an <error> element
            DocElementParser.validateResponse(recordHeadersDocument);

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

            return recordHeaders.stream().map(recordHeader -> new Record(recordHeader, new Record.Request(repo.getUrl(), repo.getPreferredMetadataParam()),null));
        } else if (repo.getPath() != null) {
            try {
                return Files.walk(repo.getPath()).filter(Files::isRegularFile)
                    .flatMap(p -> {
                        try (var inputStream = Files.newInputStream(p)) {
                            return Stream.of(OaiPmhHelpers.getSaxBuilder().build(inputStream));
                        } catch (IOException | JDOMException e) {
                            log.warn("[{}] Couldn't parse {}: {}", repo.getCode(), p, e.toString());
                            return Stream.empty();
                        }
                    }).flatMap(doc -> {
                        // Parse request element to retrieve the base URL of the repository
                        var request = parseRequestElement(repo, doc);

                        // Parse the record header from the document
                        var recordHeaders = parseRecordHeadersFromDoc(doc);

                        return recordHeaders.stream().map(recordHeader -> new Record(recordHeader, request.orElse(null), doc));
                    });
            } catch (IOException e) {
                throw new HarvesterException("Reading path failed: " + e, e);
            }
        } else {
            throw new IllegalArgumentException("Repo " + repo.getCode() + " has no URL or path defined");
        }
    }
}
