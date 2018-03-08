package eu.cessda.pasc.oci.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.InetAddress;

/**
 * Extra Util configuration
 *
 * @author moses@doraventures.com
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

  @Autowired
  AppConfigurationProperties appConfigurationProperties;

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public DocumentBuilder documentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate(getClientHttpRequestFactory());
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
  public Client client() throws Exception {

    Settings esSettings = Settings.settingsBuilder().put("cluster.name", esClusterName).build();

    //https://www.elastic.co/guide/en/elasticsearch/guide/current/_transport_client_versus_node_client.html
    return TransportClient.builder()
        .settings(esSettings)
        .build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(esHost), esPort)
        );
  }

  @Bean()
  public ElasticsearchTemplate elasticsearchTemplate() throws Exception {
    return new ElasticsearchTemplate(client());
  }
}
