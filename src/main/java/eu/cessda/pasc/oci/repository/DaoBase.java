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
package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Shareable Dao functions
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class DaoBase {

  private static final String UNSUCCESSFUL_RESPONSE = "Unsuccessful response from CDC Handler [%s].";
  private final RestTemplate restTemplate;

  @Autowired
  public DaoBase(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  String postForStringResponse(String fullUrl) throws ExternalSystemException {
    ResponseEntity<String> responseEntity;

    try {
      responseEntity = restTemplate.getForEntity(fullUrl, String.class);
      return responseEntity.getBody();
    } catch (RestClientException e) {
      String message = String.format(UNSUCCESSFUL_RESPONSE, fullUrl);
      log.error(message, e);
      ExternalSystemException exception = new ExternalSystemException(message, e.getCause());
      try {
        exception.setExternalResponseBody((((HttpServerErrorException) e).getResponseBodyAsString()));
      } catch (Exception e1) {
        exception.setExternalResponseBody(e.getMessage());
      }
      throw exception;
    }
  }
}
