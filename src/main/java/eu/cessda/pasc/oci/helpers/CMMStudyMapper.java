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

package eu.cessda.pasc.oci.helpers;

import eu.cessda.pasc.oci.configurations.HandlerConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import eu.cessda.pasc.oci.models.oai.configuration.OaiPmh;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cessda.pasc.oci.helpers.DocElementParser.getLanguageKeyValuePairs;
import static eu.cessda.pasc.oci.helpers.OaiPmhConstants.*;
import static eu.cessda.pasc.oci.helpers.ParsingStrategies.*;
import static eu.cessda.pasc.oci.helpers.TimeUtility.dataCollYearDateFunction;

/**
 * Responsible for Mapping oai-pmh fields to a CMMStudy
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@Component
public class CMMStudyMapper {

  private final DocElementParser docElementParser;
  private final XPathFactory xFactory;
  private final OaiPmh oaiPmh;

  @Autowired
  public CMMStudyMapper(DocElementParser docElementParser, XPathFactory xFactory, HandlerConfigurationProperties handlerConfigurationProperties) {
    this.docElementParser = docElementParser;
    this.xFactory = xFactory;
    this.oaiPmh = handlerConfigurationProperties.getOaiPmh();
  }

  /**
   * NOTE: Extracts the Study Number from the header element
   * <p>
   * Original specified path cant be relied on (/codeBook/stdyDscr/citation/titlStmt/IDNo)
   * <ul>
   * <li>It may have multiple identifiers for different agency.</li>
   * <li>Where as The header will by default specify the unique code identifier
   * for the repo(agency) we are querying
   * </li>
   * <p>
   * </ul>
   * Actual path used: /record/header/identifier
   *
   * @param builder  the document builder
   * @param document the document to parse
   * @return true if record is active
   */
  public boolean parseHeaderElement(CMMStudy.CMMStudyBuilder builder, Document document) {
    parseStudyNumber(document).ifPresent(builder::studyNumber);
    parseLastModified(document).ifPresent(builder::lastModified);
    boolean recordStatus = parseRecordStatus(document);
    builder.active(recordStatus);
    return recordStatus;
  }

  public String parseDefaultLanguage(Document document) {
    XPathExpression<Attribute> attributeExpression = xFactory
            .compile(RECORD_DEFAULT_LANGUAGE, Filters.attribute(), null, OAI_AND_DDI_NS);
    Optional<Attribute> codeBookLang = Optional.ofNullable(attributeExpression.evaluateFirst(document));
    return (codeBookLang.isPresent() && !codeBookLang.get().getValue().trim().isEmpty()) ?
            codeBookLang.get().getValue().trim() : oaiPmh.getMetadataParsingDefaultLang().getLang();
  }

  /**
   * Parse records status.
   */
  private boolean parseRecordStatus(Document document) {
    Attribute status = xFactory.compile(RECORD_STATUS_XPATH, Filters.attribute(), null, OAI_NS).evaluateFirst(document);
    return status == null || !status.getValue().equalsIgnoreCase("deleted");
  }

  /**
   * Parse last Modified.
   */
  private Optional<String> parseLastModified(Document document) {
    return docElementParser.getFirstElement(document, LAST_MODIFIED_DATE_XPATH).map(Element::getText);
  }

  /**
   * Parse study number.
   */
  private Optional<String> parseStudyNumber(Document document) {
    return docElementParser.getFirstElement(document, IDENTIFIER_XPATH).map(Element::getText);
  }

  /**
   * Parses Abstract
   * <p>
   * Xpath = {@value OaiPmhConstants#ABSTRACT_XPATH }
   */
  public Map<String, String> parseAbstract(Document document, String langCode) {
    return parseLanguageContentOfElement(document, langCode, ABSTRACT_XPATH, true);
  }

  /**
   * Parses Year of Publication from:
   * <p>
   * Xpath = {@value OaiPmhConstants#YEAR_OF_PUB_XPATH }
   */
  public Optional<String> parseYrOfPublication(Document document) {
    return docElementParser.getFirstAttribute(document, YEAR_OF_PUB_XPATH).map(Attribute::getValue);
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PID_STUDY_XPATH }
   */
  public Map<String, List<Pid>> parsePidStudies(Document document, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, document, PID_STUDY_XPATH, pidStrategyFunction());
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CREATORS_XPATH }
   */
  public Map<String, List<String>> parseCreator(Document doc, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, doc, CREATORS_XPATH, creatorStrategyFunction());
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CLASSIFICATIONS_XPATH }
   */
  public Map<String, List<TermVocabAttributes>> parseClassifications(Document doc, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, doc, CLASSIFICATIONS_XPATH,
            termVocabAttributeStrategyFunction(false));
  }

  /**
   * Parses parseKeyword(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#KEYWORDS_XPATH }
   */
  public Map<String, List<TermVocabAttributes>> parseKeywords(Document doc, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, doc, KEYWORDS_XPATH, termVocabAttributeStrategyFunction(false));
  }

  /**
   * Parses Type Of Time Method(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_TIME_METHOD_XPATH }
   */
  public Map<String, List<TermVocabAttributes>> parseTypeOfTimeMethod(Document doc, OaiPmh config, String defaultLangIsoCode) {

    return docElementParser.extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, doc, TYPE_OF_TIME_METHOD_XPATH,
            termVocabAttributeStrategyFunction(true));
  }

  /**
   * Parses Type Of Mode Of Collection(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_MODE_OF_COLLECTION_XPATH }
   */
  public Map<String, List<TermVocabAttributes>> parseTypeOfModeOfCollection(Document doc, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, doc, TYPE_OF_MODE_OF_COLLECTION_XPATH,
            termVocabAttributeStrategyFunction(true));
  }

  /**
   * Parses Unit Type(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#UNIT_TYPE_XPATH }
   */
  public Map<String, List<TermVocabAttributes>> parseUnitTypes(Document document, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, document, UNIT_TYPE_XPATH, termVocabAttributeStrategyFunction(true));
  }

  /**
   * Parses Type Of Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_SAMPLING_XPATH }
   */
  public Map<String, List<VocabAttributes>> parseTypeOfSamplingProcedure(Document doc, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(oaiPmh, defaultLangIsoCode, doc, TYPE_OF_SAMPLING_XPATH,
            samplingTermVocabAttributeStrategyFunction(true));
  }

  /**
   * Parses area Countries covered by a study:
   * <p>
   * Xpath = {@value OaiPmhConstants#STUDY_AREA_COUNTRIES_XPATH }
   */
  public Map<String, List<Country>> parseStudyAreaCountries(Document document, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, document, STUDY_AREA_COUNTRIES_XPATH, countryStrategyFunction());
  }

  /**
   * Parse Publisher from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PUBLISHER_XPATH } and
   * Xpath = {@value OaiPmhConstants#DISTRIBUTOR_XPATH }
   */
  public Map<String, Publisher> parsePublisher(Document document, String defaultLang) {
    Map<String, Publisher> producerPathMap = docElementParser.extractMetadataObjectForEachLang(oaiPmh, defaultLang, document,
            PUBLISHER_XPATH, publisherStrategyFunction());
    Map<String, Publisher> distrPathMap = docElementParser.extractMetadataObjectForEachLang(oaiPmh, defaultLang, document,
            DISTRIBUTOR_XPATH, publisherStrategyFunction());

    Map<String, Publisher> mergedStudyUrls = new HashMap<>(producerPathMap);
    distrPathMap.forEach((k, v) -> mergedStudyUrls.merge(k, v, (docDscrValue, stdyDscrValue) -> docDscrValue));
    return mergedStudyUrls;
  }

  /**
   * Parses Study Title.
   * <p>
   * Xpath = {@value OaiPmhConstants#TITLE_XPATH } and {@value OaiPmhConstants#PAR_TITLE_XPATH }
   */
  public Map<String, String> parseStudyTitle(Document document, String langCode) {

    Map<String, String> titles = parseLanguageContentOfElement(document, langCode, TITLE_XPATH, false);

    // https://bitbucket.org/cessda/cessda.cdc.version2/issues/135
    if (!titles.isEmpty()) {
      Map<String, String> parTitles =
              parseLanguageContentOfElement(document, langCode, PAR_TITLE_XPATH, false);
      parTitles.forEach(titles::putIfAbsent);  // parTitl lang must not be same as or override titl lang
    }
    HTMLFilter.cleanMapValues(titles);
    return titles;
  }

  private Map<String, String> parseLanguageContentOfElement(Document document, String langCode, String titleXpath, boolean isConcatenating) {
    List<Element> elements = docElementParser.getElements(document, titleXpath);
    return getLanguageKeyValuePairs(oaiPmh, elements, isConcatenating, langCode, Element::getText);
  }

  /**
   * Parses parse Study Url from two plausible allowed xPaths
   * <p>
   * Xpath = {@value OaiPmhConstants#STUDY_URL_DOC_DSCR_XPATH }
   * Xpath = {@value OaiPmhConstants#STUDY_URL_STDY_DSCR_XPATH }
   */
  public Map<String, String> parseStudyUrl(Document document, String langCode) {

    List<Element> docDscrElement = docElementParser.getElements(document, STUDY_URL_DOC_DSCR_XPATH);
    List<Element> stdyDscrElements = docElementParser.getElements(document, STUDY_URL_STDY_DSCR_XPATH);
    Map<String, String> urlFromDocDscr =
            getLanguageKeyValuePairs(oaiPmh, docDscrElement, false, langCode, uriStrategyFunction());
    Map<String, String> urlFromStdyDscr =
            getLanguageKeyValuePairs(oaiPmh, stdyDscrElements, false, langCode, uriStrategyFunction());

    Map<String, String> mergedStudyUrls = new HashMap<>(urlFromDocDscr);
    urlFromStdyDscr.forEach((k, v) -> mergedStudyUrls.merge(k, v, (docDscrValue, stdyDscrValue) -> docDscrValue));
    return mergedStudyUrls;

  }

  /**
   * Parses Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#SAMPLING_XPATH }
   */
  public Map<String, List<String>> parseSamplingProcedureFreeTexts(Document doc, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, doc, SAMPLING_XPATH, nullableElementValueStrategyFunction());
  }

  /**
   * Parses Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#SAMPLING_XPATH }
   */
  public Map<String, List<String>> parseDataAccessFreeText(Document doc, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, doc, DATA_RESTRCTN_XPATH, nullableElementValueStrategyFunction());
  }

  /**
   * Parses Data Collection Period dates from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH}
   * <p>
   * For Data Collection start and end date plus the four digit Year value as Data Collection Year
   */
  public DataCollectionPeriod parseDataCollectionDates(Document doc) {
    Map<String, String> dateAttrs = docElementParser.getDateElementAttributesValueMap(doc, DATA_COLLECTION_PERIODS_PATH);

    var dataCollectionPeriodBuilder = DataCollectionPeriod.builder();

    if (dateAttrs.containsKey(SINGLE_ATTR)) {
      final String singleDateValue = dateAttrs.get(SINGLE_ATTR);
      dataCollectionPeriodBuilder.startDate(singleDateValue);
      dataCollectionPeriodBuilder.dataCollectionYear(dataCollYearDateFunction().apply(singleDateValue).orElse(0));
    } else {
      if (dateAttrs.containsKey(START_ATTR)) {
        final String startDateValue = dateAttrs.get(START_ATTR);
        dataCollectionPeriodBuilder.startDate(startDateValue);
        dataCollectionPeriodBuilder.dataCollectionYear(dataCollYearDateFunction().apply(startDateValue).orElse(0));
      }
      if (dateAttrs.containsKey(END_ATTR)) {
        dataCollectionPeriodBuilder.endDate(dateAttrs.get(END_ATTR));
      }
    }

    return dataCollectionPeriodBuilder.build();
  }

  /**
   * Parses area Countries covered by a study:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH }
   */
  public Map<String, List<DataCollectionFreeText>> parseDataCollectionFreeTexts(Document document, String defaultLangIsoCode) {
    return docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, document, DATA_COLLECTION_PERIODS_PATH, dataCollFreeTextStrategyFunction());
  }

  /**
   * Parses File Language(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#FILE_TXT_LANGUAGES_XPATH }
   *
   * @return a set with all of the file languages
   */
  public Set<String> parseFileLanguages(Document document) {
    List<String> fileTxtAttrs = docElementParser.getAttributeValues(document, FILE_TXT_LANGUAGES_XPATH);
    List<String> fileNameAttrs = docElementParser.getAttributeValues(document, FILENAME_LANGUAGES_XPATH);
    return Stream.concat(fileTxtAttrs.stream(), fileNameAttrs.stream()).collect(Collectors.toSet());
  }

  @Builder
  @Value
  public static class DataCollectionPeriod {
    String startDate;
    int dataCollectionYear;
    String endDate;

    public Optional<String> getStartDate() {
      return Optional.ofNullable(startDate);
    }

    public Optional<String> getEndDate() {
      return Optional.ofNullable(endDate);
    }
  }
}