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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
  private final Map<String, Counter> activeRecordCounters = new HashMap<>();
  private final Map<String, Counter> inactiveRecordCounters = new HashMap<>();

  @Autowired
  public GetRecordServiceImpl(GetRecordDoa getRecordDoa, HandlerConfigurationProperties oaiPmhHandlerConfig, MeterRegistry meterRegistry) {
    this.getRecordDoa = getRecordDoa;
    this.oaiPmhHandlerConfig = oaiPmhHandlerConfig;
    for (var repo : oaiPmhHandlerConfig.getOaiPmh().getRepos()) {
      activeRecordCounters.put(repo.getUrl(), Counter.builder("cdc.oai-pmh.records.active.retrieved").tag("url", repo.getUrl())
              .description("Active records retrieved from endpoints").register(meterRegistry));
      inactiveRecordCounters.put(repo.getUrl(), Counter.builder("cdc.oai-pmh.records.inactive.retrieved").tag("url", repo.getUrl())
              .description("Inactive records retrieved from endpoints").register(meterRegistry));
    }
  }

  @Override
  public CMMStudy getRecord(String repositoryUrl, String studyIdentifier) throws CustomHandlerException {
    log.trace("Querying Repo [{}] for StudyID [{}]", repositoryUrl, studyIdentifier);
    String fullUrl = OaiPmhHelpers.buildGetStudyFullUrl(repositoryUrl, studyIdentifier, oaiPmhHandlerConfig);
    String recordXML = getRecordDoa.getRecordXML(fullUrl);

    try {
      return mapDDIRecordToCMMStudy(recordXML, repositoryUrl).studyXmlSourceUrl(fullUrl).build();
    } catch (JDOMException | IOException e) {
      throw new InternalSystemException(String.format("Unable to parse xml error message [%s], FullUrl [%s]", e.getMessage(), fullUrl), e);
    }
  }

  private CMMStudy.CMMStudyBuilder mapDDIRecordToCMMStudy(String recordXML, String repositoryUrl)
          throws JDOMException, IOException, InternalSystemException {

    CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
    OaiPmh oaiPmh = oaiPmhHandlerConfig.getOaiPmh();
    Document document = new SAXBuilder().build(IOUtils.toInputStream(recordXML, Charsets.UTF_8));

    log.trace("Record XML String [{}]", document);

    // We exit if the record has an <error> element
    ErrorStatus errorStatus = validateResponse(document, X_FACTORY);
    if (errorStatus.isHasError()) {
      log.debug("Returned Record has an <error> element with ErrorStatus message [{}]", errorStatus.getMessage());
      throw new InternalSystemException(errorStatus.getMessage());
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
      activeRecordCounters.get(repositoryUrl).increment();
    } else {
      inactiveRecordCounters.get(repositoryUrl).increment();
    }
    return builder;
  }
}
