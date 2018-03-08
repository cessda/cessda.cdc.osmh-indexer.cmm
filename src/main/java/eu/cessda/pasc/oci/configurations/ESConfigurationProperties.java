package eu.cessda.pasc.oci.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Loads Configurations from application*.yml specifically for elasticsearch
 *
 * @author moses@doraventures.com
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "elasticsearch")
@Getter
@Setter
public class ESConfigurationProperties {

  private String clustername= "elasticsearch";
  private String host = "127.0.0.1";
  private String port = "9300";
  private int numberOfShards = 1;
  private int numberOfReplicas = 1;
}
