package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import org.springframework.stereotype.Repository;

/**
 * Data access object for fetching Record from remote repositories implementation
 *
 * @author moses@doraventures.com
 */
@Repository
public class GetRecordDoaImpl implements GetRecordDoa {

  @Override
  public String getRecordXML(String repoUrl, String studyIdentifier) {
    throw new RuntimeException("not yet implemented");
  }
}
