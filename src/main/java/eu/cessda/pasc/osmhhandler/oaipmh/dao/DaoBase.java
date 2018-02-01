package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.UtilitiesConfiguration;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.UNSUCCESSFUL_RESPONSE;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerLogHelper.logResponse;

/**
 * Shareable Dao functions
 *
 * @author moses@doraventures.com
 */
@Component
@Slf4j
public class DaoBase {

  @Autowired
  UtilitiesConfiguration configuration;

  String postForStringResponse(String fullUrl) throws ExternalSystemException {
    ResponseEntity<String> responseEntity;

    try {
      responseEntity = configuration.getRestTemplate().getForEntity(fullUrl, String.class);
    } catch (RestClientException e) {
      throw new ExternalSystemException(UNSUCCESSFUL_RESPONSE, e.getCause());
    }

    HttpStatus statusCode = responseEntity.getStatusCode();
    if (statusCode.is2xxSuccessful()) {
      return responseEntity.getBody();
    } else {
      logResponse(statusCode, log, LogLevel.ERROR);
      throw new ExternalSystemException(UNSUCCESSFUL_RESPONSE);
    }
  }
}
