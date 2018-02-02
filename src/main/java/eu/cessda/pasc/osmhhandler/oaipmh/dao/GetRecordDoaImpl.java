package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendGetRecordParams;

/**
 * Data access object for fetching Record from remote repositories implementation
 *
 * @author moses@doraventures.com
 */
@Repository
public class GetRecordDoaImpl extends DaoBase implements GetRecordDoa {

  @Autowired
  private PaSCHandlerOaiPmhConfig oaiPmhConfig;

  @Override
  public String getRecordXML(String repoUrl, String studyIdentifier) throws ExternalSystemException {
    final String fullUrl = appendGetRecordParams(repoUrl, studyIdentifier, oaiPmhConfig.getOaiPmh());
    return postForStringResponse(fullUrl);
  }
}
