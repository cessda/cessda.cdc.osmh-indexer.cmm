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
package eu.cessda.pasc.oci.helpers;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.*;

/**
 * @author moses AT doravenetures DOT com
 */
@Slf4j
public class LogHelperTest {

  @Test
  public void shouldCallLoggerWithExpectedLogLevel() {

    Logger logger = mock(Logger.class);
    LogHelper.logResponse(HttpStatus.BAD_REQUEST, logger, LogLevel.ERROR);
    verify(logger, times(1)).error(anyString());

    logger = mock(Logger.class);
    LogHelper.logResponse(HttpStatus.BAD_REQUEST, logger, LogLevel.INFO);
    verify(logger, times(1)).info(anyString());

    logger = mock(Logger.class);
    LogHelper.logResponse(HttpStatus.BAD_REQUEST, logger, LogLevel.DEBUG);
    verify(logger, times(1)).debug(anyString());
  }
}
