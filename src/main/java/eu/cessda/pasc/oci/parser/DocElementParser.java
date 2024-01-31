/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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
import lombok.NonNull;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final AppConfigurationProperties.OaiPmh oaiPmh;

    DocElementParser(AppConfigurationProperties.OaiPmh oaiPmh) {
        this.oaiPmh = oaiPmh;
    }

    @Autowired
    private DocElementParser(AppConfigurationProperties appConfigurationProperties) {
        this.oaiPmh = appConfigurationProperties.oaiPmh();
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
     * @param elements the elements to extract attributes from.
     * @return a {@link Map} with the keys set to the {@value OaiPmhConstants#EVENT_ATTR} and the values set to the date values.
     */
    static Map<String, String> getDateElementAttributesValueMap(List<Element> elements) {
        //PUG requirement: we only care about those with @date CV
        Map<String, String> map = new HashMap<>();
        for (var element : elements) {
            var eventAttr = getAttributeValue(element, EVENT_ATTR);
            var dateAttr = getAttributeValue(element, DATE_ATTR);
            if (dateAttr.isPresent() && eventAttr.isPresent()) {
                map.putIfAbsent(eventAttr.get(), dateAttr.get());
            }
        }
        return map;
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
        return attributes.stream().map(Attribute::getValue).toList();
    }

    /**
     * Extracts metadata from the given {@link Document}, using the given parser strategy {@link Function}.
     * <p>
     * This method performs per-language extraction using the semantics of {@link #parseLanguageCode(Element, String)},
     * and returns all elements that are present.
     *
     * @param <T>                the type returned by the parser strategy.
     * @param defaultLangIsoCode the language to fall back to if the elements do not have a {@value OaiPmhConstants#LANG_ATTR} attribute.
     * @param document           the {@link Document} to parse.
     * @param xPath              the XPath to search.
     * @param parserStrategy     the strategy to apply to each element.
     * @param namespace          the namespace to search.
     * @return a {@link Map} with the key set to the language, and the value a {@link List} of {@link T}.
     */
    <T> Map<String, List<T>> extractMetadataObjectListForEachLang(String defaultLangIsoCode, Document document, String xPath, Function<Element, Optional<T>> parserStrategy, Namespace... namespace) {
        var elements = getElements(document, xPath, namespace);
        return elements.stream().flatMap(element ->
            parserStrategy.apply(element).flatMap(parsedMetadataPojoValue ->
                parseLanguageCode(element, defaultLangIsoCode).map(lang -> Map.entry(lang, parsedMetadataPojoValue))
            ).stream()
        ).collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    /**
     * Extracts metadata from the given {@link Document}, using the given parser strategy {@link Function}.
     * <p>
     * This method performs per-language extraction using the semantics of {@link #parseLanguageCode(Element, String)}.
     * If multiple values with the same language key are encountered, the last encountered is returned.
     *
     * @param <T>                the type returned by the parser strategy.
     * @param defaultLangIsoCode the language to fall back to if the elements do not have a {@value OaiPmhConstants#LANG_ATTR} attribute.
     * @param document           the {@link Document} to parse.
     * @param xPath              the XPath to search.
     * @param parserStrategy     the strategy to apply to each element.
     * @param namespace          the namespace to search.
     * @return a {@link HashMap} with the key set to the language.
     */
    <T> HashMap<String, T> extractMetadataObjectForEachLang(String defaultLangIsoCode, Document document, String xPath, Function<Element, T> parserStrategy, Namespace... namespace) {
        var elements = getElements(document, xPath, namespace);
        return elements.stream().flatMap(element -> parseLanguageCode(element, defaultLangIsoCode).map(lang -> Map.entry(lang, parserStrategy.apply(element))).stream())
            // If multiple values with the same key are returned, the last value wins
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, HashMap::new));
    }

    private Optional<String> parseLanguageCode(Element element, String defaultLangIsoCode) {

        var langAttr = getLangAttribute(element);
        if (langAttr == null) {
            var concept = element.getChild("concept", element.getNamespace());
            if (concept != null) {
                langAttr = getLangAttribute(concept);
            }
        }

        if (langAttr != null) {
            // If a language-region tag is present, i.e. en-GB, only keep the first part
            var langValue = langAttr.getValue();
            var dashIndex = langValue.indexOf('-');
            if (dashIndex != -1) {
                return Optional.of(langValue.substring(0, dashIndex));
            } else {
                return Optional.of(langValue);
            }
        } else if (oaiPmh.metadataParsingDefaultLang().active()) {
            return Optional.of(defaultLangIsoCode);
        }

        return Optional.empty();
    }

    @NonNull
    static Optional<String> parseLanguageCode(Attribute langAttr) {
        // If a language-region tag is present, i.e. en-GB, only keep the first part
        var langValue = langAttr.getValue();
        var dashIndex = langValue.indexOf('-');
        if (dashIndex != -1) {
            return Optional.of(langValue.substring(0, dashIndex));
        } else {
            return Optional.of(langValue);
        }
    }

    /**
     * Attempt to find the {@code xml:lang} attribute in the given element.
     * @param element the element to parse.
     * @return the attribute, or {@code null} if the attribute cannot be found.
     */
    static Attribute getLangAttribute(Element element) {
        var langAttr = element.getAttribute(LANG_ATTR, XML_NAMESPACE);
        if (langAttr != null) {
            return langAttr;
        } else {
            // Try to parse older DDI styles of the language
            return element.getAttribute("xml-lang");
        }
    }

}
