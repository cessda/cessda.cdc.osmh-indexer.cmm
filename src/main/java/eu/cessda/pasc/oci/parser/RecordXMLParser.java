/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.LoggingConstants;
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.exception.InvalidUniverseException;
import eu.cessda.pasc.oci.exception.UnsupportedXMLNamespaceException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.Request;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.lifecycle.StudyUnit;
import eu.cessda.pasc.oci.models.oaipmh.Header;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.io.Files.getNameWithoutExtension;

/**
 * Service Class responsible for querying the repository to fetch remote records.
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class RecordXMLParser {

    private static final XPathExpression<Element> OAI_RECORD_EXPRESSION =  XPathFactory.instance().compile(OaiPmhConstants.RECORD_ELEMENT, Filters.element(), null, OaiPmhConstants.OAI_NS);
    private static final int MAX_FILE_SIZE_MB = 50;

    private final CMMStudyMapper cmmStudyMapper;
    private final StreamingLifecycleMapper streamingLifecycleMapper;
    private final XMLInputFactory xmlInputFactory;

    private Set<Namespace> suppressedNamespaceWarnings = null;

    public RecordXMLParser() throws IOException {
        this.cmmStudyMapper = new CMMStudyMapper();
        this.streamingLifecycleMapper = new StreamingLifecycleMapper();
        this.xmlInputFactory = XMLInputFactory.newFactory();
    }

    @Autowired
    public RecordXMLParser(CMMStudyMapper cmmStudyMapper, StreamingLifecycleMapper streamingLifecycleMapper, XMLInputFactory xmlInputFactory) {
        this.cmmStudyMapper = cmmStudyMapper;
        this.streamingLifecycleMapper = streamingLifecycleMapper;
        this.xmlInputFactory = xmlInputFactory;
    }

    /**
     * Load an XML document from the given path.
     * @param path the path to the XML document.
     */
    private Document getDocument(Path path, SeekableByteChannel channel) throws XMLParseException {
        // DOM Parser has a max size
        try {
            if (channel.size() > MAX_FILE_SIZE_MB * (1000 * 1000)) { // 50 MB
                throw new IOException("File size " + (channel.size() / (1000 * 1000)) + " MB is greater than " + MAX_FILE_SIZE_MB + " MB");
            }
            var inputStream = Channels.newInputStream(channel);
            return OaiPmhHelpers.getSaxBuilder().build(inputStream);
        } catch (IOException | JDOMException e) {
            throw new XMLParseException(path.toUri(), e);
        }
    }

    /**
     * Parse an OAI-PMH record header element into a {@link Header} object.
     *
     * @param headerElement the element to parse.
     * @return a record header.
     */
    @SuppressWarnings({"java:S131", "java:S1301"}) // There is no need to take action for other element names
    private Header parseRecordHeader(Element headerElement) {

        // Header values
        String identifier = null;
        String datestamp = null;
        List<String> setSpec = Collections.emptyList();
        boolean deleted = false;

        // Check if the record is deleted
        if (headerElement.hasAttributes()) {
            var deletedAttribute = headerElement.getAttributeValue(OaiPmhConstants.STATUS_ATTR);
            deleted = OaiPmhConstants.DELETED.equals(deletedAttribute);
        }

        // Parse the elements of the header
        var childElements = headerElement.getChildren();
        for (var child : childElements) {
            switch (child.getName()) {
                case OaiPmhConstants.IDENTIFIER -> identifier = child.getText();
                case OaiPmhConstants.DATESTAMP_ELEMENT -> datestamp = child.getText();
            }
        }
        return new Header(identifier, datestamp, setSpec, deleted);
    }

    /**
     * Gets a record from a remote repository.
     * @param repo the repository to retrieve the record from.
     * @param path the study to retrieve.
     * @return a {@link CMMStudy} representing the study.
     * @throws XMLParseException if an error occurred parsing the XML.
     */
    public List<CMMStudy> getRecord(Repo repo, Path path) throws XMLParseException {
        try (var byteChannel = Files.newByteChannel(path)) {
            return getRecordImpl(repo, path, byteChannel);
        } catch (IOException | XMLStreamException e) {
            throw new XMLParseException(path.toUri(), e);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private List<CMMStudy> getRecordImpl(Repo repo, Path path, SeekableByteChannel byteChannel) throws XMLParseException, XMLStreamException, IOException {

        // Fallback study number and last modified
        String fileName = getNameWithoutExtension(path.toString());
        String lastModified;
        try {
            // Set last modified to the file modified time if the header is not present or invalid
            lastModified = Files.getLastModifiedTime(path).toString();
        } catch (IOException _) {
            // Fallback - use the current time
            lastModified = OffsetDateTime.now(ZoneId.systemDefault()).toString();
        }

        // Try using the streaming parser
        try {
            var inputStream = StreamUtils.nonClosing(Channels.newInputStream(byteChannel));
            return parseRecordStreaming(repo, inputStream, fileName, lastModified);
        } catch (UnsupportedDDIException e) {
            log.debug("[{}]: {}: falling back to the DOM parser: {}", repo, path, e.toString());

            // Reset byte channel
            byteChannel.position(0);
        }

        // Retrieve document
        var document = getDocument(path, byteChannel);

        // Parse request element to retrieve the base URL of the repository
        var request = parseRecord(repo, path, document);

        var cmmStudies = new ArrayList<CMMStudy>();

        for (var recordObj : request.records()) {
            // Short-Circuit. We carry on to parse beyond the headers only if the record is active.
            if ((recordObj.recordHeader() != null && recordObj.recordHeader().deleted())) {
                // Marked as deleted, don't store
                continue;
            }
            try {
                var cmmStudy = mapDDIRecordToCMMStudy(repo, request, recordObj, fileName, lastModified);
                cmmStudies.add(cmmStudy);
            } catch (UnsupportedXMLNamespaceException e) {
                var recordIdentifier = recordObj.recordHeader() != null ? recordObj.recordHeader().identifier() : null;
                logUnsupportedNamespace(repo.code(), recordIdentifier, e);
            }
        }


        return cmmStudies;
    }

    /**
     * Parse a record using the streaming lifecycle parser
     *
     * @param repo the repository.
     * @param inputStream the input stream.
     * @param fileName the file name.
     * @return a list of studies.
     * @throws UnsupportedDDIException if the DDI is unsupported.
     * @throws XMLStreamException if an error occurs parsing the XML.
     */
    private List<CMMStudy> parseRecordStreaming(Repo repo, InputStream inputStream, String fileName, String fileLastModified) throws UnsupportedDDIException, XMLStreamException {

        var source = new StreamSource(inputStream);

        // Parse the document
        var parser = StreamingLifecycleParser.parseDocument(xmlInputFactory, source);

        /*
         * OAI-PMH Request
         */
        var uri = parser.getRequest().map(URI::create).orElse(null);

        /*
         * OAI-PMH Header
         */
        var recordHeader = parser.getRecordHeader();
        String studyNumber;
        String lastModified;
        if (recordHeader.isPresent()) {
            var h = recordHeader.get();
            studyNumber = h.identifier();
            lastModified = h.datestamp();
        } else {
            // Derive the study number from the file name
            studyNumber = fileName;
            lastModified = fileLastModified;
        }

        // Get top level study units
        var studyUnitList = parser.getObjectsByType(StudyUnit.class);

        // Get all components
        var allComponents = parser.getObjectsById();

        var cmmStudies = new ArrayList<CMMStudy>();

        for (var studyUnit : studyUnitList) {
            // Get study
            var cmmStudy = streamingLifecycleMapper.parseFragmentedStudy(
                    repo, uri, studyNumber, lastModified, allComponents, studyUnit
            );
            cmmStudies.add(cmmStudy);
        }

        return cmmStudies;
    }

    private void logUnsupportedNamespace(String code, String recordIdentifier, UnsupportedXMLNamespaceException e) {
        // Only initialise if required
        if (suppressedNamespaceWarnings == null) {
            suppressedNamespaceWarnings = ConcurrentHashMap.newKeySet();
        }
        if (suppressedNamespaceWarnings.add(e.getNamespace())) {
            // Only log on first encounter with this namespace
            log.atWarn().addKeyValue(LoggingConstants.STUDY_ID, recordIdentifier).log(
                "[{}]: {} cannot be parsed: {}. Further reports for this namespace have been suppressed.",
                 code, recordIdentifier, e.getMessage()
            );
        }
    }

    private Request parseRecord(Repo repo, Path path, Document document) {

        if (document.getRootElement().getNamespace().equals(OaiPmhConstants.OAI_NS)) {

            // Parse request element
            var elem = document.getRootElement().getChild(OaiPmhConstants.REQUEST, OaiPmhConstants.OAI_NS);

            var uriString = elem.getTextTrim();

            URI baseURL = null;
            try {
                baseURL = new URI(uriString);
            } catch (URISyntaxException e) {
                log.atWarn().addKeyValue(LoggingConstants.STUDY_ID, path).log(
                    "{}: {}: {} could not be parsed as a URL: {}",
                    repo.code(), path, uriString, e.getMessage()
                );
            }

            // Find all records, iterate through them
            var elements = OAI_RECORD_EXPRESSION.evaluate(document);

            var recordList = new ArrayList<Record>();

            for (var recordElement : elements) {
                var headerElement = recordElement.getChild("header", OaiPmhConstants.OAI_NS);
                var header = parseRecordHeader(headerElement);

                // Extract the metadata if present
                Document metadataDocument = null;
                var oaiMetadataElement = recordElement.getChild("metadata", OaiPmhConstants.OAI_NS);
                if (oaiMetadataElement != null && !oaiMetadataElement.getChildren().isEmpty()) {
                    // Detach the metadata from its document and attach it to a new document
                    var metadataElement = oaiMetadataElement.getChildren().getFirst();
                    metadataDocument = new Document(metadataElement.detach());
                }
                recordList.add(new Record(header, metadataDocument));
            }

            return new Request(baseURL, recordList);
        } else {
            // OAI response not at the root of the document, create a synthetic request
            return Request.createSyntheticRequest(document);
        }
    }

    /**
     * Convert a {@link Document} to a {@link CMMStudy}.
     *
     * @param repository the source repository.
     * @param request the request element from the OAI-PMH response.
     * @param recordObj   the {@link Record} to convert.
     * @param fileName the path of the source XML.
     */
    @SuppressWarnings("java:S3776")
    private CMMStudy mapDDIRecordToCMMStudy(Repo repository, Request request, Record recordObj, String fileName, String fileLastModified) {

        String studyNumber;
        String lastModified;
        if (recordObj.recordHeader() != null) {
            // A header was present, extract values
            studyNumber = recordObj.recordHeader().identifier();
            lastModified = recordObj.recordHeader().datestamp();
        } else {
            // Derive the study number from the file name
            studyNumber = fileName;

            // Set last modified to the file modified time if the header is not present or invalid
            lastModified = fileLastModified;
        }

        URI repositoryUrl;
        if (request.baseURL() != null) {
            repositoryUrl = request.baseURL();
        } else {
            repositoryUrl = repository.url();
        }

        try (var _ = MDC.putCloseable(LoggingConstants.STUDY_ID, studyNumber)) {
            CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();

            builder.studyNumber(studyNumber);
            builder.lastModified(lastModified);
            builder.repositoryUrl(repositoryUrl);

            // Check if metadata is present, parse if it is
            var metadata = recordObj.metadata();
            if (metadata != null) {
                // Get the XPaths required for the metadata
                var xPaths = XPaths.getXPaths(metadata.getRootElement().getNamespace());

                var defaultLangIsoCode = cmmStudyMapper.parseDefaultLanguage(metadata, repository, xPaths);
                builder.titleStudy(cmmStudyMapper.parseStudyTitle(metadata, xPaths, defaultLangIsoCode));

                var parseStudyUrlResults = cmmStudyMapper.parseStudyUrl(metadata, xPaths);
                builder.studyUrl(parseStudyUrlResults.results());

                var parseDataAccessURIResults = cmmStudyMapper.parseDataAccessURI(metadata, xPaths, defaultLangIsoCode);
                builder.dataAccessUrl(parseDataAccessURIResults.results());

                if (!parseStudyUrlResults.exceptions().isEmpty() || !parseDataAccessURIResults.exceptions().isEmpty()) {
                    // Copy exceptions into a single list
                    var combinedExceptions = new ArrayList<>(
                            parseStudyUrlResults.exceptions().size() + parseDataAccessURIResults.exceptions().size()
                    );
                    combinedExceptions.addAll(parseDataAccessURIResults.exceptions());
                    combinedExceptions.addAll(parseStudyUrlResults.exceptions());

                    log.warn("[{}] Some URLs in study {} couldn't be parsed: {}",
                            repository.code(), studyNumber, combinedExceptions
                    );
                }

                builder.abstractField(cmmStudyMapper.parseAbstract(metadata, xPaths, defaultLangIsoCode));
                builder.pidStudies(cmmStudyMapper.parsePidStudies(metadata, xPaths));
                builder.creators(cmmStudyMapper.parseCreator(metadata, xPaths));
                builder.dataAccess(cmmStudyMapper.parseDataAccess(metadata, xPaths, defaultLangIsoCode, repository.code()));
                builder.dataAccessFreeTexts(cmmStudyMapper.parseDataAccessFreeText(metadata, xPaths, defaultLangIsoCode));
                builder.classifications(cmmStudyMapper.parseClassifications(metadata, xPaths, defaultLangIsoCode));
                builder.keywords(cmmStudyMapper.parseKeywords(metadata, xPaths, defaultLangIsoCode));
                builder.typeOfTimeMethods(cmmStudyMapper.parseTypeOfTimeMethod(metadata, xPaths, defaultLangIsoCode));
                builder.studyAreaCountries(cmmStudyMapper.parseStudyAreaCountries(metadata, xPaths, defaultLangIsoCode));
                builder.unitTypes(cmmStudyMapper.parseUnitTypes(metadata, xPaths, defaultLangIsoCode));
                builder.publisher(cmmStudyMapper.parsePublisher(metadata, xPaths, defaultLangIsoCode));
                cmmStudyMapper.parseYrOfPublication(metadata, xPaths).ifPresent(builder::publicationYear);
                builder.fileLanguages(cmmStudyMapper.parseFileLanguages(metadata, xPaths));
                builder.typeOfSamplingProcedures(cmmStudyMapper.parseTypeOfSamplingProcedure(metadata, xPaths, defaultLangIsoCode));
                builder.typeOfModeOfCollections(cmmStudyMapper.parseTypeOfModeOfCollection(metadata, xPaths, defaultLangIsoCode));

                var dataCollectionPeriodResults = cmmStudyMapper.parseDataCollectionDates(metadata, xPaths);
                if (dataCollectionPeriodResults.exceptions() != null) {
                    // Parsing errors occurred, log here
                    log.warn("[{}] Some dates in study {} couldn't be parsed: {}",
                            repository.code(), studyNumber, dataCollectionPeriodResults.exceptions().toString()
                    );
                }
                dataCollectionPeriodResults.results().getStartDate().ifPresent(builder::dataCollectionPeriodStartdate);
                dataCollectionPeriodResults.results().getEndDate().ifPresent(builder::dataCollectionPeriodEnddate);
                dataCollectionPeriodResults.results().getDataCollectionYear().ifPresent(builder::dataCollectionYear);
                builder.dataCollectionFreeTexts(dataCollectionPeriodResults.results().getFreeTexts());

                try {
                    builder.universe(cmmStudyMapper.parseUniverses(metadata, xPaths, defaultLangIsoCode));
                } catch (InvalidUniverseException e) {
                    log.warn("[{}] Some universes in study {} couldn't be parsed: {}",
                            repository.code(), studyNumber, e.toString()
                    );
                }
                builder.relatedPublications(cmmStudyMapper.parseRelatedPublications(metadata, xPaths, defaultLangIsoCode));
                builder.funding(cmmStudyMapper.parseFunding(metadata, xPaths, defaultLangIsoCode));
                builder.dataKindFreeTexts(cmmStudyMapper.parseDataKindFreeText(metadata, xPaths, defaultLangIsoCode));
                builder.generalDataFormats(cmmStudyMapper.parseGeneralDataFormats(metadata, xPaths, defaultLangIsoCode));
                builder.series(cmmStudyMapper.parseSeries(metadata, xPaths, defaultLangIsoCode));
            }

            try {
                //should retrieve from header, if present
                builder.studyXmlSourceUrl(OaiPmhHelpers.buildGetStudyFullUrl(repository.url(), studyNumber, repository.preferredMetadataParam()));
            } catch (URISyntaxException e) {
                log.warn("[{}] Study URL for {} couldn't be parsed: {}", repository.code(), studyNumber, e.toString());
            }

            return builder.build();
        }
    }
}
