/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.UtilitiesConfiguration;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Shareable Dao functions
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class DaoBase {

  private final UtilitiesConfiguration configuration;

  @Autowired
  public DaoBase(UtilitiesConfiguration configuration) {
    this.configuration = configuration;
  }

  String postForStringResponse(String fullUrl) throws ExternalSystemException {
    ResponseEntity<String> responseEntity;

    try {
      log.debug("Sending request to remote SP with url [{}].", fullUrl);
      responseEntity = configuration.getRestTemplate().getForEntity(fullUrl, String.class);
      log.debug("Got response code of [{}] for [{}]", responseEntity.getStatusCodeValue(), fullUrl);
      return responseEntity.getBody();
    } catch (RestClientException e) {
      String message = String.format("RestClientException! Unsuccessful response from remote SP's Endpoint [%s]", fullUrl);
      ExternalSystemException exception;
      try {
        exception = new ExternalSystemException(message, e.getCause(), ((HttpServerErrorException) e).getResponseBodyAsString());
      } catch (Exception e1) {
        exception = new ExternalSystemException(message, e.getCause(), e.getMessage());
      }

      log.trace(message, e);
      throw exception;
    }
  }
}
