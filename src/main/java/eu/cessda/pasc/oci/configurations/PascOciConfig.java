package eu.cessda.pasc.oci.configurations;

import eu.cessda.pasc.oci.models.configurations.Harvester;
import eu.cessda.pasc.oci.models.configurations.OaiPmh;
import eu.cessda.pasc.oci.models.configurations.RestTemplateProps;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;

/**
 * Loads Configurations from application*.yml
 *
 * @author moses@doraventures.com
 */
@Configuration
@EnableConfigurationProperties
@EnableMBeanExport
@Qualifier("PascOsmhIndexerConfig")
@ConfigurationProperties(prefix = "osmhConsumer")
@Getter
public class PascOciConfig {

  private OaiPmh oaiPmh = new OaiPmh();
  private RestTemplateProps restTemplateProps = new RestTemplateProps();
  private Harvester harvester = new Harvester();
}
