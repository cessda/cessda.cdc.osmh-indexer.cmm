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

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.List;
import java.util.function.Function;

/**
 * Maps elements found at a specified XPath using a provided mapping function.
 *
 * @param <T> the resulting type of the mapping function.
 */
class SimpleXMLMapper<T> implements XMLMapper<T> {
    private final String xPath;
    private final Function<List<Element>, T> mappingFunction;

    /**
     * Constructs a new instance of the XMLMapper.
     *
     * @param xPath the XPath of the elements to map.
     * @param mappingFunction the mapping function.
     */
    SimpleXMLMapper(String xPath, Function<List<Element>, T> mappingFunction) {
        this.xPath = xPath;
        this.mappingFunction = mappingFunction;
    }

    /**
     * Resolves the given context object to an instance of T.
     *
     * @param context a XPath context to pass to {@link XPathExpression#evaluate(Object)}.
     * @param namespace the XML namespaces to allow resolution of prefixes.
     * @return an instance of {@link T}.
     */
    @Override
    public T resolve(Object context, Namespace... namespace) {
        XPathExpression<Element> expression = XPathFactory.instance().compile(xPath, Filters.element(), null, namespace);
        var result = expression.evaluate(context);
        return mappingFunction.apply(result);
    }
}
