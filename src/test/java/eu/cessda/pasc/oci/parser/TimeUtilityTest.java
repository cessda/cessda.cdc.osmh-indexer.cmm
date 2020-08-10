/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.parser;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
public class TimeUtilityTest {


  @Test
  public void shouldReturnExpectedDateValue() {

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

  @Test
  public void shouldReturnMissingForInvalidDateValue() {

    // Given
    Optional<LocalDateTime> localDateTime = TimeUtility.getLocalDateTime("invalid-date-string");

    // When
    then(localDateTime.isPresent()).isFalse();
  }
}
