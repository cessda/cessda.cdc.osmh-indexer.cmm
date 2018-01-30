package eu.cessda.pasc.osmh.indexer.models.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Repo configuration model
 *
 * @author moses@doraventures.com
 */
@Getter
@Setter
public class Repo {

  private String url;
  private String serviceProviderName;
}
