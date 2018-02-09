package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.TermVocabAttributes;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;

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
    XPathExpression<Attribute> attributeExpression = xFactory.compile(RECORD_STATUS_XPATH, Filters.attribute(), null, OAI_NS);
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
            statusBuilder.hasError(true).message(element.getAttributeValue(CODE_ATTR) + ": " + element.getValue())
        );

    return statusBuilder.build();
  }

  /**
   * Parse last Modified.
   */
  private static void parseLastModified(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    getFirstElement(document, xFactory, LAST_MODIFIED_DATE_XPATH)
        .ifPresent((Element element) -> builder.lastModified(element.getValue()));
  }

  /**
   * Parse study number.
   */
  private static void parseStudyNumber(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    getFirstElement(document, xFactory, IDENTIFIER_XPATH)
        .ifPresent((Element element) -> builder.studyNumber(element.getValue()));
  }

  /**
   * Access Class Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#ACCESS_CLASS_XPATH }
   */
  public static void parseAccessClass(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    getFirstElement(document, xFactory, ACCESS_CLASS_XPATH)
        .ifPresent(element -> builder.accessClass(element.getValue()));
  }

  /**
   * Parse Publisher from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PUBLISHER_XPATH }
   */
  public static void parsePublisher(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    getFirstElement(document, xFactory, PUBLISHER_XPATH)
        .ifPresent(element -> builder.publisher(element.getValue()));
  }

  /**
   * Parses Study Title.
   * <p>
   * Xpath = {@value OaiPmhConstants#TITLE_XPATH }
   */
  public static void parseStudyTitle(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, TITLE_XPATH);
    Map<String, String> studyTitles = getLanguageKeyValuePairs(config, elements, false);
    builder.titleStudy(studyTitles);
  }

  /**
   * Parses Abstract
   * <p>
   * Xpath = {@value OaiPmhConstants#ABSTRACT_XPATH }
   */
  public static void parseAbstract(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, ABSTRACT_XPATH);
    Map<String, String> abstracts = getLanguageKeyValuePairs(config, elements, true);
    builder.abstractField(abstracts);
  }

  /**
   * Parses Year of Publication from:
   * <p>
   * Xpath = {@value OaiPmhConstants#YEAR_OF_PUB_XPATH }
   */
  public static void parseYrOfPublication(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    Optional<Element> yrOfPublicationDate = getFirstElement(document, xFactory, YEAR_OF_PUB_XPATH);
    yrOfPublicationDate.ifPresent((Element element) -> {
      try {
        builder.publicationYear(Integer.parseInt(element.getValue()));
      } catch (NumberFormatException e) {
        log.warn("Could not parse year to Int. Defaulting to 1970");
        builder.publicationYear(config.getPublicationYearDefault());
      }
    });
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PID_STUDY_XPATH }
   */
  public static void parsePidStudies(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] pidStudies = getElementValues(document, xFactory, PID_STUDY_XPATH);
    builder.pidStudies(pidStudies);
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CREATORS_XPATH }
   */
  public static void parseCreator(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {

    List<Element> elements = getElements(document, xFactory, CREATORS_XPATH);
    String[] myFinalList = elements.stream()
        .filter(Objects::nonNull)
        .map(DocElementParser::extractCreatorWithAffiliation)
        .toArray(String[]::new);

    builder.creators(myFinalList);
  }

  /**
   * Parses PID Study(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#CLASSIFICATIONS_XPATH }
   */
  public static void parseClassifications(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {
    List<Element> classificationsElements = getElements(document, xFactory, CLASSIFICATIONS_XPATH);
    Map<String, List<TermVocabAttributes>> langClassifications = extractTermVocabAttributes(config, classificationsElements);
    builder.classifications(langClassifications);
  }

  /**
   * Parses parseKeyword(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#KEYWORDS_XPATH }
   */
  public static void parseKeywords(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {
    List<Element> keywordElements = getElements(document, xFactory, KEYWORDS_XPATH);
    Map<String, List<TermVocabAttributes>> termVocabAttributes = extractTermVocabAttributes(config, keywordElements);
    builder.keywords(termVocabAttributes);
  }

  /**
   * Parses Type Of Time Method(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_TIME_METHOD_XPATH }
   */
  public static void parseTypeOfTimeMethod(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] typeOfTimeMethods = getElementValues(document, xFactory, TYPE_OF_TIME_METHOD_XPATH);
    builder.typeOfTimeMethods(typeOfTimeMethods);
  }

  /**
   * Parses Type Of Time Method(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#STUDY_AREA_COUNTRIES_XPATH }
   */
  public static void parseStudyAreaCountries(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] typeOfTimeMethods = getElementValues(document, xFactory, STUDY_AREA_COUNTRIES_XPATH);
    builder.studyAreaCountries(typeOfTimeMethods);
  }

  /**
   * Parses Type Of Time Method(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#UNIT_TYPE_XPATH }
   */
  public static void parseUnitTypes(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] typeOfTimeMethods = getElementValues(document, xFactory, UNIT_TYPE_XPATH);
    builder.unitTypes(typeOfTimeMethods);
  }

  /**
   * Parses Type Of Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_SAMPLING_XPATH }
   */
  public static void parseTypeOfSamplingProcedure(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] elementValues = getElementValues(document, xFactory, TYPE_OF_SAMPLING_XPATH);
    builder.typeOfSamplingProcedures(elementValues);
  }

  /**
   * Parses Sampling Procedure(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#SAMPLING_XPATH }
   */
  public static void parseSamplingProcedure(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, SAMPLING_XPATH);
    Map<String, String> samplingProcedures = getLanguageKeyValuePairs(config, elements, false);
    builder.samplingProcedure(samplingProcedures);
  }

  /**
   * Parses Type Of Mode Of Collection(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#TYPE_OF_MODE_OF_COLLECTION_XPATH }
   */
  public static void parseTypeOfModeOfCollection(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] elementValues = getElementValues(document, xFactory, TYPE_OF_MODE_OF_COLLECTION_XPATH);
    builder.typeOfSamplingProcedures(elementValues);
  }

  /**
   * Parses Type Of Time Method(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#FILE_LANGUAGES_XPATH }
   */
  public static void parseFileLanguages(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] typeOfTimeMethods = getAttributeValues(document, xFactory, FILE_LANGUAGES_XPATH);
    builder.unitTypes(typeOfTimeMethods);
  }

  /**
   * Parses Person Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_ACCESS_XPATH }
   */
  public static void parseDataAccess(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, DATA_ACCESS_XPATH);
    Map<String, String> dataAccess = getLanguageKeyValuePairs(config, elements, false);
    builder.dataAccess(dataAccess);
  }

  /**
   * Parses Data Collection Period End date from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH }
   */
  public static void parseDataCollectionPeriods(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {

    List<Element> elements = getElements(document, xFactory, DATA_COLLECTION_PERIODS_PATH);
    for (Element element : elements) {
      if (SINGLE_ATTR.equalsIgnoreCase(element.getAttributeValue(EVENT_ATTR))) {
        builder.dataCollectionPeriodStartdate(element.getAttributeValue(DATE_ATTR));
      } else if (START_ATTR.equalsIgnoreCase(element.getAttributeValue(EVENT_ATTR))) {
        builder.dataCollectionPeriodStartdate(element.getAttributeValue(DATE_ATTR));
      } else if (END_ATTR.equalsIgnoreCase(element.getAttributeValue(EVENT_ATTR))) {
        builder.dataCollectionPeriodEnddate(element.getAttributeValue(DATE_ATTR));
      }
    }
  }

  /**
   * Parses parse Institution Full Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#INST_FULL_NAME_XPATH }
   */
  public static void parseInstitutionFullName(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, INST_FULL_NAME_XPATH);
    Map<String, String> institutionFullNames = getLanguageKeyValuePairs(config, elements, true);
    builder.institutionFullName(institutionFullNames);
  }
}
