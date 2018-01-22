package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.dao.GetRecordDoa;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;

/**
 * @author moses@doraventures.com
 */
@Service
@Slf4j
public class GetRecordServiceImpl implements GetRecordService {

  @Autowired
  GetRecordDoa getRecordDoa;

  @Autowired
  PaSCHandlerOaiPmhConfig handlerConfig;

  private static final XPathFactory X_FACTORY = XPathFactory.instance();

  @Override
  public CMMStudy getRecord(String repository, String studyId) throws InternalSystemException {
    CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
    String recordXML = getRecordDoa.getRecordXML(repository, studyId);

    try {
      mapDDIRecordToCMMStudy(recordXML, builder);
    } catch (JDOMException | IOException e) {
      throw new InternalSystemException("Unable to parse xml :" + e.getMessage());
    }
    return builder.build();
  }

  private void mapDDIRecordToCMMStudy(String recordXML, CMMStudy.CMMStudyBuilder builder)
      throws JDOMException, IOException {

    OaiPmh oaiPmh = handlerConfig.getOaiPmh();
    InputStream recordXMLStream = IOUtils.toInputStream(recordXML, Charsets.UTF_8);
    SAXBuilder saxBuilder = new SAXBuilder();
    Document document = saxBuilder.build(recordXMLStream);

    parseHeaderElement(builder, document, X_FACTORY);
    parseStudyCitationElement(builder, document, X_FACTORY);
    parseAbstract(builder, document, X_FACTORY, oaiPmh);
    parseYrOfPublication(builder, document, X_FACTORY, oaiPmh);
    parsePidStudies(builder, document, X_FACTORY);
    parsePersonName(builder, document, X_FACTORY);
    parseAccessClass(builder, document, X_FACTORY);
    parseDataAccess(builder, document, X_FACTORY, oaiPmh);
    parseDataCollectionPeriods(builder, document, X_FACTORY);
    parseInstitutionFullName(builder, document, X_FACTORY, oaiPmh);


//    parseTypeOfModeOfCollections(builder, document, X_FACTORY);

    //Access Class
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
  private void parseHeaderElement(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    XPathExpression<Element> expr2 = xFactory.compile(IDENTIFIER_XPATH, Filters.element(), null, OAI_NS);
    Element identifier = expr2.evaluateFirst(document);
    builder.studyNumber(identifier.getValue());
  }

  /**
   * Extracts all the CMM fields under path {@value OaiPmhConstants#STUDY_CITATION_XPATH } .
   */
  private void parseStudyCitationElement(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    XPathExpression<Element> xPathExpression = xFactory
        .compile(STUDY_CITATION_XPATH, Filters.element(), null, OAI_AND_DDI_NS);
    Element citationElement = xPathExpression.evaluateFirst(document);

    List<Element> citationElementChildren = citationElement.getChildren();
    for (Element citationElementChild : citationElementChildren) {
      String citationElementName = citationElementChild.getName();

      if (TITLE_STMT.equalsIgnoreCase(citationElementName)) {
        processTitleStmt(builder, citationElementChild, handlerConfig.getOaiPmh());
        continue; //FIXME: use xpath to extract title by itself alone...
      }
    }
  }

  /**
   * Processes elements under /codeBook/stdyDscr/citation/titlStmt .
   * FIXME: Use Xpath
   */
  private static void processTitleStmt(CMMStudy.CMMStudyBuilder builder, Element citationElementChild, OaiPmh config) {

    //Processes elements under /codeBook/stdyDscr/citation/titlStmt/titl
    List<Element> titles = citationElementChild.getChildren(TITLE, DDI_NS);
    Map<String, String> titlesMap = getLanguageKeyValuePairs(config, titles);
    builder.titleStudy(titlesMap);
  }


  /**
   * parses Abstract
   * <p>
   * Xpath = {@value OaiPmhConstants#ABSTRACT_XPATH }
   */
  private static void parseAbstract(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, ABSTRACT_XPATH);
    Map<String, String> titlesMap = getLanguageKeyValuePairs(config, elements);
    builder.abstractField(titlesMap);
  }


  /**
   * parses Year of Publication from:
   * <p>
   * Xpath = {@value OaiPmhConstants#YEAR_OF_PUB_XPATH }
   */
  private static void parseYrOfPublication(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, YEAR_OF_PUB_XPATH);
    elements.stream().findFirst().ifPresent(distDateElement -> {
      try {
        builder.publicationYear(Integer.parseInt(distDateElement.getValue()));
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
  private static void parsePidStudies(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {

    List<Element> elements = getElements(document, xFactory, PID_STUDY_XPATH);
    String[] pidStudies = elements.stream().map(Element::getValue).toArray(String[]::new);
    builder.pidStudies(pidStudies);
  }

  /**
   * Parses Person Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#PERSON_NAME_XPATH }
   */
  private static void parsePersonName(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {

    List<Element> elements = getElements(document, xFactory, PERSON_NAME_XPATH);
    elements.stream().findFirst().ifPresent(element -> builder.personName(element.getValue()));
  }

  /**
   * Access Class Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#ACCESS_CLASS_XPATH }
   */
  private static void parseAccessClass(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {

    List<Element> elements = getElements(document, xFactory, ACCESS_CLASS_XPATH);
    elements.stream().findFirst().ifPresent(element -> builder.accessClass(element.getValue()));
  }


  /**
   * Parses Person Name from:
   * <p>
   * Xpath = {@value OaiPmhConstants#DATA_ACCESS_XPATH }
   */
  private static void parseDataAccess(
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
  private static void parseDataCollectionPeriods(
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
  private static void parseInstitutionFullName(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    List<Element> elements = getElements(document, xFactory, INST_FULL_NAME_XPATH);
    Map<String, String> titlesMap = getLanguageKeyValuePairs(config, elements);
    builder.institutionFullName(titlesMap);
  }

  private static List<Element> getElements(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return expression.evaluate(document);
  }

  /**
   * Parses value of given Element for every given xml@lang attributed.
   * <p>
   * If no lang is found attempts to default to a configured xml@lang
   */
  private static Map<String, String> getLanguageKeyValuePairs(OaiPmh config, List<Element> elements) {

    Map<String, String> titlesMap = new HashMap<>();

    for (Element element : elements) {
      if (null != element.getAttribute(LANG_ATTR, XML_NS) && !element.getAttribute(LANG_ATTR, XML_NS).getValue().isEmpty()) {
        titlesMap.put(element.getAttribute(LANG_ATTR, XML_NS).getValue(), element.getValue());
      } else if (config.getMetadataParsingDefaultLang().isActive()) {
        titlesMap.put(config.getMetadataParsingDefaultLang().getLang(), element.getValue());
      } else {
        titlesMap.put(UNKNOWN_LANG, element.getValue()); // UNKNOWN_LANG(XX)
      }
    }
    return titlesMap;
  }
}
