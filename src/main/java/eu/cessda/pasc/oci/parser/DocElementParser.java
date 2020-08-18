/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.configurations.HandlerConfigurationProperties;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.models.cmmstudy.TermVocabAttributes;
import eu.cessda.pasc.oci.models.oai.configuration.OaiPmh;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static eu.cessda.pasc.oci.parser.HTMLFilter.cleanCharacterReturns;
import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * Helper methods for extracting values from a {@link org.jdom2.Document }
 *
 * @author moses AT doraventures DOT com
 */
@Component
class DocElementParser {

  private final OaiPmh oaiPmh;
  private final XPathFactory xFactory = XPathFactory.instance();

  @Autowired
  public DocElementParser(HandlerConfigurationProperties handlerConfigurationProperties) {
    this.oaiPmh = handlerConfigurationProperties.getOaiPmh();
  }

  /**
   * Extracts elements from doc
   *
   * @param document       the document to parse
   * @param xPathToElement the xPath
   * @return nonNull list of {@link Element}
   */
  List<Element> getElements(Document document, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return expression.evaluate(document).stream().filter(Objects::nonNull).collect(toList());
  }

    static TermVocabAttributes parseTermVocabAttrAndValues(Element parentElement, Element concept, boolean hasControlledValue) {
    TermVocabAttributes.TermVocabAttributesBuilder builder = TermVocabAttributes.builder();
      builder.term(cleanCharacterReturns(parentElement.getText()));

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

  /**
   * Extracts Date elements from doc that has @date
   *
   * @param document       the document to parse
   * @param xPathToElement the xPath
   * @return nonNull list of {@link Element}
   */
  private List<Element> getElementsWithDateAttr(Document document, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return expression.evaluate(document).stream()
            .filter(Objects::nonNull)
            .filter(element -> getAttributeValue(element, DATE_ATTR).isPresent()) //PUG requirement: we only care about those with @date CV
            .collect(toList());
  }

  <T> Map<String, List<T>> extractMetadataObjectListForEachLang(
          String defaultLangIsoCode, Document document, String xPath,
          Function<Element, Optional<T>> parserStrategy) {

    Map<String, List<T>> mapOfMetadataToLanguageCode = new HashMap<>();
    List<Element> elements = getElements(document, xPath);
      for (Element element : elements) {
          parserStrategy.apply(element).ifPresent(parsedMetadataPojoValue ->
              parseLanguageCode(defaultLangIsoCode, element).ifPresent(code ->
                  mapOfMetadataToLanguageCode.computeIfAbsent(code, k -> new ArrayList<>()).add(parsedMetadataPojoValue)));
      }

      return mapOfMetadataToLanguageCode;
  }

  <T> Map<String, T> extractMetadataObjectForEachLang(String defaultLangIsoCode, Document document, String xPath, Function<Element, T> parserStrategy) {

    Map<String, T> mapOfMetadataToLanguageCode = new HashMap<>();
    List<Element> elements = getElements(document, xPath);
    for (Element element : elements) {
      T parsedMetadataPojo = parserStrategy.apply(element);

        //Overrides duplicates, last wins.
      parseLanguageCode(defaultLangIsoCode, element).ifPresent(languageIsoCode -> mapOfMetadataToLanguageCode.put(languageIsoCode, parsedMetadataPojo));
    }

    return mapOfMetadataToLanguageCode;
  }

  private Optional<String> parseLanguageCode(String defaultLangIsoCode, Element element) {

    Attribute langAttr = element.getAttribute(LANG_ATTR, Namespace.XML_NAMESPACE);
    if (langAttr == null) {
      Element concept = element.getChild("concept", DDI_NS);
      if (concept != null) {
          langAttr = concept.getAttribute(LANG_ATTR, Namespace.XML_NAMESPACE);
      }
    }
    if (langAttr != null && !langAttr.getValue().isEmpty()) {
      return ofNullable(langAttr.getValue());
    } else if (oaiPmh.getMetadataParsingDefaultLang().isActive()) {
      return ofNullable(defaultLangIsoCode);
    }

    return Optional.empty();
  }

  static Optional<String> getAttributeValue(Element element, String idAttr) {
    return ofNullable(element.getAttributeValue(idAttr));
  }

  Optional<Element> getFirstElement(Document document, String xPathToElement) {
    XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
    return ofNullable(expression.evaluateFirst(document));
  }

  Optional<Attribute> getFirstAttribute(Document document, String xPathToElement) {
    XPathExpression<Attribute> expression = xFactory.compile(xPathToElement, Filters.attribute(), null, OAI_AND_DDI_NS);
    return ofNullable(expression.evaluateFirst(document));
  }

  /**
   * Temp request from PUG to concatenate repeated elements.
   * <p>
   */
  private static void concatRepeatedElements(String separator, Map<String, String> titlesMap, Element element, String xmlLang) {

    String currentElementContent = element.getText();

    if (titlesMap.containsKey(xmlLang)) {
      String previousElementContent = titlesMap.get(xmlLang);
      String concatenatedContent = previousElementContent + separator + currentElementContent;
      titlesMap.put(xmlLang, concatenatedContent); // keep concatenating
    } else {
      titlesMap.put(xmlLang, currentElementContent); // set first
    }
  }

  Map<String, String> getDateElementAttributesValueMap(Document document, String elementXpath) {
    List<Element> elements = getElementsWithDateAttr(document, elementXpath);
    return elements.stream()
            // If the same "event" type is defined for multiple languages the following filter will only allow the first.
            // eg: <collDate xml:lang="en" date="2009-03-19" event="start"/>
            //     <collDate xml:lang="fi" date="2009-03-19" event="start"/>
            // Currently there is no requirement to extract dates of event per language.
            .filter(element -> Objects.nonNull(element.getAttributeValue(EVENT_ATTR)))
            .collect(Collectors.toMap(element -> element.getAttributeValue(EVENT_ATTR), element -> element.getAttributeValue(DATE_ATTR), (a, b) -> a));
  }

  private List<Attribute> getAttributes(Document document, String xPathToElement) {
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
  Map<String, String> getLanguageKeyValuePairs(List<Element> elements, boolean isConcatenating, String langCode,
                                               Function<Element, String> textExtractionStrategy) {

    Map<String, String> titlesMap = new HashMap<>();
    for (Element element : elements) {
      String elementText = textExtractionStrategy.apply(element);
      Attribute langAttribute = element.getAttribute(LANG_ATTR, Namespace.XML_NAMESPACE);
      if (null == langAttribute || langAttribute.getValue().isEmpty()) {
        mapToADefaultingLang(isConcatenating, langCode, titlesMap, element, elementText);
      } else if (isConcatenating) {
        concatRepeatedElements(oaiPmh.getConcatSeparator(), titlesMap, element, langAttribute.getValue());
      } else {
        titlesMap.put(langAttribute.getValue(), elementText);
      }
    }
    return titlesMap;
  }

  private void mapToADefaultingLang(boolean isConcatenating, String defaultingLang,
                                    Map<String, String> titlesMap, Element element, String elementText) {
    boolean isDefaultingLang = oaiPmh.getMetadataParsingDefaultLang().isActive();
    if (isDefaultingLang) { // If defaulting lang is not configured we skip. We do not know the lang
      if (isConcatenating && oaiPmh.isConcatRepeatedElements()) {
        concatRepeatedElements(oaiPmh.getConcatSeparator(), titlesMap, element, defaultingLang);
      } else {
        titlesMap.put(defaultingLang, elementText); // Else keep overriding
      }
    }
  }

    /**
   * Parses the array values of attributes of a given elements
   *
   * @param document     the document to parse
   * @param elementXpath the Element parent node to retrieve
   * @return Array String Values of the attributes
   */
  List<String> getAttributeValues(Document document, String elementXpath) {
      List<Attribute> attributes = getAttributes(document, elementXpath);
      return attributes.stream().map(Attribute::getValue).collect(Collectors.toList());
  }

    /**
     * Checks if the record has an {@literal <error>} element.
     *
     * @param document the document to map to.
     * @throws OaiPmhException if an {@literal <error>} element was present.
     */
    void validateResponse(Document document) throws OaiPmhException {

        final Optional<Element> optionalElement = getFirstElement(document, ERROR_PATH);

        if (optionalElement.isPresent()) {
            final Element element = optionalElement.get();
            if (element.getText() != null && !element.getText().trim().isEmpty()) {
                throw new OaiPmhException(OaiPmhException.Code.valueOf(element.getAttributeValue(CODE_ATTR)), element.getText());
            } else {
                throw new OaiPmhException(OaiPmhException.Code.valueOf(element.getAttributeValue(CODE_ATTR)));
            }
        }
    }
}
