package eu.cessda.pasc.osmhhandler.oaipmh.configuration;

import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.RestTemplateProps;
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
@EnableMBeanExport
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "osmhhandler")
@Qualifier("PaSCHandlerOaiPmhConfig")
@Getter
public class PaSCHandlerOaiPmhConfig {

  private OaiPmh oaiPmh = new OaiPmh();
  private RestTemplateProps restTemplateProps = new RestTemplateProps();

}
