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
package eu.cessda.pasc.oci.exception;

import lombok.Getter;

import java.net.URI;

/**
 * Thrown when an error occurs parsing XML.
 */
@Getter
public class XMLParseException extends HarvesterException {

    private static final long serialVersionUID = -1280955307589066817L;

    private final URI xmlSource;

    /**
     * Constructs a new exception with the specified XML source and caught exception.
     *
     * @param xmlSource The URL that the XML was located at.
     * @param cause The exception that caused the XML parsing to fail.
     */
    public XMLParseException(URI xmlSource, Throwable cause) {
        super(String.format("Parsing %s failed: %s", xmlSource, cause), cause);
        this.xmlSource = xmlSource;
    }
}
