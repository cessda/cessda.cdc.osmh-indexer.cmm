package eu.cessda.pasc.osmh.indexer.configurations;

import eu.cessda.pasc.osmh.indexer.models.config.Repo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;

import java.util.List;

/**
 * Loads Configurations from application*.yml
 *
 * @author moses@doraventures.com
 */
@Configuration
@EnableConfigurationProperties
@EnableMBeanExport
@Qualifier("PascOsmhIndexerConfig")
@ConfigurationProperties(prefix = "osmhIndexer")
@Getter
@Setter
public class PascOsmhIndexerConfig {

  private String osmhHarvesterUrl;
  private String incrementalPeriod;
  private String fullRun;
  private List<Repo> repos;
}
