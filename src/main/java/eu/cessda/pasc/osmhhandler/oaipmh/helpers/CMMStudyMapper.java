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

package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.Publisher;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HTMLFilter.CLEAN_MAP_VALUES;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.ParsingStrategies.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.TimeUtility.dataCollYearDateFunction;

/**
 * Responsible for Mapping oai-pmh fields to a CMMStudy
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@UtilityClass
public class CMMStudyMapper {

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
   * @param xFactory the xFactory
   * @return true if record is active
   */
  public static boolean parseHeaderElement(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    parseStudyNumber(builder, document, xFactory);
    parseLastModified(builder, document, xFactory);
    return parseRecordStatus(builder, document, xFactory);
  }

  public static String parseDefaultLanguage(Document document, XPathFactory xFactory, OaiPmh oaiPmh) {
    XPathExpression<Attribute> attributeExpression = xFactory
        .compile(RECORD_DEFAULT_LANGUAGE, Filters.attribute(), null, OAI_AND_DDI_NS);
    Optional<Attribute> codeBookLang = Optional.ofNullable(attributeExpression.evaluateFirst(document));
    return (codeBookLang.isPresent() && !codeBookLang.get().getValue().trim().isEmpty()) ?
        codeBookLang.get().getValue().trim() : oaiPmh.getMetadataParsingDefaultLang().getLang();
  }

  /**
   * Pass records status.
   */
  private static boolean parseRecordStatus(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    XPathExpression<Attribute> attributeExpression = xFactory
        .compile(RECORD_STATUS_XPATH, Filters.attribute(), null, OAI_NS);
    Attribute status = attributeExpression.evaluateFirst(document);
    boolean isActive = null == status || !"deleted".equalsIgnoreCase(status.getValue());
    builder.active(isActive);
    return isActive;
  }

  /**
   * Parse last Modified.
   */
  private static void parseLastModified(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    getFirstElement(document, xFactory, LAST_MODIFIED_DATE_XPATH)
        .ifPresent((Element element) -> builder.lastModified(element.getText()));
  }

  /**
   * Parse study number.
   */
  private static void parseStudyNumber(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    getFirstElement(document, xFactory, IDENTIFIER_XPATH)
        .ifPresent((Element element) -> builder.studyNumber(element.getText()));
  }

  /**
   * Parses Abstract
   * <p>
   * Xpath = {@value OaiPmhConstants#ABSTRACT_XPATH }
   */
  public static void parseAbstract(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config, String langCode) {

    Map<String, String> abstracts =
        parseLanguageContentOfElement(document, xFactory, config, langCode, ABSTRACT_XPATH, true);
    builder.abstractField(abstracts);
  }

  /**
   * Parses Year of Publication from:
   * <p>
   * Xpath = {@value OaiPmhConstants#YEAR_OF_PUB_XPATH }
   */
  public static void parseYrOfPublication(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    Optional<Attribute> yrOfPublicationDate = getFirstAttribute(document, xFactory, YEAR_OF_PUB_XPATH);
    yrOfPublicationDate.ifPresent(attribute -> builder.publicationYear(attribute.getValue()));
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PID_STUDY_XPATH }
   */
  public static void parsePidStudies(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                     OaiPmh oaiPmh, String defaultLangIsoCode) {
    builder.pidStudies(extractMetadataObjectListForEachLang(
        oaiPmh, defaultLangIsoCode, document, xFactory, PID_STUDY_XPATH, pidStrategyFunction()));
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CREATORS_XPATH }
   */
  public static void parseCreator(CMMStudy.CMMStudyBuilder builder, Document doc, XPathFactory xFactory,
                                  OaiPmh config, String defaultLangIsoCode) {

    Map<String, List<String>> creatorsInLangs = extractMetadataObjectListForEachLang(
        config, defaultLangIsoCode, doc, xFactory, CREATORS_XPATH, creatorStrategyFunction());

    builder.creators(creatorsInLangs);
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CLASSIFICATIONS_XPATH }
   */
  public static void parseClassifications(CMMStudy.CMMStudyBuilder builder, Document doc, XPathFactory xFactory,
                                          OaiPmh config, String defaultLangIsoCode) {
    builder.classifications(
        extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, doc, xFactory, CLASSIFICATIONS_XPATH,
            termVocabAttributeStrategyFunction(false)));
  }

  /**
   * Parses parseKeyword(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#KEYWORDS_XPATH }
   */
  public static void parseKeywords(CMMStudy.CMMStudyBuilder builder, Document doc, XPathFactory xFactory,
                                   OaiPmh config, String defaultLangIsoCode) {
    builder.keywords(
        extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, doc, xFactory, KEYWORDS_XPATH, termVocabAttributeStrategyFunction(false)));
  }

  /**
   * Parses Type Of Time Method(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_TIME_METHOD_XPATH }
   */
  public static void parseTypeOfTimeMethod(CMMStudy.CMMStudyBuilder builder, Document doc, XPathFactory xFactory,
                                           OaiPmh config, String defaultLangIsoCode) {

    builder.typeOfTimeMethods(
        extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, doc, xFactory, TYPE_OF_TIME_METHOD_XPATH,
            termVocabAttributeStrategyFunction(true)));
  }

  /**
   * Parses Type Of Mode Of Collection(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_MODE_OF_COLLECTION_XPATH }
   */
  public static void parseTypeOfModeOfCollection(CMMStudy.CMMStudyBuilder builder, Document doc,
                                                 XPathFactory xFactory, OaiPmh config, String defaultLangIsoCode) {
    builder.typeOfModeOfCollections(
        extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, doc, xFactory, TYPE_OF_MODE_OF_COLLECTION_XPATH,
            termVocabAttributeStrategyFunction(true)));
  }

  /**
   * Parses Unit Type(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#UNIT_TYPE_XPATH }
   */
  public static void parseUnitTypes(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                    OaiPmh config, String defaultLangIsoCode) {
    builder.unitTypes(
        extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, document, xFactory, UNIT_TYPE_XPATH, termVocabAttributeStrategyFunction(true)));
  }

  /**
   * Parses Type Of Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_SAMPLING_XPATH }
   */
  public static void parseTypeOfSamplingProcedure(CMMStudy.CMMStudyBuilder builder, Document doc,
                                                  XPathFactory xFactory, OaiPmh config, String defaultLangIsoCode) {
    builder.typeOfSamplingProcedures(
        extractMetadataObjectListForEachLang(config, defaultLangIsoCode, doc, xFactory, TYPE_OF_SAMPLING_XPATH,
            samplingTermVocabAttributeStrategyFunction(true)));
  }

  /**
   * Parses area Countries covered by a study:
   * <p>
   * Xpath = {@value OaiPmhConstants#STUDY_AREA_COUNTRIES_XPATH }
   */
  public static void parseStudyAreaCountries(CMMStudy.CMMStudyBuilder builder, Document document,
                                             XPathFactory xFactory, OaiPmh config, String defaultLangIsoCode) {
    builder.studyAreaCountries(extractMetadataObjectListForEachLang(
        config, defaultLangIsoCode, document, xFactory, STUDY_AREA_COUNTRIES_XPATH, countryStrategyFunction()));
  }

  /**
   * Parse Publisher from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PUBLISHER_XPATH } and
   * Xpath = {@value OaiPmhConstants#DISTRIBUTOR_XPATH }
   */
  public static void parsePublisher(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                    OaiPmh config, String defaultLang) {
    Map<String, Publisher> producerPathMap = extractMetadataObjectForEachLang(config, defaultLang, document, xFactory,
        PUBLISHER_XPATH, publisherStrategyFunction());
    Map<String, Publisher> distrPathMap = extractMetadataObjectForEachLang(config, defaultLang, document, xFactory,
        DISTRIBUTOR_XPATH, publisherStrategyFunction());

    Map<String, Publisher> mergedStudyUrls = new HashMap<>(producerPathMap);
    distrPathMap.forEach((k, v) -> mergedStudyUrls.merge(k, v, (docDscrValue, stdyDscrValue) -> docDscrValue));
    builder.publisher(mergedStudyUrls);
  }

  /**
   * Parses Study Title.
   * <p>
   * Xpath = {@value OaiPmhConstants#TITLE_XPATH } and {@value OaiPmhConstants#PAR_TITLE_XPATH }
   */
  public static void parseStudyTitle(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                     OaiPmh config, String langCode) {

    Map<String, String> titles = parseLanguageContentOfElement(document, xFactory, config, langCode, TITLE_XPATH, false);

    // https://bitbucket.org/cessda/cessda.cdc.version2/issues/135
    if (!titles.isEmpty()) {
      Map<String, String> parTitles =
          parseLanguageContentOfElement(document, xFactory, config, langCode, PAR_TITLE_XPATH, false);
      parTitles.forEach(titles::putIfAbsent);  // parTitl lang must not be same as or override titl lang
    }

    CLEAN_MAP_VALUES.apply(titles);
    builder.titleStudy(titles);
  }

  private static Map<String, String> parseLanguageContentOfElement(Document document, XPathFactory xFactory,
                                                                   OaiPmh config, String langCode, String titleXpath,
                                                                   boolean isConcatenating) {
    List<Element> elements = getElements(document, xFactory, titleXpath);
    return getLanguageKeyValuePairs(config, elements, isConcatenating, langCode, Element::getText);
  }

  /**
   * Parses parse Study Url from two plausible allowed xPaths
   * <p>
   * Xpath = {@value OaiPmhConstants#STUDY_URL_DOC_DSCR_XPATH }
   * Xpath = {@value OaiPmhConstants#STUDY_URL_STDY_DSCR_XPATH }
   */
  public static void parseStudyUrl(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                   OaiPmh config, String langCode) {

    List<Element> docDscrElement = getElements(document, xFactory, STUDY_URL_DOC_DSCR_XPATH);
    List<Element> stdyDscrElements = getElements(document, xFactory, STUDY_URL_STDY_DSCR_XPATH);
    Map<String, String> urlFromDocDscr =
        getLanguageKeyValuePairs(config, docDscrElement, false, langCode, uriStrategyFunction());
    Map<String, String> urlFromStdyDscr =
        getLanguageKeyValuePairs(config, stdyDscrElements, false, langCode, uriStrategyFunction());

    Map<String, String> mergedStudyUrls = new HashMap<>(urlFromDocDscr);
    urlFromStdyDscr.forEach((k, v) -> mergedStudyUrls.merge(k, v, (docDscrValue, stdyDscrValue) -> docDscrValue));
    builder.studyUrl(mergedStudyUrls);
  }

  /**
   * Parses Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#SAMPLING_XPATH }
   */
  public static void parseSamplingProcedureFreeTexts(CMMStudy.CMMStudyBuilder builder, Document doc,
                                                     XPathFactory xFactory, OaiPmh config, String defaultLangIsoCode) {
    builder.samplingProcedureFreeTexts(
        extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, doc, xFactory, SAMPLING_XPATH, nullableElementValueStrategyFunction()));
  }

  /**
   * Parses Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#SAMPLING_XPATH }
   */
  public static void parseDataAccessFreeText(CMMStudy.CMMStudyBuilder builder, Document doc,
                                             XPathFactory xFactory, OaiPmh config, String defaultLangIsoCode) {
    Map<String, List<String>> restrictionLanguageMap = extractMetadataObjectListForEachLang(
        config, defaultLangIsoCode, doc, xFactory, DATA_RESTRCTN_XPATH, nullableElementValueStrategyFunction());
    builder.dataAccessFreeTexts(restrictionLanguageMap);
  }

  /**
   * Parses Data Collection Period dates from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH}
   * <p>
   * For Data Collection start and end date plus the four digit Year value as Data Collection Year
   */
  public static void parseDataCollectionDates(CMMStudy.CMMStudyBuilder builder, Document doc,
                                              XPathFactory xFactory) {
    Map<String, String> dateAttrs = getDateElementAttributesValueMap(doc, xFactory, DATA_COLLECTION_PERIODS_PATH);

    if (dateAttrs.containsKey(SINGLE_ATTR)) {
      final String singleDateValue = dateAttrs.get(SINGLE_ATTR);
      builder.dataCollectionPeriodStartdate(singleDateValue);
      builder.dataCollectionYear(dataCollYearDateFunction().apply(singleDateValue).orElse(0));
    } else {
      if (dateAttrs.containsKey(START_ATTR)) {
        final String startDateValue = dateAttrs.get(START_ATTR);
        builder.dataCollectionPeriodStartdate(startDateValue);
        builder.dataCollectionYear(dataCollYearDateFunction().apply(startDateValue).orElse(0));
      }
      if (dateAttrs.containsKey(END_ATTR)) {
        builder.dataCollectionPeriodEnddate(dateAttrs.get(END_ATTR));
      }
    }
  }

  /**
   * Parses area Countries covered by a study:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH }
   */
  public static void parseDataCollectionFreeTexts(CMMStudy.CMMStudyBuilder builder, Document document,
                                                  XPathFactory xFactory, OaiPmh config, String defaultLangIsoCode) {
    builder.dataCollectionFreeTexts(extractMetadataObjectListForEachLang(
        config, defaultLangIsoCode, document, xFactory, DATA_COLLECTION_PERIODS_PATH, dataCollFreeTextStrategyFunction()));
  }

  /**
   * Parses File Language(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#FILE_TXT_LANGUAGES_XPATH }
   */
  public static void parseFileLanguages(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] fileTxtAttrs = getAttributeValues(document, xFactory, FILE_TXT_LANGUAGES_XPATH);
    String[] fileNameAttrs = getAttributeValues(document, xFactory, FILENAME_LANGUAGES_XPATH);
    String[] allLangs = Stream.of(fileTxtAttrs, fileNameAttrs).flatMap(Stream::of).toArray(String[]::new);
    Set<String> languageFilesSet = Arrays.stream(allLangs).collect(Collectors.toSet());
    builder.fileLanguages(languageFilesSet);
  }
}