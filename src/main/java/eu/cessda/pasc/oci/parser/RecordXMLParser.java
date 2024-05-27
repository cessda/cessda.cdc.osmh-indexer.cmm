/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.Request;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.TermVocabAttributes;
import eu.cessda.pasc.oci.models.cmmstudy.VocabAttributes;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.io.Files.getNameWithoutExtension;
import static net.logstash.logback.argument.StructuredArguments.value;

/**
 * Service Class responsible for querying the repository to fetch remote records.
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class RecordXMLParser {

    private static final XPathExpression<Element> OAI_RECORD_EXPRESSION = XPathFactory.instance().compile(OaiPmhConstants.RECORD_ELEMENT, Filters.element(), null, OaiPmhConstants.OAI_NS);

    private final CMMStudyMapper cmmStudyMapper;
    private Set<Namespace> suppressedNamespaceWarnings = null;

    @Autowired
    public RecordXMLParser(CMMStudyMapper cmmStudyMapper) {
        this.cmmStudyMapper = cmmStudyMapper;
    }

    /**
     * Load an XML document from the given path.
     * @param path the path to the XML document.
     * @throws XMLParseException if the document could not be parsed, or an IO error occurred.
     */
    private Document getDocument(Path path) throws XMLParseException {
        try (var inputStream = Files.newInputStream(path)) {
            return OaiPmhHelpers.getSaxBuilder().build(inputStream);
        } catch (IOException | JDOMException e) {
            throw new XMLParseException(e);
        }
    }

    /**
     * Parse an OAI-PMH record header element into a {@link RecordHeader} object.
     *
     * @param headerElement the element to parse.
     * @return a record header.
     */
    @SuppressWarnings({"java:S131", "java:S1301"}) // There is no need to take action for other element names
    private RecordHeader parseRecordHeader(Element headerElement) {

        var recordHeaderBuilder = RecordHeader.builder();

        // Check if the record is deleted
        if (headerElement.hasAttributes()) {
            var deletedAttribute = headerElement.getAttributeValue(OaiPmhConstants.STATUS_ATTR);
            recordHeaderBuilder.deleted(OaiPmhConstants.DELETED.equals(deletedAttribute));
        }

        // Parse the elements of the header
        var childElements = headerElement.getChildren();
        for (var child : childElements) {
            switch (child.getName()) {
                case OaiPmhConstants.IDENTIFIER -> {
                    String identifier = child.getText();
                    recordHeaderBuilder.identifier(identifier);
                }
                case OaiPmhConstants.DATESTAMP_ELEMENT -> {
                    String lastModified = child.getText();
                    recordHeaderBuilder.lastModified(lastModified);
                }
            }
        }
        return recordHeaderBuilder.build();
    }

    /**
     * Gets a record from a remote repository.
     * @param repo the repository to retrieve the record from.
     * @param path the study to retrieve.
     * @return a {@link CMMStudy} representing the study.
     * @throws XMLParseException if an error occurred parsing the XML.
     */
    public List<CMMStudy> getRecord(Repo repo, Path path) throws XMLParseException {

        // Retrieve
        var document = getDocument(path);

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
                var cmmStudy = mapDDIRecordToCMMStudy(repo, request, recordObj, path);
                cmmStudies.add(cmmStudy);
            } catch (UnsupportedXMLNamespaceException e) {
                var recordIdentifier = recordObj.recordHeader() != null ? recordObj.recordHeader().identifier() : null;
                logUnsupportedNamespace(repo.code(), recordIdentifier, e);
            }
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
            log.warn("[{}]: {} cannot be parsed: {}. Further reports for this namespace have been suppressed.",
                value(LoggingConstants.REPO_NAME, code),
                value(LoggingConstants.STUDY_ID, recordIdentifier),
                e.getMessage()
            );
        }
    }

    private Request parseRecord(Repo repo, Path path, Document document) {

        if (document.getRootElement().getNamespace().equals(OaiPmhConstants.OAI_NS)) {

            // Parse request element
            var elem = document.getRootElement().getChild("request", OaiPmhConstants.OAI_NS);

            var uriString = elem.getTextTrim();

            URI baseURL = null;
            try {
                baseURL = new URI(uriString);
            } catch (URISyntaxException e) {
                log.warn("{}: {}: {} could not be parsed as a URL: {}",
                    value(LoggingConstants.REPO_NAME, repo.code()),
                    value(LoggingConstants.STUDY_ID, path),
                    uriString,
                    e.toString()
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
     * @param path the path of the source XML.
     */
    @SuppressWarnings({"java:S3776", "UnstableApiUsage"})
    private CMMStudy mapDDIRecordToCMMStudy(Repo repository, Request request, Record recordObj, Path path) {

        CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();

        String studyNumber;
        String lastModified;
        if (recordObj.recordHeader() != null) {
            // A header was present, extract values
            studyNumber = recordObj.recordHeader().identifier();
            lastModified = recordObj.recordHeader().lastModified();
        } else {
            // Derive the study number from the file name
            studyNumber = getNameWithoutExtension(path.toString());
            try {
                // Set last modified to the file modified time if the header is not present or invalid
                lastModified = Files.getLastModifiedTime(path).toString();
            } catch (IOException e) {
                // Fallback - use the current time
                lastModified = OffsetDateTime.now(ZoneId.systemDefault()).toString();
            }
        }

        builder.studyNumber(studyNumber);
        builder.lastModified(lastModified);

        // Check if metadata is present, parse if it is
        var metadata = recordObj.metadata();
        if (metadata != null) {
            // Get the XPaths required for the metadata
            var xPaths = XPaths.getXPaths(metadata.getRootElement().getNamespace());

            var defaultLangIsoCode = cmmStudyMapper.parseDefaultLanguage(metadata, repository, xPaths);
            builder.titleStudy(cmmStudyMapper.parseStudyTitle(metadata, xPaths, defaultLangIsoCode));

            var parseStudyUrlResults = cmmStudyMapper.parseStudyUrl(metadata, xPaths, defaultLangIsoCode);
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
                    value(LoggingConstants.REPO_NAME, repository.code()),
                    value(LoggingConstants.STUDY_ID, studyNumber),
                    combinedExceptions
                );
            }

            builder.abstractField(cmmStudyMapper.parseAbstract(metadata, xPaths, defaultLangIsoCode));
            builder.pidStudies(cmmStudyMapper.parsePidStudies(metadata, xPaths, defaultLangIsoCode));
            builder.creators(cmmStudyMapper.parseCreator(metadata, xPaths, defaultLangIsoCode));
            builder.dataAccessFreeTexts(cmmStudyMapper.parseDataAccessFreeText(metadata, xPaths, defaultLangIsoCode));
            builder.classifications(cmmStudyMapper.parseClassifications(metadata, xPaths, defaultLangIsoCode));
            builder.keywords(cmmStudyMapper.parseKeywords(metadata, xPaths, defaultLangIsoCode));
            builder.typeOfTimeMethods(cmmStudyMapper.parseTypeOfTimeMethod(metadata, xPaths, defaultLangIsoCode));
            builder.studyAreaCountries(cmmStudyMapper.parseStudyAreaCountries(metadata, xPaths, defaultLangIsoCode));
            builder.unitTypes(cmmStudyMapper.parseUnitTypes(metadata, xPaths, defaultLangIsoCode));
            builder.publisher(cmmStudyMapper.parsePublisher(metadata, xPaths, defaultLangIsoCode));
            cmmStudyMapper.parseYrOfPublication(metadata, xPaths).ifPresent(builder::publicationYear);
            builder.fileLanguages(cmmStudyMapper.parseFileLanguages(metadata, xPaths));
            var samplingProcedures = cmmStudyMapper.parseTypeOfSamplingProcedure(metadata, xPaths, defaultLangIsoCode);
            var result = extractSamplingProcedures(samplingProcedures);
            builder.typeOfSamplingProcedures(result.vocabAttributes());
            builder.samplingProcedureFreeTexts(result.terms());
            builder.typeOfModeOfCollections(cmmStudyMapper.parseTypeOfModeOfCollection(metadata, xPaths, defaultLangIsoCode));

            var dataCollectionPeriodResults = cmmStudyMapper.parseDataCollectionDates(metadata, xPaths, defaultLangIsoCode);
            if (!dataCollectionPeriodResults.exceptions().isEmpty()) {
                // Parsing errors occurred, log here
                log.warn("[{}] Some dates in study {} couldn't be parsed: {}",
                    value(LoggingConstants.REPO_NAME, repository.code()),
                    value(LoggingConstants.STUDY_ID, studyNumber),
                    dataCollectionPeriodResults.exceptions()
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
                    value(LoggingConstants.REPO_NAME, repository.code()),
                    value(LoggingConstants.STUDY_ID, studyNumber),
                    e.toString()
                );
            }
            builder.relatedPublications(cmmStudyMapper.parseRelatedPublications(metadata, xPaths, defaultLangIsoCode));
            builder.funding(cmmStudyMapper.parseFunding(metadata, xPaths, defaultLangIsoCode));
        }

        URI repositoryUrl;
        if (request.baseURL() != null) {
            repositoryUrl = request.baseURL();
        } else {
            repositoryUrl = repository.url();
        }
        builder.repositoryUrl(repositoryUrl);

        try {
            //should retrieve from header, if present
            builder.studyXmlSourceUrl(OaiPmhHelpers.buildGetStudyFullUrl(repository.url(), studyNumber, repository.preferredMetadataParam()));
        } catch (URISyntaxException e) {
            log.warn("[{}] Study URL for {} couldn't be parsed: {}",
                value(LoggingConstants.REPO_NAME, repository.code()),
                value(LoggingConstants.STUDY_ID, studyNumber),
                e.toString()
            );
        }

        return builder.build();
    }

    @NonNull
    private static SamplingProcedures extractSamplingProcedures(Map<String, List<TermVocabAttributes>> samplingProcedures) {
        var vocab = new HashMap<String, List<VocabAttributes>>();
        var sampTerm = new HashMap<String, List<String>>();

        samplingProcedures.forEach((lang, termVocabAttributesList) -> {
            // Create separate lists
            var va = new ArrayList<VocabAttributes>(termVocabAttributesList.size());
            var ft = new ArrayList<String>(termVocabAttributesList.size());

            // Copy content to the lists
            for (var vocAttr : termVocabAttributesList) {
                if (!vocAttr.id().isEmpty()) {
                    va.add(new VocabAttributes(vocAttr.vocab(), vocAttr.vocabUri(), vocAttr.id()));
                }
                if (!vocAttr.term().isEmpty()) {
                    ft.add(vocAttr.term());
                }
            }

            // Put the lists with the separated content in the maps
            if (!va.isEmpty()) {
                vocab.put(lang, va);
            }
            if (!ft.isEmpty()) {
                sampTerm.put(lang, ft);
            }
        });

        return new SamplingProcedures(vocab, sampTerm);
    }

    private record SamplingProcedures(
        Map<String, List<VocabAttributes>> vocabAttributes,
        Map<String, List<String>> terms
    ) {
    }
}
