package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.TermVocabAttributes;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.VocabAttributes;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HTMLFilter.CLEAN_CHARACTER_RETURNS_STRATEGY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;
import static java.util.Optional.ofNullable;
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

  static TermVocabAttributes parseTermVocabAttrAndValues(Element parentElement, Element concept, boolean hasControlledValue) {
    TermVocabAttributes.TermVocabAttributesBuilder builder = TermVocabAttributes.builder();
    builder.term(CLEAN_CHARACTER_RETURNS_STRATEGY.apply(parentElement.getText()));

    if (hasControlledValue) {
      builder.vocab(getAttributeValue(concept, VOCAB_ATTR).orElse(""))
          .vocabUri(getAttributeValue(concept, VOCAB_URI_ATTR).orElse(""))
          .id(concept.getText());
    } else {
      builder.vocab(getAttributeValue(parentElement, VOCAB_ATTR).orElse(""))
          .vocabUri(getAttributeValue(parentElement, VOCAB_URI_ATTR).orElse(""))
          .id(getAttributeValue(parentElement, ID_ATTR).orElse(""));
    }
    return builder.build();
  }

  static VocabAttributes parseVocabAttrAndValues(Element parentElement, Element concept, boolean hasControlledValue) {
    VocabAttributes.VocabAttributesBuilder builder = VocabAttributes.builder();
    if (hasControlledValue) {
      builder.vocab(getAttributeValue(concept, VOCAB_ATTR).orElse(""))
          .vocabUri(getAttributeValue(concept, VOCAB_URI_ATTR).orElse(""))
          .id(concept.getText());
    } else {
      builder.vocab(getAttributeValue(parentElement, VOCAB_ATTR).orElse(""))
          .vocabUri(getAttributeValue(parentElement, VOCAB_URI_ATTR).orElse(""))
          .id(getAttributeValue(parentElement, ID_ATTR).orElse(""));
    }
    return builder.build();
  }

  static <T> Map<String, List<T>> extractMetadataObjectListForEachLang(
      OaiPmh config, String defaultLangIsoCode, Document document, XPathFactory xFactory, String xPath,
      Function<Element, Optional<T>> parserStrategy) {

    Map<String, List<T>> mapOfMetadataToLanguageCode = new HashMap<>();
    List<Element> elements = getElements(document, xFactory, xPath);
    elements.forEach(element -> {
          Optional<T> parsedMetadataPojo = parserStrategy.apply(element);
          parsedMetadataPojo.ifPresent(parsedMetadataPojoValue -> {
            Optional<String> langCode = parseLanguageCode(config, defaultLangIsoCode, element);
            langCode.ifPresent(code ->
                mapMetadataToLanguageCode(mapOfMetadataToLanguageCode, parsedMetadataPojoValue, code));
          });
        }
    );

    return mapOfMetadataToLanguageCode;
  }

  static <T> Map<String, T> extractMetadataObjectForEachLang(
      OaiPmh config, String defaultLangIsoCode, Document document, XPathFactory xFactory, String xPath, Function<Element, T> parserStrategy) {

    Map<String, T> mapOfMetadataToLanguageCode = new HashMap<>();
    List<Element> elements = getElements(document, xFactory, xPath);
    elements.forEach(element -> {
          T parsedMetadataPojo = parserStrategy.apply(element);
          Optional<String> langCode = parseLanguageCode(config, defaultLangIsoCode, element);
          langCode.ifPresent(languageIsoCode ->
              mapOfMetadataToLanguageCode.put(languageIsoCode, parsedMetadataPojo)); //Overrides duplicates, last wins.
        }
    );

    return mapOfMetadataToLanguageCode;
  }

  private static Optional<String> parseLanguageCode(OaiPmh config, String defaultLangIsoCode, Element element) {
    Optional<String> langCode = Optional.empty();

    Optional<Attribute> langAttr = parseLangAttribute(element);
    if (langAttr.isPresent() && !langAttr.get().getValue().isEmpty()) {
      langCode = ofNullable(langAttr.get().getValue());
    } else if (config.getMetadataParsingDefaultLang().isActive()) {
      langCode = ofNullable(defaultLangIsoCode);
    }

    return langCode;
  }

  private static Optional<Attribute> parseLangAttribute(Element element) {
    Optional<Attribute> parentLangAttr = ofNullable(element.getAttribute(LANG_ATTR, XML_NS));
    Optional<Attribute> langAttr;
    if (parentLangAttr.isPresent()) {
      langAttr = parentLangAttr;
    } else {
      Optional<Element> concept = ofNullable(element.getChild("concept", DDI_NS));
      langAttr = concept.isPresent() ? ofNullable(concept.get().getAttribute(LANG_ATTR, XML_NS)) : Optional.empty();
    }
    return langAttr;
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

  static Optional<String> getAttributeValue(Element element, String idAttr) {
    return ofNullable(element.getAttributeValue(idAttr));
  }

  static Optional<Element> getFirstElement(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return ofNullable(expression.evaluateFirst(document));
  }

  static Optional<Attribute> getFirstAttribute(Document document, XPathFactory xFactory, String xPathToElement) {
    XPathExpression<Attribute> expression = xFactory.compile(xPathToElement, Filters.attribute(), null, OAI_AND_DDI_NS);
    return ofNullable(expression.evaluateFirst(document));
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
    return elements.stream()
        // If the same "event" type is defined for multiple languages the following filter will only allow the first.
        // eg: <collDate xml:lang="en" date="2009-03-19" event="start"/>
        //     <collDate xml:lang="fi" date="2009-03-19" event="start"/>
        // Currently there is no requirement to extract dates of event per language.
        .filter(distinctByKey(element -> element.getAttributeValue(EVENT_ATTR))) //
        .collect(Collectors.toMap(element ->
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
                                                      boolean isConcatenating, String langCode,
                                                      Function<Element, String> textExtractionStrategy) {

    Map<String, String> titlesMap = new HashMap<>();
    for (Element element : elements) {
      String elementText = textExtractionStrategy.apply(element);
      Attribute langAttribute = element.getAttribute(LANG_ATTR, XML_NS);
      if (null == langAttribute || langAttribute.getValue().isEmpty()) {
        mapToADefaultingLang(config, isConcatenating, langCode, titlesMap, element, elementText);
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

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
