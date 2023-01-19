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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * Exception for HTTP level failures that prevent a request from completing.
 * The body of the HTTP response is stored for logging purposes.
 *
 * @author moses AT doraventures DOT com
 * @author Matthew Morris
 */
public class HTTPException extends IOException {

    @Serial
    private static final long serialVersionUID = 928798312826959273L;

    private final ExternalResponse externalResponse;

    /**
     * Constructs a {@link HTTPException} with the specified status code and external response body.
     *
     * @param statusCode           the status code of the external response that caused this exception
     * @param externalResponseBody the body of the external response that caused this exception
     */
    public HTTPException(int statusCode, byte[] externalResponseBody) {
        super("Server returned " + statusCode);
        externalResponse = new ExternalResponse(externalResponseBody, statusCode);
    }

    /**
     * Gets the external response that caused this exception.
     */
    @NonNull
    public ExternalResponse getExternalResponse() {
        return externalResponse;
    }

    /**
     * An immutable object describing the status code and the body of the external response.
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Value
    @SuppressWarnings("ArrayToString")
    public static class ExternalResponse implements Serializable {
        @Serial
        private static final long serialVersionUID = -7110617275735794989L;

        /**
         * The body of the response.
         */
        byte @NonNull [] body;
        /**
         * The status code of the response.
         */
        int statusCode;

        /**
         * The body of the response as a {@link StandardCharsets#UTF_8} string.
         */
        public String getBodyAsString() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }
}
