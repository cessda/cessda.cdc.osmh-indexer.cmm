package eu.cessda.pasc.osmhhandler.oaipmh.models.configuration;

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
  private String preferredMetadataVersion;

  public String getUrl() {
    return url;
  }
}
