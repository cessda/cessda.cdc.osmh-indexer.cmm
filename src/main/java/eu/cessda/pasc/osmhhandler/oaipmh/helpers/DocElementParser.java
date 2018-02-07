package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;
import static java.util.stream.Collectors.toList;

/**
 * Helper methods for extracting values from a {@link org.jdom2.Document }
 *
 * @author moses@doraventures.com
 */
class DocElementParser {

  private DocElementParser() {
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
    return elements.stream()
        .filter(Objects::nonNull)
        .map(Element::getValue)
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);
  }

  /**
   * Extracts elements from doc
   *
   * @param document       the document to parse
   * @param xFactory       the xFactory
   * @param xPathToElement the xPath
   * @return nonNull list of {@link Element}
   */
  static List<Element> getElements(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return expression.evaluate(document).stream().filter(Objects::nonNull).collect(toList());
  }

  static Optional<Element> getFirstElement(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return Optional.ofNullable(expression.evaluateFirst(document));
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

    boolean isDefaultingLang = config.getMetadataParsingDefaultLang().isActive();
    String defaultingLang = config.getMetadataParsingDefaultLang().getLang();
    Map<String, String> titlesMap = new HashMap<>();

    for (Element element : elements) {
      Attribute langAttribute = element.getAttribute(LANG_ATTR, XML_NS);
      if (null == langAttribute || langAttribute.getValue().isEmpty()) {
        if (isDefaultingLang) { // If defaulting lang is not configured we skip. We do not know the lang
          if (isConcatenating && config.isConcatRepeatedElements()) {
            concatRepeatedElements(config.getConcatSeparator(), titlesMap, element, defaultingLang);
          } else {
            titlesMap.put(defaultingLang, element.getValue()); // Else keep overriding
          }
        }
      } else if (isConcatenating) {
        concatRepeatedElements(config.getConcatSeparator(), titlesMap, element, langAttribute.getValue());
      } else {
        titlesMap.put(langAttribute.getValue(), element.getValue());
      }
    }
    return titlesMap;
  }

  /**
   * Temp request from PUG to concatenate repeated elements.
   * <p>
   */
  private static void concatRepeatedElements(String separator, Map<String, String> titlesMap, Element element, String xmlLang) {
    String currentElementContent = element.getValue();

    if (titlesMap.containsKey(xmlLang)) {
      String previousElementContent = titlesMap.get(xmlLang);
      String concatenatedContent = previousElementContent + separator + currentElementContent;
      titlesMap.put(xmlLang, concatenatedContent); // keep concatenating
    } else {
      titlesMap.put(xmlLang, currentElementContent); // set first
    }
  }

  public static String extractCreatorWithAffiliation(Element element) {
    String affiliationAttr = element.getAttributeValue(CREATOR_AFFILIATION_ATTR);
    return (null == affiliationAttr) ? element.getValue() : (element.getValue() + " (" + affiliationAttr + ")");
  }
}
