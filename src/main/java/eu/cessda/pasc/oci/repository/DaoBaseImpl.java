/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.exception.ExternalSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Shareable Dao functions
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@Service
public class DaoBaseImpl implements DaoBase {

    private final HttpClient httpClient;

    public DaoBaseImpl(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    protected InputStream getInputStream(String fullUrl) throws IOException {
        return getInputStream(URI.create(fullUrl));
    }

    @Override
    public InputStream getInputStream(URI uri) throws IOException {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri).build();
        try {
            log.debug("Sending request to remote SP with url [{}].", uri);

            Instant start = null;
            if (log.isDebugEnabled()) {
                start = Instant.now();
            }

            HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (log.isDebugEnabled()) {
                Instant end = Instant.now();
                assert start != null;
                log.debug("Got response code of [{}], getting headers took [{}] ms",
                        httpResponse.statusCode(), Duration.between(start, end).toMillis());
            }

            // Check the returned HTTP status code, throw an exception if not a success code
            // This includes redirects
            if (httpResponse.statusCode() < 300) {
                return httpResponse.body();
            }

            // The HTTP request wasn't successful, attempt to read the body
            try (InputStream body = httpResponse.body()) {
                throw new ExternalSystemException(
                        httpResponse.statusCode(),
                        new String(body.readAllBytes(), StandardCharsets.UTF_8)
                );
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted. Request cancelled.");
            Thread.currentThread().interrupt();
            return InputStream.nullInputStream();
        }
    }
}
