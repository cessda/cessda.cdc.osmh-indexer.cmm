package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendListRecordParams;

/**
 * Data access object contract implementation for querying remote repository for RecordHeaders.
 *
 * @author moses@doraventures.com
 */
@Repository
public class ListRecordHeadersDaoImpl extends DaoBase implements ListRecordHeadersDao {

  private HandlerConfigurationProperties config;

  @Autowired
  public ListRecordHeadersDaoImpl(HandlerConfigurationProperties config) {
    this.config = config;
  }

  @Override
  public String listRecordHeaders(String baseRepoUrl) throws ExternalSystemException {
    String finalListRecordUrl = appendListRecordParams(baseRepoUrl, config.getOaiPmh());
    return postForStringResponse(finalListRecordUrl);
  }

  @Override
  public String listRecordHeadersResumption(String repoUrlWithResumptionToken) throws ExternalSystemException {
    return postForStringResponse(repoUrlWithResumptionToken);
  }
}
