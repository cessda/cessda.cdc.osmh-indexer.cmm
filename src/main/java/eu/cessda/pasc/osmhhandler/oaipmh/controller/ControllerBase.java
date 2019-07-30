/**
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
package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.MESSAGE;

/**
 * Base helper class for controllers to inherit from
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
public class ControllerBase {

  @Autowired
  ObjectMapper objectMapper;

  static ResponseEntity<String> logAndGetResponseEntityMessage(String message, HttpStatus httpStatus, Logger logger) {
    logger.error(message);
    return getResponseEntityMessage(message, httpStatus);
  }

  static ResponseEntity<String> getResponseEntityMessage(String message, HttpStatus httpStatus) {
    return getResponseEntity(getSimpleResponseMessage(message), httpStatus);
  }

  static ResponseEntity<String> getResponseEntity(String message, HttpStatus httpStatus) {
    return new ResponseEntity<>(message, httpStatus);
  }

  private static String getSimpleResponseMessage(String messageString) {
    JSONObject obj = new JSONObject();
    obj.put(MESSAGE, messageString);
    return obj.toJSONString();
  }
}
