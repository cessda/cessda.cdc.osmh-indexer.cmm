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
package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.logging.RemoteResponse;
import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Handler Log Helper
 *
 * @author moses AT doraventures DOT com
 */
public class HandlerLogHelper {

  private static final String ERROR = "ERROR";

  private HandlerLogHelper() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  public static void logResponse(HttpStatus statusCode, ExternalSystemException exception, Logger log, LogLevel logLevel) {

    String msg = RemoteResponse.builder()
        .logLevel(logLevel)
        .responseCode(statusCode.value())
        .responseMessage(exception.getExternalResponseBody())
        .occurredAt(LocalDateTime.now()).build().toString();

    String s = logLevel.toString();
    if (ERROR.equals(s)) {
      log.error(msg);
    }
  }
}
