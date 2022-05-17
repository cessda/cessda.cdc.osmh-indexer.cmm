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

import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.exception.InvalidURIException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.http.HttpClient;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Service Class responsible for querying the repository to fetch remote records.
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class RecordXMLParser {

    private final CMMStudyMapper cmmStudyMapper;
    private final HttpClient httpClient;

    @Autowired
    public RecordXMLParser(CMMStudyMapper cmmStudyMapper, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.cmmStudyMapper = cmmStudyMapper;
    }

    /**
     * Gets a record from a remote repository.
     * @param repo the repository to retrieve the record from.
     * @param recordVar the study to retrieve.
     * @return a {@link CMMStudy} representing the study.
     * @throws OaiPmhException if the document contains an {@code <error>} element.
     * @throws XMLParseException if an error occurred parsing the XML.
     * @throws IndexerException if the request URL could not be converted into a {@link URI}.
     */
    public CMMStudy getRecord(Repo repo, Record recordVar) throws IndexerException {

        final Document document;

        URI fullUrl = null;

        if (recordVar.getDocument() == null) {
            try {
                // If the document is not present retrieve it from the OAI-PMH endpoint.
                fullUrl = OaiPmhHelpers.buildGetStudyFullUrl(repo, recordVar.getRecordHeader().getIdentifier());

                log.debug("[{}] Querying for StudyID [{}]", repo.getCode(), recordVar.getRecordHeader().getIdentifier());

                try (var recordXML = httpClient.getInputStream(fullUrl)) {
                    document = OaiPmhHelpers.getSaxBuilder().build(recordXML);
                    if (log.isTraceEnabled()) {
                        log.trace("Record XML String [{}]", new XMLOutputter().outputString(document));
                    }
                }
            } catch (JDOMException | IOException e) {
                throw new XMLParseException(fullUrl, e);
            } catch (URISyntaxException e) {
                throw new IndexerException(e);
            }
        } else {
            // The document has already been parsed.
            document = recordVar.getDocument();
            if (recordVar.getRequest() != null) {
                try {
                    fullUrl = OaiPmhHelpers.buildGetStudyFullUrl(recordVar.getRequest().getBaseURL(), recordVar.getRecordHeader().getIdentifier(), recordVar.getRequest().getMetadataPrefix());
                } catch (URISyntaxException e) {
                    throw new IndexerException(e);
                }
            }
        }
        return mapDDIRecordToCMMStudy(document, fullUrl, repo);
    }

    /**
     * Convert a {@link Document} to a {@link CMMStudy}.
     * @param document the {@link Document} to convert.
     * @param repository the source repository.
     * @throws OaiPmhException if the document contains an {@code <error>} element.
     */
    private CMMStudy mapDDIRecordToCMMStudy(Document document, URI fullUrl, Repo repository) throws OaiPmhException {

        CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();

        // Short-Circuit. We carry on to parse beyond the headers only if the record is active.
        var headerElement = cmmStudyMapper.parseHeaderElement(document);
        headerElement.getStudyNumber().ifPresent(builder::studyNumber);
        headerElement.getLastModified().ifPresent(builder::lastModified);
        builder.active(headerElement.isRecordActive());
        if (headerElement.isRecordActive()) {
            var xPaths = getXPaths(repository);
            var defaultLangIsoCode = cmmStudyMapper.parseDefaultLanguage(document, repository, xPaths);
            builder.titleStudy(cmmStudyMapper.parseStudyTitle(document, xPaths, defaultLangIsoCode));
            try {
                builder.studyUrl(cmmStudyMapper.parseStudyUrl(document, xPaths, defaultLangIsoCode));
            } catch (InvalidURIException e) {
                log.warn("[{}] Some URLs in study {} couldn't be parsed: {}", repository.getCode(), headerElement.getStudyNumber().orElse(""), e.toString());
            }
            builder.abstractField(cmmStudyMapper.parseAbstract(document, xPaths, defaultLangIsoCode));
            builder.pidStudies(cmmStudyMapper.parsePidStudies(document, xPaths, defaultLangIsoCode));
            builder.creators(cmmStudyMapper.parseCreator(document, xPaths, defaultLangIsoCode));
            builder.dataAccessFreeTexts(cmmStudyMapper.parseDataAccessFreeText(document, xPaths, defaultLangIsoCode));
            builder.classifications(cmmStudyMapper.parseClassifications(document, xPaths, defaultLangIsoCode));
            builder.keywords(cmmStudyMapper.parseKeywords(document, xPaths, defaultLangIsoCode));
            builder.typeOfTimeMethods(cmmStudyMapper.parseTypeOfTimeMethod(document, xPaths, defaultLangIsoCode));
            builder.studyAreaCountries(cmmStudyMapper.parseStudyAreaCountries(document, xPaths, defaultLangIsoCode));
            builder.unitTypes(cmmStudyMapper.parseUnitTypes(document, xPaths, defaultLangIsoCode));
            builder.publisher(cmmStudyMapper.parsePublisher(document, xPaths, defaultLangIsoCode));
            cmmStudyMapper.parseYrOfPublication(document, xPaths).ifPresent(builder::publicationYear);
            builder.fileLanguages(cmmStudyMapper.parseFileLanguages(document, xPaths));
            builder.typeOfSamplingProcedures(cmmStudyMapper.parseTypeOfSamplingProcedure(document, xPaths, defaultLangIsoCode));
            builder.samplingProcedureFreeTexts(cmmStudyMapper.parseSamplingProcedureFreeTexts(document, xPaths, defaultLangIsoCode));
            builder.typeOfModeOfCollections(cmmStudyMapper.parseTypeOfModeOfCollection(document, xPaths, defaultLangIsoCode));
            try {
                var dataCollectionPeriod = cmmStudyMapper.parseDataCollectionDates(document, xPaths);
                dataCollectionPeriod.getStartDate().ifPresent(builder::dataCollectionPeriodStartdate);
                dataCollectionPeriod.getEndDate().ifPresent(builder::dataCollectionPeriodEnddate);
                builder.dataCollectionYear(dataCollectionPeriod.getDataCollectionYear());
            } catch (DateNotParsedException e) {
                log.warn("[{}] Some dates in study {} couldn't be parsed: {}", repository.getCode(), headerElement.getStudyNumber().orElse(""), e.toString());
            }
            builder.dataCollectionFreeTexts(cmmStudyMapper.parseDataCollectionFreeTexts(document, xPaths, defaultLangIsoCode));
        }
        if (fullUrl != null) {
            builder.studyXmlSourceUrl(fullUrl.toString());
        }

        return builder.build();
    }

    private XPaths getXPaths(Repo repository) {
        if (repository.getHandler().equalsIgnoreCase("DDI_2_5")) {
            return XPaths.DDI_2_5_XPATHS;
        } else if (repository.getHandler().equalsIgnoreCase("NESSTAR")) {
            return XPaths.NESSTAR_XPATHS;
        }
        throw new IllegalArgumentException("Unexpected value: " + repository.getHandler());
    }
}
