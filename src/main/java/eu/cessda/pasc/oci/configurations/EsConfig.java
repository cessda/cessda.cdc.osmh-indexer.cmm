package eu.cessda.pasc.oci.configurations;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * Elasticsearch Configurations
 *
 * @author moses@doraventures.com
 */
@Configuration
@Component
@EnableElasticsearchRepositories(basePackages = "eu.cessda.pasc.oci.repository")
public class EsConfig {

  @Value("${elasticsearch.host}")
  private String esHost;

  @Value("${elasticsearch.port}")
  private int esPort;

  @Value("${elasticsearch.clustername}")
  private String esClusterName;

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