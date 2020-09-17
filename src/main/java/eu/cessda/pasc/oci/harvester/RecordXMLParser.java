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
import eu.cessda.pasc.oci.http.HttpClient;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.CMMStudyMapper;
import eu.cessda.pasc.oci.parser.OaiPmhHelpers;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
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
class RecordXMLParser {

    private final CMMStudyMapper cmmStudyMapper;
    private final HttpClient httpClient;

    @Autowired
    public RecordXMLParser(CMMStudyMapper cmmStudyMapper, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.cmmStudyMapper = cmmStudyMapper;
    }

    public CMMStudy getRecord(Repo repo, String studyIdentifier) throws HarvesterException {
        log.debug("[{}] Querying for StudyID [{}]", repo.getCode(), studyIdentifier);
        URI fullUrl = null;
        try {
            fullUrl = OaiPmhHelpers.buildGetStudyFullUrl(repo, studyIdentifier);
            try (InputStream recordXML = httpClient.getInputStream(fullUrl)) {
                return mapDDIRecordToCMMStudy(recordXML, fullUrl, repo);
            }
        } catch (JDOMException | IOException e) {
            throw new XMLParseException(fullUrl, e);
        } catch (URISyntaxException e) {
            throw new HarvesterException(e);
        }
    }

    private CMMStudy mapDDIRecordToCMMStudy(InputStream recordXML, URI sourceUri, Repo repository) throws JDOMException, IOException, OaiPmhException {

        CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
        Document document = getSaxBuilder().build(recordXML);

        if (log.isTraceEnabled()) {
            log.trace("Record XML String [{}]", new XMLOutputter().outputString(document));
        }

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
            var dataCollectionPeriod = cmmStudyMapper.parseDataCollectionDates(document);
            dataCollectionPeriod.getStartDate().ifPresent(builder::dataCollectionPeriodStartdate);
            dataCollectionPeriod.getEndDate().ifPresent(builder::dataCollectionPeriodEnddate);
            builder.dataCollectionYear(dataCollectionPeriod.getDataCollectionYear());
            builder.dataCollectionFreeTexts(cmmStudyMapper.parseDataCollectionFreeTexts(document, defaultLangIsoCode));
        }
        return builder.studyXmlSourceUrl(sourceUri.toString()).build();
    }

    private SAXBuilder getSaxBuilder() {
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return saxBuilder;
    }
}
