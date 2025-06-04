/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.exception;

import lombok.Getter;
import org.jdom2.Namespace;

import java.io.Serial;

/**
 * Thrown when attempting to parse a DDI document with an unsupported XML namespace.
 */
@Getter
public class UnsupportedXMLNamespaceException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = -5524959625579025110L;

    /**
     * The XML namespace that was not supported.
     */
    private final Namespace namespace;

    /**
     * Constructs an UnsupportedXMLNamespaceException with the specified namespace.
     * @param namespace the namespace.
     */
    public UnsupportedXMLNamespaceException(Namespace namespace) {
        super("XML namespace \"" + namespace.getURI() + "\" not supported");
        this.namespace = namespace;
    }
}
