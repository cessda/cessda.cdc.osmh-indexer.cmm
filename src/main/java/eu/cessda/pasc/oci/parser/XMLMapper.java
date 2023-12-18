/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import lombok.NonNull;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class XMLMapper<T> {
    private final String xPath;
    private final Function<List<Element>, T> mappingFunction;

    XMLMapper(String xpath, Function<List<Element>, T> mappingFunction) {
        this.xPath = xpath;
        this.mappingFunction = mappingFunction;
    }

    public T resolve(Object context, Namespace... namespace) {
        XPathExpression<Element> expression = XPathFactory.instance().compile(xPath, Filters.element(), null, namespace);
        var result = expression.evaluate(context);
        return mappingFunction.apply(result);
    }

    /**
     * Gets the content of the {@code xml:lang} attribute of the given element. If the language code
     * contains a region (i.e. de-DE) the region is stripped.
     *
     * @param element the element to parse.
     * @return the language, or {@code null} if the {@code xml:lang} attribute was not present.
     */
    static String getLangOfElement(Element element) {
        var langAttr = DocElementParser.getLangAttribute(element);
        if (langAttr != null) {
            // If a language-region tag is present, i.e. en-GB, only keep the first part
            var langValue = langAttr.getValue();
            var dashIndex = langValue.indexOf('-');
            if (dashIndex != -1) {
                return langValue.substring(0, dashIndex);
            } else {
                return langValue;
            }
        } else {
            return null;
        }
    }

    @NonNull
    static <T> Function<List<Element>, Map<String, T>> parseLanguageContentOfElement(Function<Element, T> mappingFunction) {
        return parseLanguageContentOfElement(mappingFunction, (a, b) -> b);
    }

    @NonNull
    static <T> Function<List<Element>, Map<String, T>> parseLanguageContentOfElement(Function<Element, T> mappingFunction, BinaryOperator<T> mergeFunction) {
        return elementList -> {
            var langMap = new HashMap<String, T>();

            for (var element : elementList) {
                var lang = getLangOfElement(element);
                var content = mappingFunction.apply(element);
                langMap.merge(lang, content, mergeFunction);
            }

            return langMap;
        };
    }

    @NonNull
    static <T> Function<List<Element>, Map<String, T>> parseLanguageOptContentOfElement(Function<Element, Optional<T>> mappingFunction) {
        return parseLanguageOptContentOfElement(mappingFunction, (a, b) -> b);
    }

    @NonNull
    static <T> Function<List<Element>, Map<String, T>> parseLanguageOptContentOfElement(Function<Element, Optional<T>> mappingFunction, BinaryOperator<T> mergeFunction) {
        return elementList -> {
            var langMap = new HashMap<String, T>();

            for (var element : elementList) {
                var lang = getLangOfElement(element);
                var content = mappingFunction.apply(element);
                content.ifPresent(t -> langMap.merge(lang, t, mergeFunction));
            }

            return langMap;
        };
    }

    /**
     * Extracts metadata from the given list of {@link Element}s, using the given parser strategy {@link Function}.
     * <p>
     * This uses {@link DocElementParser#getLangAttribute(Element)} to extract the language of the elements. If no
     * language information is found then the elements are added under the {@code null} key.
     *
     * @param <T>                the type returned by the parser strategy.
     * @param elementExtractor     the strategy to apply to each element.
     * @return a {@link Map} with the key set to the language, and the value a {@link List} of {@link T}.
     */
    @NonNull
    static <T> Function<List<Element>, Map<String, List<T>>> extractMetadataObjectListForEachLang(Function<Element, Optional<T>> elementExtractor) {
        return elementList -> {
            var map = new HashMap<String, List<T>>();

            for (var element : elementList) {
                elementExtractor.apply(element).ifPresent(mappedElement ->
                    map.computeIfAbsent(getLangOfElement(element), k -> new ArrayList<>()).add(mappedElement)
                );
            }

            return map;
        };
    }

    /**
     * Map the first found element using the given mapping function.
     *
     * @param elementMapper the mapping function.
     * @param <T> the type to map.
     * @param <R> the resulting type.
     */
    @NonNull
    static <T, R> Function<List<T>, Optional<R>> getFirstEntry(Function<T, R> elementMapper) {
        return elements -> elements.stream().findFirst().map(elementMapper);
    }
}
