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

import lombok.NonNull;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * An XML mapper that supports both direct and DDI 3.x references.
 * If the direct XPath does not resolve the reference XPath is tried.
 *
 * @param <T> the return type of the mapping function.
 */
public class ResolvingXMLMapper<T> implements XMLMapper<T> {
    private final String baseXPath;
    private final String referenceXPath;
    private final String withinXPath;
    private final Function<List<Element>, T> mappingFunction;

    /**
     * Create a new instance of the XML mapper.
     *
     * @param baseXPath the direct XPath to the element.
     * @param referenceXPath the XPath to the reference element.
     * @param mappingFunction the mapping function.
     */
    public ResolvingXMLMapper(String baseXPath, String referenceXPath, Function<List<Element>, T> mappingFunction) {
        this(baseXPath, referenceXPath, null, mappingFunction);
    }

    /**
     * Create a new instance of the XML mapper.
     *
     * @param baseXPath the direct XPath to the base element.
     * @param referenceXPath the XPath to the reference element.
     * @param withinXPath the XPath to the final element after resolution.
     * @param mappingFunction the mapping function.
     */
    public ResolvingXMLMapper(String baseXPath, String referenceXPath, String withinXPath, Function<List<Element>, T> mappingFunction) {
        this.baseXPath = baseXPath;
        this.referenceXPath = referenceXPath;
        this.withinXPath = withinXPath;
        this.mappingFunction = mappingFunction;
    }

    @Override
    public T resolve(Object context, Namespace... namespace) {
        // Resolve base or reference
        XPathExpression<Element> expression = XPathFactory.instance().compile(baseXPath, Filters.element(), null, namespace);

        var elementList = expression.evaluate(context);

        if (elementList.isEmpty()) {
            // Try resolving the reference XPath
            XPathExpression<Element> referenceExpression = XPathFactory.instance().compile(referenceXPath, Filters.element(), null, namespace);
            var referenceElements = referenceExpression.evaluate(context);

            if (!referenceElements.isEmpty()) {
                elementList = resolveReferences(referenceElements);
            }
        }

        if (withinXPath != null) {
            // Resolve inner element
            XPathExpression<Element> targetExpression = XPathFactory.instance().compile(withinXPath, Filters.element(), null, namespace);
            var targetElementList = new ArrayList<Element>();
            for (var element : elementList) {
                var targetElement = targetExpression.evaluate(element);
                targetElementList.addAll(targetElement);
            }

            return mappingFunction.apply(targetElementList);
        } else {
            return mappingFunction.apply(elementList);
        }
    }

    private static @NonNull List<Element> resolveReferences(List<Element> referenceElements) {
        var elementList = new ArrayList<Element>();
        for (var ref : referenceElements) {
            var resolvedReference = XMLMapper.resolveReference(ref);
            resolvedReference.map(ResolvedReference::element).ifPresent(elementList::add);
        }
        return elementList;
    }
}
