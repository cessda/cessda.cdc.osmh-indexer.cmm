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
package eu.cessda.pasc.oci.configurations;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * Extra Util configuration
 *
 * @author moses AT doraventures DOT com
 */
@Configuration
@Slf4j
public class ElasticsearchConfiguration implements AutoCloseable   {

    private final String esHost;
    private final int esHttpPort;
    private final String esUsername;
    private final String esPassword;

    private RestHighLevelClient restHighLevelClient;

    public ElasticsearchConfiguration(
        @Value("${elasticsearch.host:localhost}") String esHost,
        @Value("${elasticsearch.httpPort:9200}") int esHttpPort,
        @Value("${elasticsearch.username:#{null}}") String esUsername,
        @Value("${elasticsearch.password:#{null}}") String esPassword
    ) {
        this.esHost = esHost;
        this.esHttpPort = esHttpPort;
        this.esUsername = esUsername;
        this.esPassword = esPassword;
    }

    @Bean
    public RestHighLevelClient elasticsearchClient() {
        if (restHighLevelClient == null) {
            var esHosts = new HttpHost(esHost, esHttpPort, "http");

            final var restClientBuilder = RestClient.builder(esHosts);

            if (esUsername != null && esPassword != null) {
                // Set HTTP credentials
                var credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(esUsername, esPassword));
                restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                );
            }

            restHighLevelClient = new RestHighLevelClient(restClientBuilder);
        }
        return restHighLevelClient;
    }

    @Override
    @PreDestroy
    public void close() {
        if (restHighLevelClient != null) {
            try {
                restHighLevelClient.close();
            } catch (IOException e) {
                log.debug("Error occurred when closing ES REST client", e);
            }
            restHighLevelClient = null;
        }
    }
}
