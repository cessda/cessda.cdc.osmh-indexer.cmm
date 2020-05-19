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
package eu.cessda.pasc.oci.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mizosoft.methanol.Methanol;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Extra Util configuration
 *
 * @author moses AT doraventures DOT com
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "eu.cessda.pasc.oci.repository")
@Slf4j
public class AppUtilityBeansConfiguration {

  @Value("${elasticsearch.host}")
  private String esHost;

  @Value("${elasticsearch.port}")
  private int esPort;

  @Value("${elasticsearch.clustername}")
  private String esClusterName;

  private final AppConfigurationProperties appConfigurationProperties;

  @Autowired
  public AppUtilityBeansConfiguration(AppConfigurationProperties appConfigurationProperties) {
    this.appConfigurationProperties = appConfigurationProperties;
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }


  @Bean
  public Client client() throws UnknownHostException {
    log.debug("Creating Elasticsearch Client\nCluster name={}\nHostname={}", esClusterName, esHost);
    Settings esSettings = Settings.settingsBuilder().put("cluster.name", esClusterName).build();

    // https://www.elastic.co/guide/en/elasticsearch/guide/current/_transport_client_versus_node_client.html
    return TransportClient.builder()
            .settings(esSettings)
            .build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(esHost), esPort));
  }

  @Bean
  public ElasticsearchTemplate elasticsearchTemplate() throws UnknownHostException {
    return new ElasticsearchTemplate(client());
  }

  @Bean
  public HttpClient httpClient() {
    return Methanol.newBuilder()
            .autoAcceptEncoding(true)
            .connectTimeout(Duration.ofMillis(appConfigurationProperties.getRestTemplateProps().getConnTimeout()))
            .requestTimeout(Duration.ofMillis(appConfigurationProperties.getRestTemplateProps().getReadTimeout()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }
}
