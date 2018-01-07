package eu.cessda.pasc.osmhhandler.oaipmh.models.config;

/**
 * Repo configuration model
 *
 * @author moses@doraventures.com
 */
public class Repo {

  private String url;
  private String preferredMetadataVersion;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPreferredMetadataVersion() {
    return preferredMetadataVersion;
  }

  public void setPreferredMetadataVersion(String preferredMetadataVersion) {
    this.preferredMetadataVersion = preferredMetadataVersion;
  }
}
