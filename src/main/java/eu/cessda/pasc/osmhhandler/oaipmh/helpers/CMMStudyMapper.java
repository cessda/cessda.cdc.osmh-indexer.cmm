package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorStatus;
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
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.ParsingStrategies.*;

/**
 * Responsible for Mapping oai-pmh fields to a CMMStudy
 *
 * @author moses@doraventures.com
 */
@Slf4j
public class CMMStudyMapper {

  private CMMStudyMapper() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
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
   * @param xFactory the xFactory
   * @return true if record is active
   */
  public static boolean parseHeaderElement(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    parseStudyNumber(builder, document, xFactory);
    parseLastModified(builder, document, xFactory);
    return parseRecordStatus(builder, document, xFactory);
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
   * Checks if the record has an <error> element.
   *
   * @param document the document to map to.
   * @param xFactory the Path Factory.
   * @return ErrorStatus of the record.
   */
  public static ErrorStatus validateRecord(Document document, XPathFactory xFactory) {

    ErrorStatus.ErrorStatusBuilder statusBuilder = ErrorStatus.builder();
    getFirstElement(document, xFactory, ERROR_PATH)
        .ifPresent((Element element) ->
            statusBuilder.hasError(true).message(element.getAttributeValue(CODE_ATTR) + ": " + element.getText())
        );

    return statusBuilder.build();
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
   * Access Class Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#ACCESS_CLASS_XPATH }
   */
  public static void parseAccessClass(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    getFirstElement(document, xFactory, ACCESS_CLASS_XPATH)
        .ifPresent(element -> builder.accessClass(element.getText()));
  }

  /**
   * Parses Abstract
   * <p>
   * Xpath = {@value OaiPmhConstants#ABSTRACT_XPATH }
   */
  public static void parseAbstract(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, ABSTRACT_XPATH);
    Map<String, String> abstracts = getLanguageKeyValuePairs(config, elements, true, creatorStrategyFunction());
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
                                     OaiPmh oaiPmh) {
    builder.pidStudies(extractMetadataObjectListForEachLang(
        oaiPmh, document, xFactory, PID_STUDY_XPATH, pidStrategyFunction()));
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CREATORS_XPATH }
   */
  public static void parseCreator(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                  OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, CREATORS_XPATH);
    Map<String, String> creatorsInLangs = getLanguageKeyValuePairs(config, elements, false, creatorStrategyFunction());
    builder.creators(creatorsInLangs);
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CLASSIFICATIONS_XPATH }
   */
  public static void parseClassifications(CMMStudy.CMMStudyBuilder builder, Document doc, XPathFactory xFactory,
                                          OaiPmh config) {
    builder.classifications(
        extractMetadataObjectListForEachLang(
            config, doc, xFactory, CLASSIFICATIONS_XPATH, termVocabAttributeStrategyFunction()));
  }

  /**
   * Parses parseKeyword(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#KEYWORDS_XPATH }
   */
  public static void parseKeywords(CMMStudy.CMMStudyBuilder builder, Document doc, XPathFactory xFactory,
                                   OaiPmh config) {
    builder.keywords(
        extractMetadataObjectListForEachLang(
            config, doc, xFactory, KEYWORDS_XPATH, termVocabAttributeStrategyFunction()));
  }

  /**
   * Parses Type Of Time Method(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_TIME_METHOD_XPATH }
   */
  public static void parseTypeOfTimeMethod(CMMStudy.CMMStudyBuilder builder, Document doc, XPathFactory xFactory,
                                           OaiPmh config) {

    builder.typeOfTimeMethods(
        extractMetadataObjectListForEachLang(
            config, doc, xFactory, TYPE_OF_TIME_METHOD_XPATH, termVocabAttributeStrategyFunction()));
  }

  /**
   * Parses area Countries covered by a study:
   * <p>
   * Xpath = {@value OaiPmhConstants#STUDY_AREA_COUNTRIES_XPATH }
   */
  public static void parseStudyAreaCountries(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                             OaiPmh config) {
    builder.studyAreaCountries(extractMetadataObjectListForEachLang(
        config, document, xFactory, STUDY_AREA_COUNTRIES_XPATH, countryStrategyFunction()));
  }

  /**
   * Parse Publisher from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PUBLISHER_XPATH }
   */
  public static void parsePublisher(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                    OaiPmh config) {
    builder.publisher(extractMetadataObjectForEachLang(
        config, document, xFactory, PUBLISHER_XPATH, publisherStrategyFunction()));
  }

  /**
   * Parses Study Title.
   * <p>
   * Xpath = {@value OaiPmhConstants#TITLE_XPATH }
   */
  public static void parseStudyTitle(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, TITLE_XPATH);
    Map<String, String> studyTitles = getLanguageKeyValuePairs(config, elements, false, rawTextStrategyFunction());
    builder.titleStudy(studyTitles);
  }

  /**
   * Parses Unit Type(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#UNIT_TYPE_XPATH }
   */
  public static void parseUnitTypes(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                    OaiPmh config) {
    builder.unitTypes(
        extractMetadataObjectListForEachLang(
            config, document, xFactory, UNIT_TYPE_XPATH, termVocabAttributeStrategyFunction()));
  }

  /**
   * Parses Type Of Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_SAMPLING_XPATH }
   */
  public static void parseTypeOfSamplingProcedure(CMMStudy.CMMStudyBuilder builder, Document doc,
                                                  XPathFactory xFactory, OaiPmh config) {
    builder.typeOfSamplingProcedures(
        extractMetadataObjectListForEachLang(
            config, doc, xFactory, TYPE_OF_SAMPLING_XPATH, samplingTermVocabAttributeStrategyFunction()));
  }

  /**
   * Parses Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#SAMPLING_XPATH }
   */
  public static void parseSamplingProcedureFreeTexts(CMMStudy.CMMStudyBuilder builder, Document doc,
                                                     XPathFactory xFactory, OaiPmh config) {
    builder.samplingProcedureFreeTexts(
        extractMetadataObjectListForEachLang(config, doc, xFactory, SAMPLING_XPATH, samplingProcStrategyFunction()));
  }

  /**
   * Parses Type Of Mode Of Collection(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_MODE_OF_COLLECTION_XPATH }
   */
  public static void parseTypeOfModeOfCollection(CMMStudy.CMMStudyBuilder builder, Document doc,
                                                 XPathFactory xFactory, OaiPmh config) {
    builder.typeOfModeOfCollections(
        extractMetadataObjectListForEachLang(
            config, doc, xFactory, TYPE_OF_MODE_OF_COLLECTION_XPATH, termVocabAttributeStrategyFunction()));
  }

  /**
   * Parses Data Collection Period dates from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH}
   */
  public static void parseDataCollectionDates(CMMStudy.CMMStudyBuilder builder, Document doc,
                                              XPathFactory xFactory) {
    Map<String, String> dateAttrs = getDateElementAttributesValueMap(doc, xFactory, DATA_COLLECTION_PERIODS_PATH);

    if (dateAttrs.containsKey(SINGLE_ATTR)) {
      builder.dataCollectionPeriodStartdate(dateAttrs.get(SINGLE_ATTR));
    } else {
      if (dateAttrs.containsKey(START_ATTR)) {
        builder.dataCollectionPeriodStartdate(dateAttrs.get(START_ATTR));
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
  public static void parseDataCollectionFreeTexts(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory,
                                                  OaiPmh config) {
    builder.dataCollectionFreeTexts(extractMetadataObjectListForEachLang(
        config, document, xFactory, DATA_COLLECTION_PERIODS_PATH, dataCollFreeTextStrategyFunction()));
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

  /**
   * Parses Data Access from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_ACCESS_XPATH }
   */
  public static void parseDataAccess(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, DATA_ACCESS_XPATH);
    Map<String, String> dataAccess = getLanguageKeyValuePairs(config, elements, false, creatorStrategyFunction());
    builder.dataAccess(dataAccess);
  }
}