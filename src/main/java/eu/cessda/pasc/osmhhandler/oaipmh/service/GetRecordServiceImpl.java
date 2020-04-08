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

package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.dao.GetRecordDoa;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.CMMStudyMapper.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.RecordResponseValidator.validateResponse;

/**
 * Service Class responsible for querying the repository to fetch remote records.
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class GetRecordServiceImpl implements GetRecordService {

  private static final XPathFactory X_FACTORY = XPathFactory.instance();
  private final GetRecordDoa getRecordDoa;
  private final HandlerConfigurationProperties oaiPmhHandlerConfig;

  @Autowired
  public GetRecordServiceImpl(GetRecordDoa getRecordDoa, HandlerConfigurationProperties oaiPmhHandlerConfig) {
    this.getRecordDoa = getRecordDoa;
    this.oaiPmhHandlerConfig = oaiPmhHandlerConfig;
  }

  @Override
  public CMMStudy getRecord(String repositoryUrl, String studyIdentifier) throws CustomHandlerException {
    log.trace("Querying Repo [{}] for StudyID [{}]", repositoryUrl, studyIdentifier);
    String fullUrl = OaiPmhHelpers.buildGetStudyFullUrl(repositoryUrl, studyIdentifier, oaiPmhHandlerConfig);

    try (InputStream recordXML = getRecordDoa.getRecordXML(fullUrl)) {
      return mapDDIRecordToCMMStudy(recordXML, repositoryUrl).studyXmlSourceUrl(fullUrl).build();
    } catch (JDOMException | IOException e) {
      throw new InternalSystemException(String.format("Unable to parse xml! FullUrl [%s]", fullUrl), e);
    }
  }

  private CMMStudy.CMMStudyBuilder mapDDIRecordToCMMStudy(InputStream recordXML, String repositoryUrl)
          throws JDOMException, IOException, InternalSystemException {

    CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
    OaiPmh oaiPmh = oaiPmhHandlerConfig.getOaiPmh();
    Document document = new SAXBuilder().build(recordXML);

    log.trace("Record XML String [{}]", document);

    // We exit if the record has an <error> element
    ErrorStatus errorStatus = validateResponse(document, X_FACTORY);
    if (errorStatus.isHasError()) {
      throw new InternalSystemException("Remote repository " + repositoryUrl + " returned error: " + errorStatus.getMessage());
    }

    // Short-Circuit. We carry on to parse beyond the headers only if record is active
    boolean isActiveRecord = parseHeaderElement(builder, document, X_FACTORY);
    if (isActiveRecord) {
      String defaultLangIsoCode = parseDefaultLanguage(document, X_FACTORY, oaiPmh);
      parseStudyTitle(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseStudyUrl(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseAbstract(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parsePidStudies(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseCreator(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseDataAccessFreeText(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseClassifications(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseKeywords(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseTypeOfTimeMethod(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseStudyAreaCountries(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseUnitTypes(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parsePublisher(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseYrOfPublication(builder, document, X_FACTORY);
      parseFileLanguages(builder, document, X_FACTORY);
      parseTypeOfSamplingProcedure(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseSamplingProcedureFreeTexts(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseTypeOfModeOfCollection(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
      parseDataCollectionDates(builder, document, X_FACTORY);
      parseDataCollectionFreeTexts(builder, document, X_FACTORY, oaiPmh, defaultLangIsoCode);
    }
    return builder;
  }
}
