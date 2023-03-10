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
package eu.cessda.pasc.oci.http;

import eu.cessda.pasc.oci.exception.HTTPException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Shareable Dao functions
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@Service
public class HttpClientImpl implements HttpClient {

    private final java.net.http.HttpClient httpClient;

    public HttpClientImpl(java.net.http.HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    protected InputStream getInputStream(String fullUrl) throws IOException {
        return getInputStream(URI.create(fullUrl));
    }

    @Override
    public InputStream getInputStream(URI uri) throws IOException {
        var httpRequest = HttpRequest.newBuilder(uri).build();

        log.debug("Sending request to url [{}].", uri);

        try {
            var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            log.debug("Got response code of [{}]", httpResponse.statusCode());

            // Check the returned HTTP status code, throw an exception if not a success code
            // This includes redirects
            if (httpResponse.statusCode() < 300) {
                return httpResponse.body();
            }

            // The HTTP request wasn't successful, attempt to read the body
            try (InputStream body = httpResponse.body()) {
                throw new HTTPException(
                    httpResponse.statusCode(),
                    body.readAllBytes()
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }
}
