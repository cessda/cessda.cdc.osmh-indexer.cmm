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
package eu.cessda.pasc.oci.http;

import eu.cessda.pasc.oci.exception.HTTPException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Interface for HTTP client methods.
 */
public interface HttpClient {
    /**
     * Performs a HTTP Get request to the specified URL.
     *
     * @return An {@link InputStream} of the response body.
     * @throws HTTPException if the server returns a failure code.
     *                       The status code and the body of the response will be attached to the exception.
     * @throws IOException   if an IO Error occurs.
     */
    InputStream getInputStream(URI uri) throws IOException;
}
