package eu.cessda.pasc.oci.configurations;

import eu.cessda.pasc.oci.models.configurations.Endpoints;
import eu.cessda.pasc.oci.models.configurations.Harvester;
import eu.cessda.pasc.oci.models.configurations.RestTemplateProps;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads Default Configurations from application*.yml
 *
 * @author moses@doraventures.com
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "osmhConsumer")
@Getter
public class AppConfigurationProperties {

  private Endpoints endpoints = new Endpoints();
  private RestTemplateProps restTemplateProps = new RestTemplateProps();
  private Harvester harvester = new Harvester();
  private List<String> languages = new ArrayList<>();
}
