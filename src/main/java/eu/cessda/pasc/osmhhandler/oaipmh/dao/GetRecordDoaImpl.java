package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
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

  private final HandlerConfigurationProperties oaiPmhConfig;

  @Autowired
  public GetRecordDoaImpl(HandlerConfigurationProperties oaiPmhConfig) {
    this.oaiPmhConfig = oaiPmhConfig;
  }

  @Override
  public String getRecordXML(String repoUrl, String studyIdentifier) throws CustomHandlerException {

    String decodedStudyId = decodeStudyNumber(studyIdentifier);
    final String fullUrl = appendGetRecordParams(repoUrl, decodedStudyId, oaiPmhConfig.getOaiPmh());
    return postForStringResponse(fullUrl);
  }
}
