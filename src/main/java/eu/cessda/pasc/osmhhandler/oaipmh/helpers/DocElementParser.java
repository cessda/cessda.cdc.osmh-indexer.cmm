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
class DocElementParser {

  private DocElementParser() throws RuntimeException {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  /**
   * Parses the array values of elements
   *
   * @param document             the document to parse
   * @param xFactory             the xFactory
   * @param classificationsXpath the Element parent node to retrieve
   * @return String[] Values of the Element
   */
  static String[] getElementValues(Document document, XPathFactory xFactory, String classificationsXpath) {
    List<Element> elements = getElements(document, xFactory, classificationsXpath);
    return elements.stream().map(Element::getValue).toArray(String[]::new);
  }

  static List<Element> getElements(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return expression.evaluate(document);
  }

  static Element getFirstElements(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return expression.evaluateFirst(document);
  }

  /**
   * Parses the array values of attributes of a given elements
   *
   * @param document             the document to parse
   * @param xFactory             the xFactory
   * @param classificationsXpath the Element parent node to retrieve
   * @return Array String Values of the attributes
   */
  static String[] getAttributeValues(Document document, XPathFactory xFactory, String classificationsXpath) {
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
   * If no lang is found attempts to default to a configured xml@lang.
   * <p>
   * If configuration is set to not default to a given lang, effect is this element is not extracted.
   */
  static Map<String, String> getLanguageKeyValuePairs(OaiPmh config, List<Element> elements, boolean isConcatenating) {

    Map<String, String> titlesMap = new HashMap<>();
    for (Element element : elements) {
      if (null != element.getAttribute(LANG_ATTR, XML_NS) && !element.getAttribute(LANG_ATTR, XML_NS).getValue().isEmpty()) {
        titlesMap.put(element.getAttribute(LANG_ATTR, XML_NS).getValue(), element.getValue());
      } else if (config.getMetadataParsingDefaultLang().isActive()) {
        if (isConcatenating && config.isConcatenateRepeatedElements()) {
          concatenateRepeatedElements(config, titlesMap, element);
        } else {
          titlesMap.put(config.getMetadataParsingDefaultLang().getLang(), element.getValue()); // Else keep overriding
        }
      }
    }
    return titlesMap;
  }

  /**
   * TODO: remove/raise questions!
   * <p>
   * Temp request from PUG to concatenate repeated elements.  This is to be removed once SPs start tagging their data
   * with the xml:lang tags.  This will then be extracted to key (lang e.g. en) : value (abstract content).
   * <p>
   * Current toggle is osmhhandler.oaiPmh.concatenateRepeatedElements property.
   */
  private static void concatenateRepeatedElements(OaiPmh config, Map<String, String> titlesMap, Element element) {
    if (titlesMap.containsKey(config.getMetadataParsingDefaultLang().getLang())) {
      String previousContent = titlesMap.get(config.getMetadataParsingDefaultLang().getLang());
      String concatenatedContent = previousContent + config.getConcatenateSeparator() + element.getValue();
      titlesMap.put(config.getMetadataParsingDefaultLang().getLang(), concatenatedContent); // keep concatenating
    } else {
      titlesMap.put(config.getMetadataParsingDefaultLang().getLang(), element.getValue()); // set first
    }
  }
}
