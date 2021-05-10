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
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Extra Util configuration
 *
 * @author moses AT doraventures DOT com
 */
@Configuration
@Slf4j
public class ElasticsearchConfiguration implements AutoCloseable   {

    private final String esHost;
    private final int esPort;
    private final int esHttpPort;
    private final String esClusterName;
    private final String esUsername;
    private final String esPassword;

    private RestHighLevelClient restHighLevelClient;
    private TransportClient transportClient;

    public ElasticsearchConfiguration(
        @Value("${elasticsearch.host:localhost}") String esHost,
        @Value("${elasticsearch.port:9300}") int esPort,
        @Value("${elasticsearch.httpPort:9200}") int esHttpPort,
        @Value("${elasticsearch.clustername:elasticsearch}") String esClusterName,
        @Value("${elasticsearch.username:#{null}}") String esUsername,
        @Value("${elasticsearch.password:#{null}}") String esPassword
    ) {
        this.esHost = esHost;
        this.esPort = esPort;
        this.esHttpPort = esHttpPort;
        this.esClusterName = esClusterName;
        this.esUsername = esUsername;
        this.esPassword = esPassword;
    }

    @SuppressWarnings({"resource", "IOResourceOpenedButNotSafelyClosed", "java:S2095"})
    @Bean
    public Client client() throws UnknownHostException {
        if (transportClient == null) {
            log.debug("Creating Elasticsearch Client\nCluster name={}\nHostname={}", esClusterName, esHost);
            var esSettings = Settings.builder().put("cluster.name", esClusterName);
            if (esUsername != null && esPassword != null) {
                esSettings.put("xpack.security.user", esUsername + ":" + esPassword);
            }
            transportClient = new PreBuiltXPackTransportClient(esSettings.build())
                .addTransportAddress(new TransportAddress(InetAddress.getByName(esHost), esPort));
        }
        return transportClient;
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

    @Bean
    public ElasticsearchTemplate elasticsearchTemplate() throws UnknownHostException {
        return new ElasticsearchTemplate(client());
    }

    @Override
    @PreDestroy
    public void close() {
        if (transportClient != null) {
            transportClient.close();
            transportClient = null;
        }
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
