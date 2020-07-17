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
package eu.cessda.pasc.oci.exception;

import org.junit.Assert;
import org.junit.Test;

public class CustomHandlerExceptionTest {

    private static final Exception INNER_EXCEPTION = new Exception("Inner exception");

    @Test
    public void shouldInstanceWithAMessageAndACause() {
        // When
        var message = "Message";

        // Then
        var customHandlerException = new CustomHandlerException(message, INNER_EXCEPTION);
        Assert.assertEquals(message, customHandlerException.getMessage());
        Assert.assertEquals(INNER_EXCEPTION, customHandlerException.getCause());
    }

    @Test
    public void shouldInstanceWithACause() {
        var customHandlerException = new CustomHandlerException(INNER_EXCEPTION);
        Assert.assertEquals(INNER_EXCEPTION, customHandlerException.getCause());
    }
}