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
package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import org.assertj.core.api.Java6BDDAssertions;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;


/**
 * @author moses AT doraventures DOT com
 */
public class TimeUtilityTest {


  // yyyy-MM-dd
  @Test
  public void shouldReturnExpectedDateValueForFormat() {

    // Given
    Optional<LocalDateTime> localDateTime = TimeUtility.getLocalDateTime("2018-03-20");

    // When
    if (localDateTime.isPresent()) {
      then(localDateTime.get().toString()).isEqualToIgnoringCase("2018-03-20T00:00");
    } else {
      fail("Failed to parse Date String");
    }
  }

  // String format yyyy-MM-dd'T'HH:mm:ssZ
  @Test
  public void shouldReturnExpectedDateValueForNesstarDateFormats() {

    // Given
    Optional<LocalDateTime> localDateTime = TimeUtility.getLocalDateTime("2015-05-04T22:55:30+0000");

    // When
    if (localDateTime.isPresent()) {
      then(localDateTime.get().toString()).isEqualToIgnoringCase("2015-05-04T22:55:30");
    } else {
      fail("Failed to parse Date String");
    }
  }

  // String format "yyyy-dd-MM HH:mm:ss.SSS"
  @Test
  public void shouldReturnExpectedDateValueForCustomDateFormat() {

    // Given
    Optional<LocalDateTime> localDateTime = TimeUtility.getLocalDateTime("2019-24-04 14:32:30.448");

    // When
    if (localDateTime.isPresent()) {
      then(localDateTime.get().toLocalDate().toString()).isEqualToIgnoringCase("2019-04-24");
    } else {
      fail("Failed to parse Date String");
    }
  }

  @Test
  public void shouldReturnMissingForInvalidDateValue() {

    // Given
    Optional<LocalDateTime> localDateTime = TimeUtility.getLocalDateTime("invalid-date-string");

    // When
    then(localDateTime.isPresent()).isFalse();
  }

  @Test
  public void shouldExtractDataCollYearDateFunction() {

    // Given
    final int expected = 1982;
    final String rawDate = "1982-01-01T00:00:00Z";

    // When
    Optional<Integer> actualYearDate = TimeUtility.dataCollYearDateFunction().apply(rawDate);

    Java6BDDAssertions.then(actualYearDate.isPresent()).isTrue();
    Java6BDDAssertions.then(actualYearDate.get()).isEqualTo(expected);
  }
}