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
import eu.cessda.pasc.oci.exception.HarvesterException;
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
import java.io.InputStream;
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
     * @param record the study to retrieve.
     * @return a {@link CMMStudy} representing the study.
     * @throws OaiPmhException if the document contains an {@code <error>} element.
     * @throws XMLParseException if an error occurred parsing the XML.
     * @throws HarvesterException if the request URL could not be converted into a {@link URI}.
     */
    public CMMStudy getRecord(Repo repo, Record record) throws HarvesterException {

        final Document document;

        if (record.getDocument() == null) {
            URI fullUrl = null;
            try {
                // If the document is not present retrieve it from the OAI-PMH endpoint.
                fullUrl = OaiPmhHelpers.buildGetStudyFullUrl(repo, record.getRecordHeader().getIdentifier());

                log.debug("[{}] Querying for StudyID [{}]", repo.getCode(), record.getRecordHeader().getIdentifier());

                try (InputStream recordXML = httpClient.getInputStream(fullUrl)) {
                    document = OaiPmhHelpers.getSaxBuilder().build(recordXML);
                    if (log.isTraceEnabled()) {
                        log.trace("Record XML String [{}]", new XMLOutputter().outputString(document));
                    }
                }
            } catch (JDOMException | IOException e) {
                throw new XMLParseException(fullUrl, e);
            } catch (URISyntaxException e) {
                throw new HarvesterException(e);
            }
        } else {
            // The document has already been parsed.
            document = record.getDocument();
        }
        return mapDDIRecordToCMMStudy(document, repo);
    }

    /**
     * Convert a {@link Document} to a {@link CMMStudy}.
     * @param document the {@link Document} to convert.
     * @param repository the source repository.
     * @throws OaiPmhException if the document contains an {@code <error>} element.
     */
    private CMMStudy mapDDIRecordToCMMStudy(Document document, Repo repository) throws OaiPmhException {

        CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();

        // Short-Circuit. We carry on to parse beyond the headers only if the record is active.
        var headerElement = cmmStudyMapper.parseHeaderElement(document);
        headerElement.getStudyNumber().ifPresent(builder::studyNumber);
        headerElement.getLastModified().ifPresent(builder::lastModified);
        builder.active(headerElement.isRecordActive());
        if (headerElement.isRecordActive()) {
            String defaultLangIsoCode = cmmStudyMapper.parseDefaultLanguage(document, repository);
            builder.titleStudy(cmmStudyMapper.parseStudyTitle(document, defaultLangIsoCode));
            builder.studyUrl(cmmStudyMapper.parseStudyUrl(document, defaultLangIsoCode));
            builder.abstractField(cmmStudyMapper.parseAbstract(document, defaultLangIsoCode));
            builder.pidStudies(cmmStudyMapper.parsePidStudies(document, defaultLangIsoCode));
            builder.creators(cmmStudyMapper.parseCreator(document, defaultLangIsoCode));
            builder.dataAccessFreeTexts(cmmStudyMapper.parseDataAccessFreeText(document, defaultLangIsoCode));
            builder.classifications(cmmStudyMapper.parseClassifications(document, defaultLangIsoCode));
            builder.keywords(cmmStudyMapper.parseKeywords(document, defaultLangIsoCode));
            builder.typeOfTimeMethods(cmmStudyMapper.parseTypeOfTimeMethod(document, defaultLangIsoCode));
            builder.studyAreaCountries(cmmStudyMapper.parseStudyAreaCountries(document, defaultLangIsoCode));
            builder.unitTypes(cmmStudyMapper.parseUnitTypes(document, defaultLangIsoCode));
            builder.publisher(cmmStudyMapper.parsePublisher(document, defaultLangIsoCode));
            cmmStudyMapper.parseYrOfPublication(document).ifPresent(builder::publicationYear);
            builder.fileLanguages(cmmStudyMapper.parseFileLanguages(document));
            builder.typeOfSamplingProcedures(cmmStudyMapper.parseTypeOfSamplingProcedure(document, defaultLangIsoCode));
            builder.samplingProcedureFreeTexts(cmmStudyMapper.parseSamplingProcedureFreeTexts(document, defaultLangIsoCode));
            builder.typeOfModeOfCollections(cmmStudyMapper.parseTypeOfModeOfCollection(document, defaultLangIsoCode));
            try {
                var dataCollectionPeriod = cmmStudyMapper.parseDataCollectionDates(document);
                dataCollectionPeriod.getStartDate().ifPresent(builder::dataCollectionPeriodStartdate);
                dataCollectionPeriod.getEndDate().ifPresent(builder::dataCollectionPeriodEnddate);
                builder.dataCollectionYear(dataCollectionPeriod.getDataCollectionYear());
            } catch (DateNotParsedException e) {
                log.warn("[{}] Some dates in study {} couldn't be parsed: {}", repository.getCode(), headerElement.getStudyNumber().orElse(""), e.toString());
            }
            builder.dataCollectionFreeTexts(cmmStudyMapper.parseDataCollectionFreeTexts(document, defaultLangIsoCode));
        }
        return builder.build();
    }

}
