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
import eu.cessda.pasc.oci.helpers.CMMStudyMapper;
import eu.cessda.pasc.oci.helpers.OaiPmhHelpers;
import eu.cessda.pasc.oci.helpers.RecordResponseValidator;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.models.oai.configuration.OaiPmh;
import eu.cessda.pasc.oci.repository.DaoBase;
import eu.cessda.pasc.oci.service.GetRecordService;
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
public class GetRecordServiceImpl implements GetRecordService {

    private final CMMStudyMapper cmmStudyMapper;
    private final DaoBase daoBase;
    private final RecordResponseValidator recordResponseValidator;
    private final HandlerConfigurationProperties oaiPmhHandlerConfig;

    @Autowired
    public GetRecordServiceImpl(CMMStudyMapper cmmStudyMapper, DaoBase daoBase, HandlerConfigurationProperties oaiPmhHandlerConfig, RecordResponseValidator recordResponseValidator) {
        this.daoBase = daoBase;
        this.oaiPmhHandlerConfig = oaiPmhHandlerConfig;
        this.cmmStudyMapper = cmmStudyMapper;
        this.recordResponseValidator = recordResponseValidator;
    }

    @Override
    public CMMStudy getRecord(Repo repo, String studyIdentifier) throws InternalSystemException, OaiPmhException {
        log.debug("[{}] Querying for StudyID [{}]", repo.getName(), studyIdentifier);
        URI fullUrl = null;
        try {
            fullUrl = OaiPmhHelpers.buildGetStudyFullUrl(repo, studyIdentifier);
            try (InputStream recordXML = daoBase.getInputStream(fullUrl)) {
                return mapDDIRecordToCMMStudy(recordXML).studyXmlSourceUrl(fullUrl.toString()).build();
            }
        } catch (JDOMException | IOException e) {
            throw new InternalSystemException(String.format("Unable to parse xml! FullUrl [%s]: %s", fullUrl, e.toString()), e);
        } catch (URISyntaxException e) {
            throw new InternalSystemException("Unable to construct URL: " + e.toString(), e);
        }
    }

    private CMMStudy.CMMStudyBuilder mapDDIRecordToCMMStudy(InputStream recordXML) throws JDOMException, IOException, OaiPmhException {

        CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
        OaiPmh oaiPmh = oaiPmhHandlerConfig.getOaiPmh();
        Document document = getSaxBuilder().build(recordXML);

        if (log.isTraceEnabled()) {
            log.trace("Record XML String [{}]", new XMLOutputter().outputString(document));
        }

        // We exit if the record has an <error> element
        recordResponseValidator.validateResponse(document);

        // Short-Circuit. We carry on to parse beyond the headers only if the record is active.
        boolean isActiveRecord = cmmStudyMapper.parseHeaderElement(builder, document);
        if (isActiveRecord) {
            String defaultLangIsoCode = cmmStudyMapper.parseDefaultLanguage(document, oaiPmh);
            cmmStudyMapper.parseStudyTitle(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseStudyUrl(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseAbstract(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parsePidStudies(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseCreator(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseDataAccessFreeText(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseClassifications(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseKeywords(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseTypeOfTimeMethod(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseStudyAreaCountries(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseUnitTypes(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parsePublisher(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseYrOfPublication(builder, document);
            cmmStudyMapper.parseFileLanguages(builder, document);
            cmmStudyMapper.parseTypeOfSamplingProcedure(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseSamplingProcedureFreeTexts(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseTypeOfModeOfCollection(builder, document, oaiPmh, defaultLangIsoCode);
            cmmStudyMapper.parseDataCollectionDates(builder, document);
            cmmStudyMapper.parseDataCollectionFreeTexts(builder, document, oaiPmh, defaultLangIsoCode);
        }
        return builder;
    }

    private SAXBuilder getSaxBuilder() {
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return saxBuilder;
    }
}