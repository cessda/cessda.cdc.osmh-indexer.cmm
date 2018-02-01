package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import org.springframework.stereotype.Repository;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendListRecordParams;

/**
 * Data access object contract implementation for querying remote repository for RecordHeaders.
 *
 * @author moses@doraventures.com
 */
@Repository
public class ListRecordHeadersDaoImpl extends DaoBase implements ListRecordHeadersDao {

  @Override
  public String listRecordHeaders(String baseRepoUrl) throws ExternalSystemException {
    String finalListRecordUrl = appendListRecordParams(baseRepoUrl);
    return postForStringResponse(finalListRecordUrl);
  }

  @Override
  public String listRecordHeadersResumption(String repoUrlWithResumptionToken) throws ExternalSystemException {
    return postForStringResponse(repoUrlWithResumptionToken);
  }
}
