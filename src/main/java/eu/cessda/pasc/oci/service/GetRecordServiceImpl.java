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

package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.oci.dao.GetRecordDoa;
import eu.cessda.pasc.oci.exception.CustomHandlerException;
import eu.cessda.pasc.oci.exception.InternalSystemException;
import eu.cessda.pasc.oci.helpers.OaiPmhHelpers;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configuration.OaiPmh;
import eu.cessda.pasc.oci.models.errors.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.io.InputStream;

import static eu.cessda.pasc.oci.helpers.CMMStudyMapper.*;
import static eu.cessda.pasc.oci.helpers.RecordResponseValidator.validateResponse;

/**
 * Service Class responsible for querying the repository to fetch remote records.
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class GetRecordServiceImpl implements GetRecordService {

  private final XPathFactory xPathFactory;
  private final GetRecordDoa getRecordDoa;
  private final HandlerConfigurationProperties oaiPmhHandlerConfig;

  @Autowired
  public GetRecordServiceImpl(GetRecordDoa getRecordDoa, HandlerConfigurationProperties oaiPmhHandlerConfig, XPathFactory xPathFactory) {
    this.getRecordDoa = getRecordDoa;
    this.oaiPmhHandlerConfig = oaiPmhHandlerConfig;
    this.xPathFactory = xPathFactory;
  }

  @Override
  public CMMStudy getRecord(String repositoryUrl, String studyIdentifier) throws CustomHandlerException {
    log.info("Querying Repo [{}] for StudyID [{}]", repositoryUrl, studyIdentifier);
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
    Document document = getSaxBuilder().build(recordXML);

    if (log.isTraceEnabled()) {
      log.trace("Record XML String [{}]", new XMLOutputter().outputString(document));
    }

    // We exit if the record has an <error> element
    ErrorStatus errorStatus = validateResponse(document, xPathFactory);
    if (errorStatus.isHasError()) {
      throw new InternalSystemException("Remote repository " + repositoryUrl + " returned error: " + errorStatus.getMessage());
    }

    // Short-Circuit. We carry on to parse beyond the headers only if the record is active.
    boolean isActiveRecord = parseHeaderElement(builder, document, xPathFactory);
    if (isActiveRecord) {
      String defaultLangIsoCode = parseDefaultLanguage(document, xPathFactory, oaiPmh);
      parseStudyTitle(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseStudyUrl(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseAbstract(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parsePidStudies(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseCreator(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseDataAccessFreeText(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseClassifications(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseKeywords(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseTypeOfTimeMethod(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseStudyAreaCountries(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseUnitTypes(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parsePublisher(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseYrOfPublication(builder, document, xPathFactory);
      parseFileLanguages(builder, document, xPathFactory);
      parseTypeOfSamplingProcedure(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseSamplingProcedureFreeTexts(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseTypeOfModeOfCollection(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
      parseDataCollectionDates(builder, document, xPathFactory);
      parseDataCollectionFreeTexts(builder, document, xPathFactory, oaiPmh, defaultLangIsoCode);
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
