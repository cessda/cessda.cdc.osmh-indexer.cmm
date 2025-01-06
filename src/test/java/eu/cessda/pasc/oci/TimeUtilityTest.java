/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
public class TimeUtilityTest {

    public TimeUtilityTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }


    @Test
    public void shouldReturnExpectedDateValue() {

        // Given
        var localDate = TimeUtility.getTimeFormat("2018-03-20", LocalDate::from);

        // When
        then(localDate.toString()).isEqualToIgnoringCase("2018-03-20");
    }

    // String format yyyy-MM-dd'T'HH:mm:ssZ
    @Test
    public void shouldReturnExpectedDateValueForNesstarDateFormats() {

        // Given
        var localDateTime = TimeUtility.getTimeFormat("2015-05-04T22:55:30+0000", LocalDateTime::from);

        // When
        then(localDateTime.toString()).isEqualToIgnoringCase("2015-05-04T22:55:30");
    }

  @Test
  public void shouldReturnMissingForInvalidDateValue() {

      // Given
      var invalid = "invalid-date-string";

      // Then
      try {
          TimeUtility.getTimeFormat(invalid, Function.identity());
          fail(DateTimeParseException.class.getName() + " is expected to throw");
      } catch (DateTimeParseException e) {
          then(e.getParsedString()).isEqualTo(invalid);
      }
  }
}
