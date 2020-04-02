/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static net.logstash.logback.argument.StructuredArguments.value;

/**
 * Shareable Dao functions
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class DaoBase {

  private final RestTemplate restTemplate;

  @Autowired
  public DaoBase(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  String postForStringResponse(String fullUrl) throws ExternalSystemException {
    ResponseEntity<String> responseEntity;

    // This is common to both error handling paths
    String message = String.format("Unsuccessful response from CDC Handler [%s].", value("error_repo_handler_source", fullUrl));
    try {
      responseEntity = restTemplate.getForEntity(fullUrl, String.class);
      return responseEntity.getBody();
    } catch (HttpServerErrorException e) {
      throw new ExternalSystemException(message, e, e.getResponseBodyAsString());
    } catch (RestClientException e) {
      throw new ExternalSystemException(message, e);
    }
  }
}
