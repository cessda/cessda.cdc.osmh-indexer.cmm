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

import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import eu.cessda.pasc.oci.models.oai.configuration.OaiPmh;
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

  @Autowired
  public CMMStudyMapper(DocElementParser docElementParser, XPathFactory xFactory) {
    this.docElementParser = docElementParser;
    this.xFactory = xFactory;
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
    parseStudyNumber(builder, document);
    parseLastModified(builder, document);
    return parseRecordStatus(builder, document);
  }

  public String parseDefaultLanguage(Document document, OaiPmh oaiPmh) {
    XPathExpression<Attribute> attributeExpression = xFactory
            .compile(RECORD_DEFAULT_LANGUAGE, Filters.attribute(), null, OAI_AND_DDI_NS);
    Optional<Attribute> codeBookLang = Optional.ofNullable(attributeExpression.evaluateFirst(document));
    return (codeBookLang.isPresent() && !codeBookLang.get().getValue().trim().isEmpty()) ?
            codeBookLang.get().getValue().trim() : oaiPmh.getMetadataParsingDefaultLang().getLang();
  }

  /**
   * Parse records status.
   */
  private boolean parseRecordStatus(CMMStudy.CMMStudyBuilder builder, Document document) {
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
  private void parseLastModified(CMMStudy.CMMStudyBuilder builder, Document document) {
    docElementParser.getFirstElement(document, LAST_MODIFIED_DATE_XPATH)
            .ifPresent((Element element) -> builder.lastModified(element.getText()));
  }

  /**
   * Parse study number.
   */
  private void parseStudyNumber(CMMStudy.CMMStudyBuilder builder, Document document) {
    docElementParser.getFirstElement(document, IDENTIFIER_XPATH)
            .ifPresent((Element element) -> builder.studyNumber(element.getText()));
  }

  /**
   * Parses Abstract
   * <p>
   * Xpath = {@value OaiPmhConstants#ABSTRACT_XPATH }
   */
  public void parseAbstract(CMMStudy.CMMStudyBuilder builder, Document document, OaiPmh config, String langCode) {
    Map<String, String> abstracts = parseLanguageContentOfElement(document, config, langCode, ABSTRACT_XPATH, true);
    builder.abstractField(abstracts);
  }

  /**
   * Parses Year of Publication from:
   * <p>
   * Xpath = {@value OaiPmhConstants#YEAR_OF_PUB_XPATH }
   */
  public void parseYrOfPublication(CMMStudy.CMMStudyBuilder builder, Document document) {
    Optional<Attribute> yrOfPublicationDate = docElementParser.getFirstAttribute(document, YEAR_OF_PUB_XPATH);
    yrOfPublicationDate.ifPresent(attribute -> builder.publicationYear(attribute.getValue()));
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PID_STUDY_XPATH }
   */
  public void parsePidStudies(CMMStudy.CMMStudyBuilder builder, Document document, OaiPmh oaiPmh, String defaultLangIsoCode) {
    builder.pidStudies(docElementParser.extractMetadataObjectListForEachLang(
            oaiPmh, defaultLangIsoCode, document, PID_STUDY_XPATH, pidStrategyFunction()));
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CREATORS_XPATH }
   */
  public void parseCreator(CMMStudy.CMMStudyBuilder builder, Document doc, OaiPmh config, String defaultLangIsoCode) {

    Map<String, List<String>> creatorsInLangs = docElementParser.extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, doc, CREATORS_XPATH, creatorStrategyFunction());

    builder.creators(creatorsInLangs);
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CLASSIFICATIONS_XPATH }
   */
  public void parseClassifications(CMMStudy.CMMStudyBuilder builder, Document doc, OaiPmh config, String defaultLangIsoCode) {
    builder.classifications(
            docElementParser.extractMetadataObjectListForEachLang(
                    config, defaultLangIsoCode, doc, CLASSIFICATIONS_XPATH,
                    termVocabAttributeStrategyFunction(false)));
  }

  /**
   * Parses parseKeyword(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#KEYWORDS_XPATH }
   */
  public void parseKeywords(CMMStudy.CMMStudyBuilder builder, Document doc, OaiPmh config, String defaultLangIsoCode) {
    builder.keywords(
            docElementParser.extractMetadataObjectListForEachLang(
                    config, defaultLangIsoCode, doc, KEYWORDS_XPATH, termVocabAttributeStrategyFunction(false)));
  }

  /**
   * Parses Type Of Time Method(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_TIME_METHOD_XPATH }
   */
  public void parseTypeOfTimeMethod(CMMStudy.CMMStudyBuilder builder, Document doc, OaiPmh config, String defaultLangIsoCode) {

    builder.typeOfTimeMethods(
            docElementParser.extractMetadataObjectListForEachLang(
                    config, defaultLangIsoCode, doc, TYPE_OF_TIME_METHOD_XPATH,
                    termVocabAttributeStrategyFunction(true)));
  }

  /**
   * Parses Type Of Mode Of Collection(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_MODE_OF_COLLECTION_XPATH }
   */
  public void parseTypeOfModeOfCollection(CMMStudy.CMMStudyBuilder builder, Document doc, OaiPmh config, String defaultLangIsoCode) {
    builder.typeOfModeOfCollections(
            docElementParser.extractMetadataObjectListForEachLang(
                    config, defaultLangIsoCode, doc, TYPE_OF_MODE_OF_COLLECTION_XPATH,
                    termVocabAttributeStrategyFunction(true)));
  }

  /**
   * Parses Unit Type(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#UNIT_TYPE_XPATH }
   */
  public void parseUnitTypes(CMMStudy.CMMStudyBuilder builder, Document document, OaiPmh config, String defaultLangIsoCode) {
    builder.unitTypes(
            docElementParser.extractMetadataObjectListForEachLang(
                    config, defaultLangIsoCode, document, UNIT_TYPE_XPATH, termVocabAttributeStrategyFunction(true)));
  }

  /**
   * Parses Type Of Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_SAMPLING_XPATH }
   */
  public void parseTypeOfSamplingProcedure(CMMStudy.CMMStudyBuilder builder, Document doc, OaiPmh config, String defaultLangIsoCode) {
    builder.typeOfSamplingProcedures(
            docElementParser.extractMetadataObjectListForEachLang(config, defaultLangIsoCode, doc, TYPE_OF_SAMPLING_XPATH,
                    samplingTermVocabAttributeStrategyFunction(true)));
  }

  /**
   * Parses area Countries covered by a study:
   * <p>
   * Xpath = {@value OaiPmhConstants#STUDY_AREA_COUNTRIES_XPATH }
   */
  public void parseStudyAreaCountries(CMMStudy.CMMStudyBuilder builder, Document document, OaiPmh config, String defaultLangIsoCode) {
    builder.studyAreaCountries(docElementParser.extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, document, STUDY_AREA_COUNTRIES_XPATH, countryStrategyFunction()));
  }

  /**
   * Parse Publisher from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PUBLISHER_XPATH } and
   * Xpath = {@value OaiPmhConstants#DISTRIBUTOR_XPATH }
   */
  public void parsePublisher(CMMStudy.CMMStudyBuilder builder, Document document, OaiPmh config, String defaultLang) {
    Map<String, Publisher> producerPathMap = docElementParser.extractMetadataObjectForEachLang(config, defaultLang, document,
            PUBLISHER_XPATH, publisherStrategyFunction());
    Map<String, Publisher> distrPathMap = docElementParser.extractMetadataObjectForEachLang(config, defaultLang, document,
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
  public void parseStudyTitle(CMMStudy.CMMStudyBuilder builder, Document document, OaiPmh config, String langCode) {

    Map<String, String> titles = parseLanguageContentOfElement(document, config, langCode, TITLE_XPATH, false);

    // https://bitbucket.org/cessda/cessda.cdc.version2/issues/135
    if (!titles.isEmpty()) {
      Map<String, String> parTitles =
              parseLanguageContentOfElement(document, config, langCode, PAR_TITLE_XPATH, false);
      parTitles.forEach(titles::putIfAbsent);  // parTitl lang must not be same as or override titl lang
    }
    HTMLFilter.cleanMapValues(titles);
    builder.titleStudy(titles);
  }

  private Map<String, String> parseLanguageContentOfElement(Document document, OaiPmh config, String langCode, String titleXpath, boolean isConcatenating) {
    List<Element> elements = docElementParser.getElements(document, titleXpath);
    return getLanguageKeyValuePairs(config, elements, isConcatenating, langCode, Element::getText);
  }

  /**
   * Parses parse Study Url from two plausible allowed xPaths
   * <p>
   * Xpath = {@value OaiPmhConstants#STUDY_URL_DOC_DSCR_XPATH }
   * Xpath = {@value OaiPmhConstants#STUDY_URL_STDY_DSCR_XPATH }
   */
  public void parseStudyUrl(CMMStudy.CMMStudyBuilder builder, Document document,
                            OaiPmh config, String langCode) {

    List<Element> docDscrElement = docElementParser.getElements(document, STUDY_URL_DOC_DSCR_XPATH);
    List<Element> stdyDscrElements = docElementParser.getElements(document, STUDY_URL_STDY_DSCR_XPATH);
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
  public void parseSamplingProcedureFreeTexts(CMMStudy.CMMStudyBuilder builder, Document doc, OaiPmh config, String defaultLangIsoCode) {
    builder.samplingProcedureFreeTexts(
            docElementParser.extractMetadataObjectListForEachLang(
                    config, defaultLangIsoCode, doc, SAMPLING_XPATH, nullableElementValueStrategyFunction()));
  }

  /**
   * Parses Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#SAMPLING_XPATH }
   */
  public void parseDataAccessFreeText(CMMStudy.CMMStudyBuilder builder, Document doc, OaiPmh config, String defaultLangIsoCode) {
    Map<String, List<String>> restrictionLanguageMap = docElementParser.extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, doc, DATA_RESTRCTN_XPATH, nullableElementValueStrategyFunction());
    builder.dataAccessFreeTexts(restrictionLanguageMap);
  }

  /**
   * Parses Data Collection Period dates from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH}
   * <p>
   * For Data Collection start and end date plus the four digit Year value as Data Collection Year
   */
  public void parseDataCollectionDates(CMMStudy.CMMStudyBuilder builder, Document doc) {
    Map<String, String> dateAttrs = docElementParser.getDateElementAttributesValueMap(doc, DATA_COLLECTION_PERIODS_PATH);

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
  public void parseDataCollectionFreeTexts(CMMStudy.CMMStudyBuilder builder, Document document, OaiPmh config, String defaultLangIsoCode) {
    builder.dataCollectionFreeTexts(docElementParser.extractMetadataObjectListForEachLang(
            config, defaultLangIsoCode, document, DATA_COLLECTION_PERIODS_PATH, dataCollFreeTextStrategyFunction()));
  }

  /**
   * Parses File Language(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#FILE_TXT_LANGUAGES_XPATH }
   */
  public void parseFileLanguages(CMMStudy.CMMStudyBuilder builder, Document document) {
    String[] fileTxtAttrs = docElementParser.getAttributeValues(document, FILE_TXT_LANGUAGES_XPATH);
    String[] fileNameAttrs = docElementParser.getAttributeValues(document, FILENAME_LANGUAGES_XPATH);
    String[] allLangs = Stream.of(fileTxtAttrs, fileNameAttrs).flatMap(Stream::of).toArray(String[]::new);
    Set<String> languageFilesSet = Arrays.stream(allLangs).collect(Collectors.toSet());
    builder.fileLanguages(languageFilesSet);
  }
}