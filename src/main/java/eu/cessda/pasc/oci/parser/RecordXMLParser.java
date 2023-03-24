/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.exception.InvalidUniverseException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.Request;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
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
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.io.Files.getNameWithoutExtension;

/**
 * Service Class responsible for querying the repository to fetch remote records.
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class RecordXMLParser {

    private final CMMStudyMapper cmmStudyMapper;

    @Autowired
    public RecordXMLParser(CMMStudyMapper cmmStudyMapper) {
        this.cmmStudyMapper = cmmStudyMapper;
    }

    // Messaging and Exceptions
    private static final String RECORD_HEADER = "RecordHeader";
    private static final String STUDY = "Study";

    private Document getDocument(Path path) throws XMLParseException {
        try (var inputStream = Files.newInputStream(path)) {
            return OaiPmhHelpers.getSaxBuilder().build(inputStream);
        } catch (IOException | JDOMException e) {
            throw new XMLParseException(e);
        }
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
                case OaiPmhConstants.IDENTIFIER_ELEMENT -> {
                    currentHeaderElementValue = child.getText();
                    recordHeaderBuilder.identifier(currentHeaderElementValue);
                }
                case OaiPmhConstants.DATESTAMP_ELEMENT -> {
                    currentHeaderElementValue = child.getText();
                    recordHeaderBuilder.lastModified(currentHeaderElementValue);
                }
                case OaiPmhConstants.SET_SPEC_ELEMENT ->
                    // Note:
                    // 1 There might be multiple SetSpec: https://www.oaforum.org/tutorial/english/page3.htm#section7
                    // 2 Depending on feedback from John Shepherdson set record type based on the SetSpec
                    // For instance for UKDA - DataCollections = Study
                    // For now we assume all setSpec are a Study as UKDA endpoint repo only holds Studies, SAME for others?
                    recordHeaderBuilder.type(STUDY);
                default -> {
                    // nothing to do
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

        for (var record : request.records()) {
            // Short-Circuit. We carry on to parse beyond the headers only if the record is active.
            if ((record.recordHeader() != null && record.recordHeader().isDeleted())) {
                // Marked as deleted, don't store
                continue;
            }
            cmmStudies.add(mapDDIRecordToCMMStudy(repo, request, record, path));
        }

        return cmmStudies;
    }

    private Request parseRecord(Repo repo, Path path, Document document) {

        if (document.getRootElement().getNamespace().equals(OaiPmhConstants.OAI_NS)) {

            // Parse request element
            var elem = document.getRootElement().getChild("request", OaiPmhConstants.OAI_NS);

            URI baseURL = null;
            try {
                baseURL = new URI(elem.getTextTrim());
            } catch (URISyntaxException e) {
                log.warn("{}: {}: {} could not be parsed as a URL: {}", repo.getCode(), path, elem.getText(), e.toString());
            }
            
            // Find all records, iterate through them
            var elements = DocElementParser.getElements(document, OaiPmhConstants.RECORD_ELEMENT, OaiPmhConstants.OAI_NS);

            var recordList = new ArrayList<Record>();
            
            for (var recordElement : elements) {
                var headerElement = recordElement.getChild("header", OaiPmhConstants.OAI_NS);
                var header = parseRecordHeader(headerElement);

                // Extract the metadata if present
                Document metadataDocument = null;
                var oaiMetadataElement = recordElement.getChild("metadata", OaiPmhConstants.OAI_NS);
                if (oaiMetadataElement != null && !oaiMetadataElement.getChildren().isEmpty()) {
                    // Detach the metadata from its document and attach it to a new document
                    var metadataElement = oaiMetadataElement.getChildren().get(0);
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
     * @param record   the {@link Record} to convert.
     * @param path the path of the source XML.
     */
    @SuppressWarnings("UnstableApiUsage")
    private CMMStudy mapDDIRecordToCMMStudy(Repo repository, Request request, Record record, Path path) {

        CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();

        String studyNumber;
        String lastModified;
        if (record.recordHeader() != null) {
            // A header was present, extract values
            studyNumber = record.recordHeader().getIdentifier();
            lastModified = record.recordHeader().getLastModified();
        } else {
            // Derive the study number from the file name, set last modified to the current time
            studyNumber = getNameWithoutExtension(path.toString());
            lastModified = LocalDateTime.now().toString();
        }

        builder.studyNumber(studyNumber);
        builder.lastModified(lastModified);

        // Check if metadata is present, parse if it is
        var metadata = record.metadata();
        if (metadata != null) {
            // Get the XPaths required for the metadata
            var xPaths = XPaths.getXPaths(metadata.getRootElement().getNamespace());

            var defaultLangIsoCode = cmmStudyMapper.parseDefaultLanguage(metadata, repository, xPaths);
            builder.titleStudy(cmmStudyMapper.parseStudyTitle(metadata, xPaths, defaultLangIsoCode));

            var parseStudyUrlResults = cmmStudyMapper.parseStudyUrl(metadata, xPaths, defaultLangIsoCode);
            builder.studyUrl(parseStudyUrlResults.results());
            if (!parseStudyUrlResults.exceptions().isEmpty()) {
                log.warn("[{}] Some URLs in study {} couldn't be parsed: {}",
                    repository.getCode(),
                    studyNumber,
                    parseStudyUrlResults.exceptions()
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
            builder.typeOfSamplingProcedures(cmmStudyMapper.parseTypeOfSamplingProcedure(metadata, xPaths, defaultLangIsoCode));
            builder.samplingProcedureFreeTexts(cmmStudyMapper.parseSamplingProcedureFreeTexts(metadata, xPaths, defaultLangIsoCode));
            builder.typeOfModeOfCollections(cmmStudyMapper.parseTypeOfModeOfCollection(metadata, xPaths, defaultLangIsoCode));

            var dataCollectionPeriodResults = cmmStudyMapper.parseDataCollectionDates(metadata, xPaths);
            dataCollectionPeriodResults.results().getStartDate().ifPresent(builder::dataCollectionPeriodStartdate);
            dataCollectionPeriodResults.results().getEndDate().ifPresent(builder::dataCollectionPeriodEnddate);
            builder.dataCollectionYear(dataCollectionPeriodResults.results().getDataCollectionYear());
            if (!dataCollectionPeriodResults.exceptions().isEmpty()) {
                // Parsing errors occurred, log here
                log.warn("[{}] Some dates in study {} couldn't be parsed: {}",
                    repository.getCode(),
                    studyNumber,
                    dataCollectionPeriodResults.exceptions()
                );
            }

            builder.dataCollectionFreeTexts(cmmStudyMapper.parseDataCollectionFreeTexts(metadata, xPaths, defaultLangIsoCode));
            try {
                builder.universe(cmmStudyMapper.parseUniverses(metadata, xPaths, defaultLangIsoCode));
            } catch (InvalidUniverseException e) {
                log.warn("[{}] Some universes in study {} couldn't be parsed: {}", repository.getCode(), studyNumber, e.toString());
            }
            builder.relatedPublications(cmmStudyMapper.parseRelatedPublications(metadata, xPaths, defaultLangIsoCode));
        }

        URI repositoryUrl;
        if (request.baseURL() != null) {
            repositoryUrl = request.baseURL();
        } else {
            repositoryUrl = repository.getUrl();
        }
        builder.repositoryUrl(repositoryUrl);

        try {
            //should retrieve from header, if present
            builder.studyXmlSourceUrl(OaiPmhHelpers.buildGetStudyFullUrl(repository.getUrl(), studyNumber, repository.getPreferredMetadataParam()));
        } catch (URISyntaxException e) {
            log.warn("[{}] Study URL for {} couldn't be parsed: {}", repository.getCode(), studyNumber, e.toString());
        }

        return builder.build();
    }
}
