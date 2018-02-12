package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.Country;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.TermVocabAttributes;
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

  static Map<String, List<TermVocabAttributes>> extractVocabValueAttrsForEachLang(
      OaiPmh config, List<Element> elements) {
    Map<String, List<TermVocabAttributes>> languageVocabValueAttrs = new HashMap<>();

    elements.forEach(element -> {
      Optional<Element> concept = Optional.ofNullable(element.getChild("concept", DDI_NS));
      TermVocabAttributes vocabValueAttrs =
          parseTermVocabAttrAndValues(element, concept.orElse(new Element("empty")));

      Attribute langAttr = concept.isPresent()
          ? concept.get().getAttribute(LANG_ATTR, XML_NS) : element.getAttribute(LANG_ATTR, XML_NS);

      if (null == langAttr || langAttr.getValue().isEmpty()) {
        boolean isDefaultingLang = config.getMetadataParsingDefaultLang().isActive();
        if (isDefaultingLang) { // If defaulting lang is not configured we skip. We do not know the lang
          String defaultingLang = config.getMetadataParsingDefaultLang().getLang();
          buildLanguageTermVocabAttributes(languageVocabValueAttrs, vocabValueAttrs, defaultingLang);
        }
      } else {
        buildLanguageTermVocabAttributes(languageVocabValueAttrs, vocabValueAttrs, langAttr.getValue());
      }
    });
    return languageVocabValueAttrs;
  }

  private static TermVocabAttributes parseTermVocabAttrAndValues(Element element, Element concept) {

    if ("empty".equals(concept.getName())) {
      return parseTermVocabAttrAndValues(element, element.getText());
    }
    // PUG requirement parent element Value of concept wins if it has a value.
    String elementValue = (element.getText().isEmpty()) ? concept.getText() : element.getText();
    return parseTermVocabAttrAndValues(concept, elementValue);
  }

  private static void buildLanguageTermVocabAttributes(Map<String, List<TermVocabAttributes>> termVocabAttributes,
                                                       TermVocabAttributes currentTermVocabAttributes,
                                                       String defaultingLang) {

    if (termVocabAttributes.containsKey(defaultingLang)) {
      List<TermVocabAttributes> currentLangTermVocabAttributes = termVocabAttributes.get(defaultingLang);
      currentLangTermVocabAttributes.add(currentTermVocabAttributes);
      termVocabAttributes.put(defaultingLang, currentLangTermVocabAttributes);
    } else {
      List<TermVocabAttributes> initialTermVocabAttributes = new ArrayList<>();
      initialTermVocabAttributes.add(currentTermVocabAttributes);
      termVocabAttributes.put(defaultingLang, initialTermVocabAttributes); // set Afresh
    }
  }

  static Map<String, List<Country>> extractMetadataForEachLang(OaiPmh config, List<Element> elements) {
    Map<String, List<Country>> languageVocabValueAttrs = new HashMap<>();

    elements.forEach(element -> {
      Country valuesAndAttrsObject = parseValuesAndAttrsObject(element);
      Attribute langAttr = element.getAttribute(LANG_ATTR, XML_NS);
      String langCode;

      if (null != langAttr && !langAttr.getValue().isEmpty()) {
        langCode = langAttr.getValue();
      } else {
        if (config.getMetadataParsingDefaultLang().isActive()) {
          langCode = config.getMetadataParsingDefaultLang().getLang();
        }else{
          return; // If defaulting lang is not configured we skip. We do not know the lang
        }
      }
      mapMetadataToLanguageCode(languageVocabValueAttrs, valuesAndAttrsObject, langCode);
    });
    return languageVocabValueAttrs;
  }

  private static void mapMetadataToLanguageCode(Map<String, List<Country>> mapOfMetadataToLanguageCode,
                                                Country valuesAndAttrsObject,
                                                String languageCode) {

    if (mapOfMetadataToLanguageCode.containsKey(languageCode)) {
      List<Country> currentLangTermVocabAttributes = mapOfMetadataToLanguageCode.get(languageCode);
      currentLangTermVocabAttributes.add(valuesAndAttrsObject);
      mapOfMetadataToLanguageCode.put(languageCode, currentLangTermVocabAttributes);
    } else {
      List<Country> initialTermVocabAttributes = new ArrayList<>();
      initialTermVocabAttributes.add(valuesAndAttrsObject);
      mapOfMetadataToLanguageCode.put(languageCode, initialTermVocabAttributes); // set Afresh
    }
  }

  private static Country parseValuesAndAttrsObject(Element elementToProcess) {
    return Country.builder()
        .iso2LetterCode(getAttributeValue(elementToProcess, ABBR_ATTR).orElse(""))
        .countryName(elementToProcess.getText()).build();
  }

  private static TermVocabAttributes parseTermVocabAttrAndValues(Element elementToProcess, String elementValue) {
    return TermVocabAttributes.builder()
        .id(getAttributeValue(elementToProcess, ID_ATTR).orElse(""))
        .vocab(getAttributeValue(elementToProcess, VOCAB_ATTR).orElse(""))
        .vocabUri(getAttributeValue(elementToProcess, VOCAB_URI_ATTR).orElse(""))
        .term(elementValue).build();
  }

  private static Optional<String> getAttributeValue(Element element, String idAttr) {
    return Optional.ofNullable(element.getAttributeValue(idAttr));
  }

  static Optional<Element> getFirstElement(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
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

    String defaultingLang = config.getMetadataParsingDefaultLang().getLang();
    Map<String, String> titlesMap = new HashMap<>();

    for (Element element : elements) {
      Attribute langAttribute = element.getAttribute(LANG_ATTR, XML_NS);
      if (null == langAttribute || langAttribute.getValue().isEmpty()) {
        boolean isDefaultingLang = config.getMetadataParsingDefaultLang().isActive();
        if (isDefaultingLang) { // If defaulting lang is not configured we skip. We do not know the lang
          if (isConcatenating && config.isConcatRepeatedElements()) {
            concatRepeatedElements(config.getConcatSeparator(), titlesMap, element, defaultingLang);
          } else {
            titlesMap.put(defaultingLang, element.getText()); // Else keep overriding
          }
        }
      } else if (isConcatenating) {
        concatRepeatedElements(config.getConcatSeparator(), titlesMap, element, langAttribute.getValue());
      } else {
        titlesMap.put(langAttribute.getValue(), element.getText());
      }
    }
    return titlesMap;
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
