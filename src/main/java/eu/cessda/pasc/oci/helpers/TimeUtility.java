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
package eu.cessda.pasc.oci.helpers;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

@Slf4j
@UtilityClass
public class TimeUtility {

  private static final String[] EXPECTED_DATE_FORMATS = new String[]{"yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ssZ"};

  /**
   * Attempts to pass date string using multiple expected date formats into a LocalDateTime.
   * If all fails return an Optional.empty().
   *
   * @param dateString to parse as a LocalDateTime.
   * @return the Optional of LocalDateTime or of empty.
   */
  public static Optional<LocalDateTime> getLocalDateTime(String dateString) {
    Optional<LocalDateTime> recordLastModifiedZoneDateTime;

    try {
      Date date = DateUtils.parseDate(dateString, EXPECTED_DATE_FORMATS);
      recordLastModifiedZoneDateTime = Optional.of(date.toInstant()
              .atZone(ZoneOffset.UTC)
          .toLocalDateTime());
      return recordLastModifiedZoneDateTime;
    } catch (ParseException | IllegalArgumentException e) {
      String formatMsg = "Cannot parse date string [{}] using expected date formats [{}], Exception Message [{}]";
      log.error(formatMsg, dateString, EXPECTED_DATE_FORMATS, e.getMessage());
      recordLastModifiedZoneDateTime = Optional.empty();
    }
    return recordLastModifiedZoneDateTime;
  }
}
