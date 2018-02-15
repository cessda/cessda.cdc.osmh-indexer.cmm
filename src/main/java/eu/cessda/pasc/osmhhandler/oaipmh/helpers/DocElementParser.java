package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.DataCollectionFreeText;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.TermVocabAttributes;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
   * @param document     the document to parse
   * @param xFactory     the xFactory
   * @param elementXpath the Element parent node to retrieve
   * @return String[] Values of the Element
   */
  static String[] getElementValues(Document document, XPathFactory xFactory, String elementXpath) {
    List<Element> elements = getElements(document, xFactory, elementXpath);
    return elements.stream()
        .filter(Objects::nonNull)
        .map(Element::getText)
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

  /**
   * Extracts Date elements from doc that has @date
   *
   * @param document       the document to parse
   * @param xFactory       the xFactory
   * @param xPathToElement the xPath
   * @return nonNull list of {@link Element}
   */
  private static List<Element> getElementsWithDateAttr(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return expression.evaluate(document).stream()
        .filter(Objects::nonNull)
        .filter(element -> getAttributeValue(element, DATE_ATTR)
            .isPresent()) //PUG requirement: we only care about those with @date CV
        .collect(toList());
  }

  static TermVocabAttributes parseTermVocabAttrAndValues(Element element, Element concept) {

    if ("empty".equals(concept.getName())) {
      return parseTermVocabAttrAndValues(element, element.getText());
    }
    // PUG requirement parent element Value of concept wins if it has a value.
    String elementValue = (element.getText().isEmpty()) ? concept.getText() : element.getText();
    return parseTermVocabAttrAndValues(concept, elementValue);
  }

  static <T> Map<String, List<T>> extractMetadataObjectListForEachLang(OaiPmh config, Document document,
                                                                       XPathFactory xFactory, String xPath,
                                                                       Function<Element, Optional<T>> parserStrategy) {

    Map<String, List<T>> mapOfMetadataToLanguageCode = new HashMap<>();
    List<Element> elements = getElements(document, xFactory, xPath);
    elements.forEach(element -> {
          Optional<T> parsedMetadataPojo = parserStrategy.apply(element);
          parsedMetadataPojo.ifPresent(parsedMetadataPojoValue -> {
            Optional<String> langCode = getParseLanguageCode(config, element);
            langCode.ifPresent(code ->
                mapMetadataToLanguageCode(mapOfMetadataToLanguageCode, parsedMetadataPojoValue, code));
          });
        }
    );

    return mapOfMetadataToLanguageCode;
  }

  static <T> Map<String, T> extractMetadataObjectForEachLang(
      OaiPmh config, Document document, XPathFactory xFactory, String xPath, Function<Element, T> parserStrategy) {

    Map<String, T> mapOfMetadataToLanguageCode = new HashMap<>();
    List<Element> elements = getElements(document, xFactory, xPath);
    elements.forEach(element -> {
          T parsedMetadataPojo = parserStrategy.apply(element);
          Optional<String> langCode = getParseLanguageCode(config, element);
          langCode.ifPresent(languageIsoCode ->
              mapOfMetadataToLanguageCode.put(languageIsoCode, parsedMetadataPojo)); //Overrides duplicates, last wins.
        }
    );

    return mapOfMetadataToLanguageCode;
  }

  private static Optional<String> getParseLanguageCode(OaiPmh config, Element element) {

    Optional<Element> concept = Optional.ofNullable(element.getChild("concept", DDI_NS));

    Attribute langAttr = concept.isPresent() ? concept.get().getAttribute(LANG_ATTR, XML_NS) :
        element.getAttribute(LANG_ATTR, XML_NS);

    Optional<String> langCode = Optional.empty();
    if (null != langAttr && !langAttr.getValue().isEmpty()) {
      langCode = Optional.ofNullable(langAttr.getValue());
    } else if (config.getMetadataParsingDefaultLang().isActive()) {
      langCode = Optional.ofNullable(config.getMetadataParsingDefaultLang().getLang());
    }

    return langCode;
  }

  private static <T> void mapMetadataToLanguageCode(Map<String, List<T>> mapOfMetadataToLanguageCode,
                                                    T metadataPojo, String languageCode) {

    if (mapOfMetadataToLanguageCode.containsKey(languageCode)) {
      List<T> currentLanguageMetadataList = mapOfMetadataToLanguageCode.get(languageCode);
      currentLanguageMetadataList.add(metadataPojo);
      mapOfMetadataToLanguageCode.put(languageCode, currentLanguageMetadataList);
    } else {
      List<T> initialLanguageMetadataList = new ArrayList<>();
      initialLanguageMetadataList.add(metadataPojo);
      mapOfMetadataToLanguageCode.put(languageCode, initialLanguageMetadataList); // set Afresh
    }
  }

  private static TermVocabAttributes parseTermVocabAttrAndValues(Element elementToProcess, String elementValue) {
    return TermVocabAttributes.builder()
        .id(getAttributeValue(elementToProcess, ID_ATTR).orElse(""))
        .vocab(getAttributeValue(elementToProcess, VOCAB_ATTR).orElse(""))
        .vocabUri(getAttributeValue(elementToProcess, VOCAB_URI_ATTR).orElse(""))
        .term(elementValue).build();
  }

  static Optional<String> getAttributeValue(Element element, String idAttr) {
    return Optional.ofNullable(element.getAttributeValue(idAttr));
  }

  static Optional<Element> getFirstElement(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return Optional.ofNullable(expression.evaluateFirst(document));
  }

  static Optional<Attribute> getFirstAttribute(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Attribute> expression = xFactory.compile(xPathToElement, Filters.attribute(), null, OAI_AND_DDI_NS);
    return Optional.ofNullable(expression.evaluateFirst(document));
  }

  /**
   * Parses the array values of attributes of a given elements
   *
   * @param document     the document to parse
   * @param xFactory     the xFactory
   * @param elementXpath the Element parent node to retrieve
   * @return Array String Values of the attributes
   */
  static String[] getAttributeValues(Document document, XPathFactory xFactory, String elementXpath) {
    List<Attribute> attributes = getAttributes(document, xFactory, elementXpath);
    return attributes.stream().map(Attribute::getValue).toArray(String[]::new);
  }

  static Map<String, String> getDateElementAttributesValueMap(Document document, XPathFactory xFactory,
                                                              String elementXpath) {
    List<Element> elements = getElementsWithDateAttr(document, xFactory, elementXpath);
    return elements.stream().collect(Collectors.toMap(element ->
        element.getAttributeValue(EVENT_ATTR), element ->
        element.getAttributeValue(DATE_ATTR)));
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
  static Map<String, String> getLanguageKeyValuePairs(OaiPmh config, List<Element> elements,
                                                      boolean isConcatenating,
                                                      Function<Element, String> textExtractionStrategy) {

    String defaultingLang = config.getMetadataParsingDefaultLang().getLang();
    Map<String, String> titlesMap = new HashMap<>();

    for (Element element : elements) {
      String elementText = textExtractionStrategy.apply(element);
      Attribute langAttribute = element.getAttribute(LANG_ATTR, XML_NS);
      if (null == langAttribute || langAttribute.getValue().isEmpty()) {
        mapToADefaultingLang(config, isConcatenating, defaultingLang, titlesMap, element, elementText);
      } else if (isConcatenating) {
        concatRepeatedElements(config.getConcatSeparator(), titlesMap, element, langAttribute.getValue());
      } else {
        titlesMap.put(langAttribute.getValue(), elementText);
      }
    }
    return titlesMap;
  }

  private static void mapToADefaultingLang(OaiPmh config, boolean isConcatenating, String defaultingLang,
                                           Map<String, String> titlesMap, Element element, String elementText) {
    boolean isDefaultingLang = config.getMetadataParsingDefaultLang().isActive();
    if (isDefaultingLang) { // If defaulting lang is not configured we skip. We do not know the lang
      if (isConcatenating && config.isConcatRepeatedElements()) {
        concatRepeatedElements(config.getConcatSeparator(), titlesMap, element, defaultingLang);
      } else {
        titlesMap.put(defaultingLang, elementText); // Else keep overriding
      }
    }
  }

  /**
   * Temp request from PUG to concatenate repeated elements.
   * <p>
   */
  private static void concatRepeatedElements(
      String separator, Map<String, String> titlesMap, Element element, String xmlLang) {

    String currentElementContent = element.getText();

    if (titlesMap.containsKey(xmlLang)) {
      String previousElementContent = titlesMap.get(xmlLang);
      String concatenatedContent = previousElementContent + separator + currentElementContent;
      titlesMap.put(xmlLang, concatenatedContent); // keep concatenating
    } else {
      titlesMap.put(xmlLang, currentElementContent); // set first
    }
  }

  public static String extractCreatorWithAffiliation(Element element) {
    return getAttributeValue(element, CREATOR_AFFILIATION_ATTR)
        .map(s -> (element.getText() + " (" + s + ")"))
        .orElseGet(element::getText);
  }
}
