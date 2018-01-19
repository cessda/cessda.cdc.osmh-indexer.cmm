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
    parseYrOfPublication(builder, document, X_FACTORY, oaiPmh);
    parsePidStudy(builder, document, X_FACTORY);
    parsePersonName(builder, document, X_FACTORY);
    parseInstitutionFullName(builder, document, X_FACTORY, oaiPmh);
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
    Map<String, String> titlesMap = getKeyValuePairsFromElementWithLang(config, titles);
    builder.titleStudy(titlesMap);
  }


  /**
   * parses Year of Publication from
   * <p>
   * Xpath = {@value OaiPmhConstants#YEAR_OF_PUB_XPATH }
   */
  private static void parseYrOfPublication(
      CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    XPathExpression<Element> xPathExpression = xFactory
        .compile(YEAR_OF_PUB_XPATH, Filters.element(), null, OAI_AND_DDI_NS);
    List<Element> distDateElements = xPathExpression.evaluate(document);

    for (Element distDateElement : distDateElements) {
      try {
        builder.publicationYear(Integer.parseInt(distDateElement.getValue()));
      } catch (NumberFormatException e) {
        log.warn("Could not parse year to Int. Defaulting to 1970");
        builder.publicationYear(config.getPublicationYearDefault());
      }
    }
  }

  /**
   * Parses PID Study(s) from
   * <p>
   * Xpath = {@value OaiPmhConstants#PID_STUDY_XPATH }
   */
  private static void parsePidStudy(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {

    XPathExpression<Element> expression = xFactory.compile(PID_STUDY_XPATH, Filters.element(), null, OAI_AND_DDI_NS);
    List<Element> pidStudyElements = expression.evaluate(document);
    String[] pidStudies = pidStudyElements.stream().map(Element::getValue).toArray(String[]::new);
    builder.pidStudies(pidStudies);
  }

  /**
   * Parses Person Name from
   * <p>
   * Xpath = {@value OaiPmhConstants#PERSON_NAME_XPATH }
   */
  private static void parsePersonName(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {

    XPathExpression<Element> expression = xFactory.compile(PERSON_NAME_XPATH, Filters.element(), null, OAI_AND_DDI_NS);
    List<Element> personNameElements = expression.evaluate(document);
    personNameElements.stream().findFirst().ifPresent(element -> builder.personName(element.getValue()));
  }

  /**
   * Parses parse Institution Full Name from
   * <p>
   * Xpath = {@value OaiPmhConstants#INST_FULL_NAME_XPATH }
   */
  private static void parseInstitutionFullName(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory, OaiPmh config) {

    XPathExpression<Element> expression = xFactory.compile(
        INST_FULL_NAME_XPATH, Filters.element(), null, OAI_AND_DDI_NS);

    List<Element> institutions = expression.evaluate(document);
    Map<String, String> titlesMap = getKeyValuePairsFromElementWithLang(config, institutions);
    builder.institutionFullName(titlesMap);
  }

  private static Map<String, String> getKeyValuePairsFromElementWithLang(OaiPmh config, List<Element> elements) {

    Map<String, String> titlesMap = new HashMap<>();

    for (Element element : elements) {
      if (null != element.getAttribute(LANG, XML_NS) && !element.getAttribute(LANG, XML_NS).getValue().isEmpty()) {
        titlesMap.put(element.getAttribute(LANG, XML_NS).getValue(), element.getValue());
      } else if (config.getMetadataParsingDefaultLang().isActive()) {
        titlesMap.put(config.getMetadataParsingDefaultLang().getLang(), element.getValue());
      } else {
        titlesMap.put(UNKNOWN_LANG, element.getValue()); // UNKNOWN_LANG(XX)
      }
    }
    return titlesMap;
  }
}
