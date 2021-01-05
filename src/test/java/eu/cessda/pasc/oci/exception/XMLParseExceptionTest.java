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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

public class XMLParseExceptionTest {
    @Test
    public void shouldInstance() {
        // When
        var uri = URI.create("http://example.local/xml");
        var cause = new IOException();

        // Then
        var xmlException = new XMLParseException(uri, cause);

        // Should contain the url and cause
        Assert.assertEquals(uri, xmlException.getXmlSource());
        Assert.assertEquals(cause, xmlException.getCause());
    }
}