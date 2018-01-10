package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;

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
  public String listRecordHeadersResumption(String baseRepoUrl, String resumptionToken) throws InternalSystemException {

    String repoUrlWithResumptionToken = appendListRecordResumptionToken(baseRepoUrl, resumptionToken);
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(repoUrlWithResumptionToken, String.class);
    if (responseEntity.getStatusCode().is2xxSuccessful()) {
      return responseEntity.getBody();
    }
    throw new InternalSystemException("Did not receive a successful response from remote repository.");
  }

  private String appendListRecordParams(String repoUrl) {
    return String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, repoUrl,
        VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, METADATA_PREFIX_PARAM_KEY, METADATA_DDI_2_5_VALUE);
  }

  private String appendListRecordResumptionToken(String baseRepoUrl, String resumptionToken) {
    return String.format(LIST_RECORD_HEADERS_RESUMPTION_URL_TEMPLATE, baseRepoUrl,
        VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, RESUMPTION_TOKEN_KEY, resumptionToken);
  }
}
