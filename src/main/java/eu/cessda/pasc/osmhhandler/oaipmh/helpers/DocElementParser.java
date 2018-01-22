package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;

/**
 * Helper methods for extracting values from a {@link org.jdom2.Document }
 *
 * @author moses@doraventures.com
 */
public class DocElementParser {

  private DocElementParser() throws RuntimeException {
    throw new IllegalStateException("Utility class, instantiation not allow");
  }

  /**
   * Parses the array values of elements
   *
   * @param document the document to parse
   * @param xFactory the xFactory
   * @param classificationsXpath the Element parent node to retrieve
   * @return Array String Values of the Element
   */
  public static String[] getElementValues(Document document, XPathFactory xFactory, String classificationsXpath) {
    List<Element> elements = getElements(document, xFactory, classificationsXpath);
    return elements.stream().map(Element::getValue).toArray(String[]::new);
  }

  public static List<Element> getElements(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return expression.evaluate(document);
  }


  /**
   * Parses the array values of attributes of a given elements
   *
   * @param document the document to parse
   * @param xFactory the xFactory
   * @param classificationsXpath the Element parent node to retrieve
   * @return Array String Values of the attributes
   */
  public static String[] getAttributeValues(Document document, XPathFactory xFactory, String classificationsXpath) {
    List<Attribute> attributes = getAttributes(document, xFactory, classificationsXpath);
    return attributes.stream().map(Attribute::getValue).toArray(String[]::new);
  }

  private static List<Attribute> getAttributes(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Attribute> expression = xFactory.compile(xPathToElement, Filters.attribute(), null, DDI_NS);
    return expression.evaluate(document);
  }

  /**
   * Parses value of given Element for every given xml@lang attributed.
   * <p>
   * If no lang is found attempts to default to a configured xml@lang
   */
  public static Map<String, String> getLanguageKeyValuePairs(OaiPmh config, List<Element> elements) {

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
