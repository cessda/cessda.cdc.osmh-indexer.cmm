package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.List;
import java.util.Map;

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
    throw new IllegalStateException("Utility class, instantiation not allow");
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
   */
  public static void parseHeaderElement(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    parseStudyNumber(builder, document, xFactory);
    parseLastModified(builder, document, xFactory);
    parseRecordStatus(builder, document, xFactory);
  }

  /**
   * Pass records status.
   * <p>
   * Defaults to false at initialisation. No need for set false on an else block.
   */
  private static void parseRecordStatus(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    XPathExpression<Attribute> attributeExpression = xFactory.compile(RECORD_STATUS_XPATH, Filters.attribute(), null, OAI_NS);
    Attribute status = attributeExpression.evaluateFirst(document);
    if (null == status || !"deleted".equalsIgnoreCase(status.getValue())) builder.active(true);
  }

  /**
   * Parse last Modified.
   */
  private static void parseLastModified(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    XPathExpression<Element> elementExpression = xFactory.compile(LAST_MODIFIED_DATE_XPATH, Filters.element(), null, OAI_NS);
    Element element = elementExpression.evaluateFirst(document);
    if (null != element) builder.lastModified(element.getValue());
  }

  /**
   * Parse study number.
   */
  private static void parseStudyNumber(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    XPathExpression<Element> elementExpression = xFactory.compile(IDENTIFIER_XPATH, Filters.element(), null, OAI_NS);
    Element element = elementExpression.evaluateFirst(document);
    if (null != element) builder.studyNumber(element.getValue());
  }

  /**
   * Parses Study Title.
   * <p>
   * Xpath = {@value OaiPmhConstants#TITLE_XPATH }
   */
  public static void parseStudyTitle(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, TITLE_XPATH);
    Map<String, String> studyTitles = getLanguageKeyValuePairs(config, elements);
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
    Map<String, String> abstracts = getLanguageKeyValuePairs(config, elements);
    builder.abstractField(abstracts);
  }


  /**
   * Parses Year of Publication from:
   * <p>
   * Xpath = {@value OaiPmhConstants#YEAR_OF_PUB_XPATH }
   */
  public static void parseYrOfPublication(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    Element yrOfPublicationDate = getFirstElements(document, xFactory, YEAR_OF_PUB_XPATH);
    if (null != yrOfPublicationDate){
      try {
        builder.publicationYear(Integer.parseInt(yrOfPublicationDate.getValue()));
      } catch (NumberFormatException e) {
        log.warn("Could not parse year to Int. Defaulting to 1970");
        builder.publicationYear(config.getPublicationYearDefault());
      }
    }
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
   * Xpath = {@value OaiPmhConstants#CLASSIFICATIONS_XPATH }
   */
  public static void parseClassifications(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] classifications = getElementValues(document, xFactory, CLASSIFICATIONS_XPATH);
    builder.classifications(classifications);
  }

  /**
   * Parses parseKeyword(s) from:
   * <p>
   * Xpath = {@value OaiPmhConstants#KEYWORDS_XPATH }
   */
  public static void parseKeywords(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    String[] keywords = getElementValues(document, xFactory, KEYWORDS_XPATH);
    builder.keywords(keywords);
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
    Map<String, String> languageKeyValuePairs = getLanguageKeyValuePairs(config, elements);
    builder.samplingProcedure(languageKeyValuePairs);
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
   * Xpath = {@value OaiPmhConstants#PERSON_NAME_XPATH }
   */
  public static void parsePersonName(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    List<Element> people = getElements(document, xFactory, PERSON_NAME_XPATH);
    people.stream().findFirst().ifPresent(element -> builder.personName(element.getValue()));
  }

  /**
   * Access Class Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#ACCESS_CLASS_XPATH }
   */
  public static void parseAccessClass(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    List<Element> accessClasses = getElements(document, xFactory, ACCESS_CLASS_XPATH);
    accessClasses.stream().findFirst().ifPresent(element -> builder.accessClass(element.getValue()));
  }

  /**
   * Access Class Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PUBLISHER_XPATH }
   */
  public static void parsePublisher(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    List<Element> accessClasses = getElements(document, xFactory, PUBLISHER_XPATH);
    accessClasses.stream().findFirst().ifPresent(element -> builder.publisher(element.getValue()));
  }

  /**
   * Parses Person Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_ACCESS_XPATH }
   */
  public static void parseDataAccess(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, DATA_ACCESS_XPATH);
    Map<String, String> valuePairs = getLanguageKeyValuePairs(config, elements);
    builder.dataAccess(valuePairs);
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
    Map<String, String> titlesMap = getLanguageKeyValuePairs(config, elements);
    builder.institutionFullName(titlesMap);
  }
}
