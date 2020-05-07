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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import lombok.extern.slf4j.Slf4j;

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
public class DaoBase {

    public static final String EXCEPTION_MESSAGE = "Unsuccessful response from remote SP's Endpoint [%s]";
    public static final String EXCEPTION_MESSAGE_REJECTED = "Records from remote SP's endpoint is rejected  [%s]";
    private final HttpClient httpClient;
    private final AppConfigurationProperties appConfigurationProperties;

    public DaoBase(HttpClient httpClient, AppConfigurationProperties appConfigurationProperties) {
        this.httpClient = httpClient;
        this.appConfigurationProperties = appConfigurationProperties;
    }

    protected InputStream postForStringResponse(String fullUrl) throws ExternalSystemException {
        return postForStringResponse(URI.create(fullUrl));
    }

    protected InputStream postForStringResponse(URI fullUrl) throws ExternalSystemException {
        HttpRequest httpRequest = HttpRequest
                .newBuilder(fullUrl)
                .timeout(Duration.ofMillis(appConfigurationProperties.getRestTemplateProps().getReadTimeout()))
                .build();
        try {
            log.debug("Sending request to remote SP with url [{}].", fullUrl);

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
            try (InputStream body = httpResponse.body()) {
                throw new ExternalSystemException(
                        String.format(EXCEPTION_MESSAGE_REJECTED, fullUrl),
                        new IOException("Server returned " + httpResponse.statusCode()),
                        new String(body.readAllBytes(), StandardCharsets.UTF_8)
                );
            }
        } catch (IOException e) {
            throw new ExternalSystemException(String.format(EXCEPTION_MESSAGE, fullUrl), e);
        } catch (InterruptedException e) {
            log.warn("Interrupted. Request cancelled.");
            Thread.currentThread().interrupt();
            return InputStream.nullInputStream();
        }
    }
}
