package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.dao.GetRecordDoa;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
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

    InputStream recordXMLStream = IOUtils.toInputStream(recordXML, Charsets.UTF_8);
    SAXBuilder saxBuilder = new SAXBuilder();
    Document document = saxBuilder.build(recordXMLStream);

    parseHeaderElement(builder, document, X_FACTORY);
    parseStudyCitationElement(builder, document, X_FACTORY);
    parseStudyInfoElement(builder, document, X_FACTORY);
  }

  private void parseHeaderElement(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    XPathExpression<Element> expr2 = xFactory.compile(IDENTIFIER_XPATH, Filters.element(), null, OAI_NS);
    Element identifier = expr2.evaluateFirst(document);
    builder.studyNumber(identifier.getValue());
  }

  /**
   * Extracts all the CMM fields under path /codeBook/stdyDscr/citation/<xxx> .
   */
  private void parseStudyCitationElement(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
    XPathExpression<Element> xPathExpression = xFactory.compile(STUDY_CITATION_XPATH, Filters.element(), null, OAI_AND_DDI_NS);
    Element citationElement = xPathExpression.evaluateFirst(document);

    List<Element> citationElementChildren = citationElement.getChildren();
    for (Element citationElementChild : citationElementChildren) {
      String citationElementName = citationElementChild.getName();

      if (TITLE_STMT.equalsIgnoreCase(citationElementName)) {
        processTitleStmt(builder, citationElementChild, handlerConfig);
      }
    }
  }

  /**
   * Lets process /codeBook/stdyDscr/citation/titlStmt .
   */
  private static void processTitleStmt(CMMStudy.CMMStudyBuilder builder, Element citationElementChild, PaSCHandlerOaiPmhConfig handlerConfig) {
    List<Element> titles = citationElementChild.getChildren(TITLE, DDI_NS);

    Map<String, String> titlesMap = new HashMap<>();
    for (Element title : titles) {

      if (null != title.getAttribute(LANG, XML_NS) && !title.getAttribute(LANG, XML_NS).getValue().isEmpty()) {
        titlesMap.put(title.getAttribute(LANG, XML_NS).getValue(), title.getValue());
      } else if (handlerConfig.getOaiPmh().getMetadataParsingDefaultLang().isActive()) {
        titlesMap.put(handlerConfig.getOaiPmh().getMetadataParsingDefaultLang().getLang(), title.getValue());
      } else {
        titlesMap.put(UNKNOWN_LANG, title.getValue()); // UNKNOWN_LANG(XX) = to signify title for non specified lang tag
      }
    }
    builder.titleStudy(titlesMap);
  }

  private void parseStudyInfoElement(CMMStudy.CMMStudyBuilder builder, Document document, XPathFactory xFactory) {
  }
}
