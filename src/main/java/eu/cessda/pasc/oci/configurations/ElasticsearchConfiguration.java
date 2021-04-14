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
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
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

    @Value("${elasticsearch.host:localhost}")
    private String esHost;

    @Value("${elasticsearch.port:9300}")
    private int esPort;

    @Value("${elasticsearch.httpPort:9200}")
    private int esHttpPort;

    @Value("${elasticsearch.clustername:elasticsearch}")
    private String esClusterName;

    private RestHighLevelClient restHighLevelClient;
    private TransportClient transportClient;

    @SuppressWarnings({"resource", "IOResourceOpenedButNotSafelyClosed"})
    @Bean
    public Client client() throws UnknownHostException {
        if (transportClient == null) {
            log.debug("Creating Elasticsearch Client\nCluster name={}\nHostname={}", esClusterName, esHost);
            Settings esSettings = Settings.builder().put("cluster.name", esClusterName).build();
            transportClient = new PreBuiltTransportClient(esSettings).addTransportAddress(new TransportAddress(InetAddress.getByName(esHost), esPort));
        }
        return transportClient;
    }

    @Bean
    public RestHighLevelClient elasticsearchClient() {
        if (restHighLevelClient == null) {
            var esHosts = new HttpHost(esHost, esHttpPort, "http");
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(esHosts));
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
