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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.exception.OaiPmhException;
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

    DocElementParser(OaiPmh oaiPmh) {
        this.oaiPmh = oaiPmh;
    }

    @Autowired
    private DocElementParser(AppConfigurationProperties appConfigurationProperties) {
        this.oaiPmh = appConfigurationProperties.getOaiPmh();
    }

    /**
     * Returns the text content of the given attribute.
     * If the attribute does not exist, an empty {@link Optional} will be returned.
     *
     * @param element the element to parse.
     * @param idAttr  the attribute to return the text content of.
     */
    static Optional<String> getAttributeValue(Element element, String idAttr) {
        return ofNullable(element.getAttributeValue(idAttr));
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
        return expression.evaluate(document);
    }

    /**
     * Extracts Date elements from doc that has @date
     *
     * @param document       the document to parse
     * @param xPathToElement the xPath
     * @return a list of {@link Element}s
     */
    private List<Element> getElementsWithDateAttr(Document document, String xPathToElement) {
        XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
        return expression.evaluate(document).stream()
            .filter(element -> getAttributeValue(element, DATE_ATTR).isPresent()) //PUG requirement: we only care about those with @date CV
            .collect(toList());
    }

    /**
     * Extracts metadata from the given {@link Document}, using the given parser strategy {@link Function}.
     * <p>
     * This method performs per-language extraction using the semantics of {@link DocElementParser#parseLanguageCode(Element, String)},
     * and returns all elements that are present.
     *
     * @param defaultLangIsoCode the language to fall back to if the elements do not have a {@value OaiPmhConstants#LANG_ATTR} attribute.
     * @param document           the {@link Document} to parse.
     * @param xPath              the XPath to search.
     * @param parserStrategy     the strategy to apply to each element.
     * @param <T>                the type returned by the parser strategy.
     * @return a {@link Map} with the key set to the language, and the value a {@link List} of {@link T}.
     */
    <T> Map<String, List<T>> extractMetadataObjectListForEachLang(String defaultLangIsoCode, Document document, String xPath, Function<Element, Optional<T>> parserStrategy) {

        var elements = getElements(document, xPath);
        return elements.stream().map(element -> parserStrategy.apply(element)
            .flatMap(parsedMetadataPojoValue -> parseLanguageCode(element, defaultLangIsoCode)
                .map(lang -> Map.entry(lang, parsedMetadataPojoValue))))
            .filter(Optional::isPresent).map(Optional::get)
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    /**
     * Extracts metadata from the given {@link Document}, using the given parser strategy {@link Function}.
     * <p>
     * This method performs per-language extraction using the semantics of {@link DocElementParser#parseLanguageCode(Element, String)}.
     * If multiple values with the same language key are encountered, the last encountered is returned.
     *
     * @param defaultLangIsoCode the language to fall back to if the elements do not have a {@value OaiPmhConstants#LANG_ATTR} attribute.
     * @param document           the {@link Document} to parse.
     * @param xPath              the XPath to search.
     * @param parserStrategy     the strategy to apply to each element.
     * @param <T>                the type returned by the parser strategy.
     * @return a {@link Map} with the key set to the language.
     */
    <T> Map<String, T> extractMetadataObjectForEachLang(String defaultLangIsoCode, Document document, String xPath, Function<Element, T> parserStrategy) {
        var elements = getElements(document, xPath);
        return elements.stream().map(element -> parseLanguageCode(element, defaultLangIsoCode).map(lang -> Map.entry(lang, parserStrategy.apply(element))))
            .filter(Optional::isPresent).map(Optional::get)
            // If multiple values with the same key are returned, the last value wins
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }

    private Optional<String> parseLanguageCode(Element element, String defaultLangIsoCode) {

        Attribute langAttr = element.getAttribute(LANG_ATTR, Namespace.XML_NAMESPACE);
        if (langAttr == null) {
            Element concept = element.getChild("concept", DDI_NS);
            if (concept != null) {
                langAttr = concept.getAttribute(LANG_ATTR, Namespace.XML_NAMESPACE);
            }
        }

        if (langAttr != null && !langAttr.getValue().isEmpty()) {
            return Optional.of(langAttr.getValue());
        } else if (oaiPmh.getMetadataParsingDefaultLang().isActive()) {
            return Optional.of(defaultLangIsoCode);
        }

        return Optional.empty();
    }

    /**
     * Gets the first {@link Element} that satisfies the XPath expression. If no elements are found, an empty {@link Optional} is returned
     *
     * @param document       the {@link Document} to parse.
     * @param xPathToElement the XPath.
     * @return The first {@link Element}, or an empty {@link Optional} if no elements were found.
     */
    Optional<Element> getFirstElement(Document document, String xPathToElement) {
        XPathExpression<Element> expression = xFactory.compile(xPathToElement, Filters.element(), null, OAI_AND_DDI_NS);
        return ofNullable(expression.evaluateFirst(document));
    }

    /**
     * Gets the first {@link Attribute} that satisfies the XPath expression. If no attributes are found, an empty {@link Optional} is returned
     *
     * @param document       the {@link Document} to parse.
     * @param xPathToElement the XPath.
     * @return The first {@link Attribute}, or an empty {@link Optional} if no attributes were found.
     */
    Optional<Attribute> getFirstAttribute(Document document, String xPathToElement) {
        XPathExpression<Attribute> expression = xFactory.compile(xPathToElement, Filters.attribute(), null, OAI_AND_DDI_NS);
        return ofNullable(expression.evaluateFirst(document));
    }

    /**
     * Gets the date attribute from elements that have an {@value OaiPmhConstants#EVENT_ATTR} attribute.
     * <p>
     * If the same {@value OaiPmhConstants#EVENT_ATTR} type is defined for multiple languages the filter will only keep the first encountered.
     * <ul>
     *     <li>{@code <collDate xml:lang="en" date="2009-03-19" event="start"/> }</li>
     *     <li><strike>{@code <collDate xml:lang="fi" date="2009-03-19" event="start"/> }</strike></li>
     * </ul>
     * Currently there is no requirement to extract dates of event per language.
     *
     * @param document     the {@link Document} to parse.
     * @param elementXpath the XPath to search.
     * @return a {@link Map} with the keys set to the {@value OaiPmhConstants#EVENT_ATTR} and the values set to the date values.
     */
    Map<String, String> getDateElementAttributesValueMap(Document document, String elementXpath) {
        List<Element> elements = getElementsWithDateAttr(document, elementXpath);
        return elements.stream()
            .filter(element -> Objects.nonNull(element.getAttributeValue(EVENT_ATTR)))
            .collect(Collectors.toMap(element -> element.getAttributeValue(EVENT_ATTR), element -> element.getAttributeValue(DATE_ATTR), (a, b) -> a));
    }

    /**
     * Gets a list of {@link Attribute}s that satisfies the XPath expression.
     *
     * @param document       the {@link Document} to parse.
     * @param xPathToElement the XPath to search.
     * @return a list of {@link Attribute}s.
     */
    private List<Attribute> getAttributes(Document document, String xPathToElement) {
        XPathExpression<Attribute> expression = xFactory.compile(xPathToElement, Filters.attribute(), null, DDI_NS);
        return expression.evaluate(document);
    }

    /**
     * Parses value of given {@link Element} for every given xml@lang attributed.
     * <p>
     * If no lang is found attempts to default to a configured xml@lang.
     * <p>
     * If configuration is set to not default to a given lang, effect is this element is not extracted.
     *
     * @param elements               a list of {@link Element}s to parse.
     * @param isConcatenating        if true, concatenate multiple {@link Element}s of the same language
     * @param langCode               the default language to use if an element does not have a {@value OaiPmhConstants#LANG_ATTR} attribute.
     * @param textExtractionStrategy the text extraction strategy to apply.
     */
    Map<String, String> getLanguageKeyValuePairs(List<Element> elements, boolean isConcatenating, String langCode,
                                                 Function<Element, String> textExtractionStrategy) {

        var titlesMap = new HashMap<String, String>();
        for (Element element : elements) {

            var langAttribute = element.getAttribute(LANG_ATTR, Namespace.XML_NAMESPACE);

            if (langAttribute != null && !langAttribute.getValue().isEmpty()) {
                putElementInMap(titlesMap, langAttribute.getValue(), textExtractionStrategy.apply(element), isConcatenating);
            } else {
                // If defaulting lang is not configured skip, the language is not known
                if (oaiPmh.getMetadataParsingDefaultLang().isActive()) {
                    putElementInMap(titlesMap, langCode, textExtractionStrategy.apply(element), isConcatenating);
                }
            }
        }
        return titlesMap;
    }

    private void putElementInMap(Map<String, String> titlesMap, String langCode, String elementText, boolean isConcatenating) {
        // Concatenate if configured
        if (isConcatenating && oaiPmh.isConcatRepeatedElements() && titlesMap.containsKey(langCode)) {
            elementText = titlesMap.get(langCode) + oaiPmh.getConcatSeparator() + elementText;
        }
        titlesMap.put(langCode, elementText);
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
