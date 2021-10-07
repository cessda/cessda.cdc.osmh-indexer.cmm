/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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
import static org.jdom2.Namespace.XML_NAMESPACE;

/**
 * Helper methods for extracting values from a {@link org.jdom2.Document }
 *
 * @author moses AT doraventures DOT com
 */
@Component
class DocElementParser {

    private final OaiPmh oaiPmh;

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
    static List<Element> getElements(Document document, String xPathToElement, Namespace... namespaces) {
        XPathExpression<Element> expression = XPathFactory.instance().compile(xPathToElement, Filters.element(), null, namespaces);
        return expression.evaluate(document);
    }

    /**
     * Checks if the record has an {@code <error>} element.
     *
     * @param document the document to map to.
     * @throws OaiPmhException if an {@code <error>} element was present.
     */
    static void validateResponse(Document document) throws OaiPmhException {

        final Optional<Element> optionalElement = getFirstElement(document, ERROR_PATH, OAI_NS);

        if (optionalElement.isPresent()) {
            var element = optionalElement.get();
            if (!element.getText().isEmpty()) {
                throw new OaiPmhException(OaiPmhException.Code.valueOf(element.getAttributeValue(CODE_ATTR)), element.getText());
            } else {
                throw new OaiPmhException(OaiPmhException.Code.valueOf(element.getAttributeValue(CODE_ATTR)));
            }
        }
    }

    /**
     * Gets the first {@link Element} that satisfies the XPath expression. If no elements are found, an empty {@link Optional} is returned
     *
     * @param document       the {@link Document} to parse.
     * @param xPathToElement the XPath.
     * @return The first {@link Element}, or an empty {@link Optional} if no elements were found.
     */
    static Optional<Element> getFirstElement(Document document, String xPathToElement, Namespace... namespaces) {
        var elements = getElements(document, xPathToElement, namespaces);
        return elements.stream().findFirst();
    }

    /**
     * Gets the first {@link Attribute} that satisfies the XPath expression. If no attributes are found, an empty {@link Optional} is returned
     *
     * @param document       the {@link Document} to parse.
     * @param xPathToElement the XPath.
     * @return The first {@link Attribute}, or an empty {@link Optional} if no attributes were found.
     */
    static Optional<Attribute> getFirstAttribute(Document document, String xPathToElement, Namespace... namespaces) {
        var attributes = getAttributes(document, xPathToElement, namespaces);
        return attributes.stream().findFirst();
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
    static Map<String, String> getDateElementAttributesValueMap(Document document, String elementXpath, Namespace... namespaces) {
        var elements = getElements(document, elementXpath, namespaces);
        return elements.stream()
            .filter(element -> getAttributeValue(element, DATE_ATTR).isPresent()) //PUG requirement: we only care about those with @date CV
            .filter(element -> Objects.nonNull(element.getAttributeValue(EVENT_ATTR)))
            .collect(Collectors.toMap(element -> element.getAttributeValue(EVENT_ATTR), element -> element.getAttributeValue(DATE_ATTR), (a, b) -> a));
    }

    /**
     * Gets a list of {@link Attribute}s that satisfies the XPath expression.
     *
     * @param document       the {@link Document} to parse.
     * @param xPathToElement the XPath to search.
     * @param namespaces the namespaces to reference.
     * @return a list of {@link Attribute}s.
     */
    private static List<Attribute> getAttributes(Document document, String xPathToElement, Namespace... namespaces) {
        XPathExpression<Attribute> expression = XPathFactory.instance().compile(xPathToElement, Filters.attribute(), null, namespaces);
        return expression.evaluate(document);
    }

    /**
     * Parses the array values of attributes of a given elements
     *
     * @param document     the document to parse
     * @param elementXpath the Element parent node to retrieve
     * @return Array String Values of the attributes
     */
    static List<String> getAttributeValues(Document document, String elementXpath, Namespace... namespaces) {
        List<Attribute> attributes = getAttributes(document, elementXpath, namespaces);
        return attributes.stream().map(Attribute::getValue).collect(Collectors.toList());
    }

    /**
     * Extracts metadata from the given {@link Document}, using the given parser strategy {@link Function}.
     * <p>
     * This method performs per-language extraction using the semantics of {@link DocElementParser#parseLanguageCode(Element, String, Namespace)},
     * and returns all elements that are present.
     *
     * @param defaultLangIsoCode the language to fall back to if the elements do not have a {@value OaiPmhConstants#LANG_ATTR} attribute.
     * @param document           the {@link Document} to parse.
     * @param xPath              the XPath to search.
     * @param namespace the namespace to search.
     * @param parserStrategy     the strategy to apply to each element.
     * @param <T>                the type returned by the parser strategy.
     * @return a {@link Map} with the key set to the language, and the value a {@link List} of {@link T}.
     */
    <T> Map<String, List<T>> extractMetadataObjectListForEachLang(String defaultLangIsoCode, Document document, String xPath, Namespace namespace, Function<Element, Optional<T>> parserStrategy) {
        var elements = getElements(document, xPath, namespace);
        return elements.stream().flatMap(element ->
            parserStrategy.apply(element).flatMap(parsedMetadataPojoValue ->
                parseLanguageCode(element, defaultLangIsoCode, namespace).map(lang -> Map.entry(lang, parsedMetadataPojoValue))
            ).stream()
        ).collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    /**
     * Extracts metadata from the given {@link Document}, using the given parser strategy {@link Function}.
     * <p>
     * This method performs per-language extraction using the semantics of {@link DocElementParser#parseLanguageCode(Element, String, Namespace)}.
     * If multiple values with the same language key are encountered, the last encountered is returned.
     *
     * @param defaultLangIsoCode the language to fall back to if the elements do not have a {@value OaiPmhConstants#LANG_ATTR} attribute.
     * @param document           the {@link Document} to parse.
     * @param xPath              the XPath to search.
     * @param namespace the namespace to search.
     * @param parserStrategy     the strategy to apply to each element.
     * @param <T>                the type returned by the parser strategy.
     * @return a {@link HashMap} with the key set to the language.
     */
    <T> HashMap<String, T> extractMetadataObjectForEachLang(String defaultLangIsoCode, Document document, String xPath, Namespace namespace, Function<Element, T> parserStrategy) {
        var elements = getElements(document, xPath, namespace);
        return elements.stream().flatMap(element -> parseLanguageCode(element, defaultLangIsoCode, namespace).map(lang -> Map.entry(lang, parserStrategy.apply(element))).stream())
            // If multiple values with the same key are returned, the last value wins
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, HashMap::new));
    }

    private void putElementInMap(Map<String, String> titlesMap, String langCode, String elementText, boolean isConcatenating) {
        // Concatenate if configured
        if (isConcatenating && oaiPmh.isConcatRepeatedElements() && titlesMap.containsKey(langCode)) {
            elementText = titlesMap.get(langCode) + oaiPmh.getConcatSeparator() + elementText;
        }
        titlesMap.put(langCode, elementText);
    }

    private Optional<String> parseLanguageCode(Element element, String defaultLangIsoCode, Namespace ddiNamespace) {

        var langAttr = element.getAttribute(LANG_ATTR, XML_NAMESPACE);
        if (langAttr == null) {
            var concept = element.getChild("concept", ddiNamespace);
            if (concept != null) {
                langAttr = concept.getAttribute(LANG_ATTR, XML_NAMESPACE);
            }
        }

        if (langAttr != null) {
            return Optional.of(langAttr.getValue());
        } else if (oaiPmh.getMetadataParsingDefaultLang().isActive()) {
            return Optional.of(defaultLangIsoCode);
        }

        return Optional.empty();
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
    HashMap<String, String> getLanguageKeyValuePairs(List<Element> elements, boolean isConcatenating, String langCode,
                                                 Function<Element, String> textExtractionStrategy) {

        var titlesMap = new HashMap<String, String>();
        for (Element element : elements) {

            var langAttribute = element.getAttribute(LANG_ATTR, XML_NAMESPACE);

            if (langAttribute != null ) {
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
}
