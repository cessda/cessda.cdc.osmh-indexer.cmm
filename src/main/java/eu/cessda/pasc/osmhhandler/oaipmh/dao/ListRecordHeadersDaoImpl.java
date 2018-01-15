package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendListRecordParams;

/**
 * Data access object contract implementation for querying remote repository for RecordHeaders.
 *
 * @author moses@doraventures.com
 */
@Repository
public class ListRecordHeadersDaoImpl implements ListRecordHeadersDao {

  @Autowired
  RestTemplate restTemplate;

  @Autowired
  PaSCHandlerOaiPmhConfig handlerOaiPmhConfig;

  @Override
  public String listRecordHeaders(String baseRepoUrl) throws InternalSystemException {

    String finalListRecordUrl = appendListRecordParams(baseRepoUrl);
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(finalListRecordUrl, String.class);
    if (responseEntity.getStatusCode().is2xxSuccessful()) {
      return responseEntity.getBody();
    }
    throw new InternalSystemException("Did not receive a successful response from remote repository.");
  }

  @Override
  public String listRecordHeadersResumption(String repoUrlWithResumptionToken) throws InternalSystemException {

    ResponseEntity<String> responseEntity = restTemplate.getForEntity(repoUrlWithResumptionToken, String.class);
    if (responseEntity.getStatusCode().is2xxSuccessful()) {
      return responseEntity.getBody();
    }
    throw new InternalSystemException("Did not receive a successful response from remote repository.");
  }

}
