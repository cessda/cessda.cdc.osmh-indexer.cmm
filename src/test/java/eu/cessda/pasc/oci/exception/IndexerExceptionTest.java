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

public class IndexerExceptionTest {

    private static final Exception INNER_EXCEPTION = new Exception("Inner exception");
    private static final String MESSAGE = "Message";

    @Test
    public void shouldInstanceWithAMessage() {
        var internalSystemException = new IndexerException(MESSAGE);
        Assert.assertEquals(MESSAGE, internalSystemException.getMessage());
    }

    @Test
    public void shouldInstanceWithAMessageAndACause() {
        var internalSystemException = new IndexerException(MESSAGE, INNER_EXCEPTION);
        Assert.assertEquals(MESSAGE, internalSystemException.getMessage());
        Assert.assertEquals(INNER_EXCEPTION, internalSystemException.getCause());
    }

    @Test
    public void shouldInstanceWithACause() {
        var internalSystemException = new IndexerException(INNER_EXCEPTION);
        Assert.assertEquals(INNER_EXCEPTION, internalSystemException.getCause());
    }
}