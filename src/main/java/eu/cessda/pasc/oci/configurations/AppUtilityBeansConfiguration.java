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
import eu.cessda.pasc.oci.helpers.exception.InternalSystemException;
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
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

  private final PerfRequestSyncInterceptor perfRequestSyncInterceptor;

  @Autowired
  public AppUtilityBeansConfiguration(AppConfigurationProperties appConfigurationProperties, PerfRequestSyncInterceptor perfRequestSyncInterceptor) {
    this.appConfigurationProperties = appConfigurationProperties;
    this.perfRequestSyncInterceptor = perfRequestSyncInterceptor;
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public DocumentBuilder documentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder();
  }

  @Bean
  public RestTemplate restTemplate() {

    final List<ClientHttpRequestInterceptor> requestInterceptors = new ArrayList<>();
    requestInterceptors.add(perfRequestSyncInterceptor);

    final RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
    restTemplate.setInterceptors(requestInterceptors);
    restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

    return restTemplate;
  }

  private ClientHttpRequestFactory getClientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
    clientHttpRequestFactory.setConnectTimeout(appConfigurationProperties.getRestTemplateProps().getConnTimeout());
    clientHttpRequestFactory.setReadTimeout(appConfigurationProperties.getRestTemplateProps().getReadTimeout());
    clientHttpRequestFactory.setConnectionRequestTimeout(appConfigurationProperties.getRestTemplateProps()
        .getConnRequestTimeout());
    return clientHttpRequestFactory;
  }

  @Bean
  public Client client() throws InternalSystemException {

    Settings esSettings = Settings.settingsBuilder().put("cluster.name", esClusterName).build();

    //https://www.elastic.co/guide/en/elasticsearch/guide/current/_transport_client_versus_node_client.html
    try {
      return TransportClient.builder()
          .settings(esSettings)
          .build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(esHost), esPort)
          );
    } catch (UnknownHostException e) {
      throw new InternalSystemException(e.getMessage(), e);
    }
  }

  @Bean()
  public ElasticsearchTemplate elasticsearchTemplate() throws InternalSystemException {
    try {
      return new ElasticsearchTemplate(client());
    } catch (InternalSystemException e) {
      throw new InternalSystemException(e.getMessage(), e);
    }
  }
}
