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
package eu.cessda.pasc.oci.configurations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Extra Util configuration
 *
 * @author moses AT doraventures DOT com
 */
@Configuration
@Slf4j
public class ElasticsearchConfiguration {

    private final String esHost;
    private final int esHttpPort;
    private final String esUsername;
    private final String esPassword;

    private final ObjectMapper objectMapper;

    @Autowired
    public ElasticsearchConfiguration(
        @Value("${elasticsearch.host:localhost}") String esHost,
        @Value("${elasticsearch.httpPort:9200}") int esHttpPort,
        @Value("${elasticsearch.username:#{null}}") String esUsername,
        @Value("${elasticsearch.password:#{null}}") String esPassword,
        ObjectMapper objectMapper
    ) {
        this.esHost = esHost;
        this.esHttpPort = esHttpPort;
        this.esUsername = esUsername;
        this.esPassword = esPassword;
        this.objectMapper = objectMapper;
    }

    public Rest5ClientTransport elasticsearchTransport() {
        var esHosts = new HttpHost("http", esHost, esHttpPort);
        final var restClientBuilder = Rest5Client.builder(esHosts);

        if (esUsername != null && esPassword != null) {
            // Set HTTP credentials
            var credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(esHosts), new UsernamePasswordCredentials(esUsername, esPassword.toCharArray()));
            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }

        var restClient = restClientBuilder.build();
        return new Rest5ClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    public ElasticsearchClient elasticsearchClient() {
        var transport = elasticsearchTransport();
        return new ElasticsearchClient(transport);
    }
}
