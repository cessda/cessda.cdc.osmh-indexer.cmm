package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendGetRecordParams;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.decodeStudyNumber;

/**
 * Data access object for fetching Record from remote repositories implementation
 *
 * @author moses@doraventures.com
 */
@Repository
public class GetRecordDoaImpl extends DaoBase implements GetRecordDoa {

  @Autowired
  private HandlerConfigurationProperties oaiPmhConfig;

  @Override
  public String getRecordXML(String repoUrl, String studyIdentifier) throws ExternalSystemException {

    String decodedStudyId = decodeStudyNumber(studyIdentifier);
    final String fullUrl = appendGetRecordParams(repoUrl, decodedStudyId, oaiPmhConfig.getOaiPmh());
    return postForStringResponse(fullUrl);
  }
}
