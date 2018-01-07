package eu.cessda.pasc.osmhhandler.oaipmh.models.config;

import java.util.List;

/**
 * OaiPmh config model
 *
 * @author moses@doraventures.com
 */
public class OaiPmh {

  private List<String> supportedApiVersions;
  private List<String> supportedRecordTypes;
  private List<Repo> repos;

  public List<String> getSupportedApiVersions() {
    return supportedApiVersions;
  }

  public void setSupportedApiVersions(List<String> supportedApiVersions) {
    this.supportedApiVersions = supportedApiVersions;
  }

  public List<String> getSupportedRecordTypes() {
    return supportedRecordTypes;
  }

  public void setSupportedRecordTypes(List<String> supportedRecordTypes) {
    this.supportedRecordTypes = supportedRecordTypes;
  }

  public List<Repo> getRepos() {
    return repos;
  }

  public void setRepos(List<Repo> repos) {
    this.repos = repos;
  }
}
