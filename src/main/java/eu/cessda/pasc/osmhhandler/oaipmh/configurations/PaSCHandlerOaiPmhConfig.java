package eu.cessda.pasc.osmhhandler.oaipmh.configurations;

import eu.cessda.pasc.osmhhandler.oaipmh.models.config.OaiPmh;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Configuration
@EnableMBeanExport
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "osmhhandler")
@Qualifier("PaSCHandlerOaiPmhConfig")
public class PaSCHandlerOaiPmhConfig {

  private OaiPmh oaiPmh = new OaiPmh();

  public OaiPmh getOaiPmh() {

    return oaiPmh;
  }
}
