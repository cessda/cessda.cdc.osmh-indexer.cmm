package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.helpers.AppConstants;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static eu.cessda.pasc.oci.helpers.LogHelper.logResponse;

/**
 * Shareable Dao functions
 *
 * @author moses@doraventures.com
 */
@Component
@Slf4j
public class DaoBase {

  @Autowired
  RestTemplate restTemplate;

  String postForStringResponse(String fullUrl) throws ExternalSystemException {
    ResponseEntity<String> responseEntity;

    try {
      log.debug("Sending request to remote SP  for [{}]", fullUrl);
      responseEntity = restTemplate.getForEntity(fullUrl, String.class);
      log.debug("Got response for [{}] as [{}]", fullUrl, responseEntity.getStatusCodeValue());
      return responseEntity.getBody();
    } catch (RestClientException e) {
      logResponse(HttpStatus.NOT_ACCEPTABLE, log, LogLevel.ERROR);
      ExternalSystemException exception = new ExternalSystemException(AppConstants.UNSUCCESSFUL_RESPONSE, e.getCause());
      exception.setExternalResponseBody((((HttpServerErrorException) e).getResponseBodyAsString()));
      throw exception;
    }
  }
}
