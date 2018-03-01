package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.UtilitiesConfiguration;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
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
      log.debug("Sending request to remote SP  for [{}]", fullUrl);
      responseEntity = configuration.getRestTemplate().getForEntity(fullUrl, String.class);
      log.debug("Got response for [{}] as [{}]", fullUrl, responseEntity.getStatusCodeValue());
      return responseEntity.getBody();
    } catch (RestClientException e) {
      ExternalSystemException exception = new ExternalSystemException(UNSUCCESSFUL_RESPONSE, e.getCause());
      try {
        exception.setExternalResponseBody((((HttpServerErrorException) e).getResponseBodyAsString()));
      } catch (Exception e1) {
        exception.setExternalResponseBody(e.getMessage());
      }

      logResponse(HttpStatus.NOT_ACCEPTABLE, exception, log, LogLevel.ERROR);
      throw exception;
    }
  }
}
