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

package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.EXPECTED_DATE_FORMATS;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.UTC_ZONE_ID;

@Slf4j
@UtilityClass
class TimeUtility {

  /**
   * Attempts to pass date string using multiple expected date formats into a LocalDateTime.
   * If all fails return an Optional.empty().
   *
   * @param dateString to parse as a LocalDateTime.
   * @return the Optional of LocalDateTime or of empty.
   */
  static Optional<LocalDateTime> getLocalDateTime(String dateString) {
    Optional<LocalDateTime> recordLastModifiedZoneDateTime;

    try {
      Date date = DateUtils.parseDate(dateString, EXPECTED_DATE_FORMATS);
      recordLastModifiedZoneDateTime = Optional.of(date.toInstant().atZone(UTC_ZONE_ID).toLocalDateTime());
      return recordLastModifiedZoneDateTime;
    } catch (ParseException | IllegalArgumentException e) {
      log.error("Cannot parse date string [{}] using expected date formats [{}], Exception Message [{}]",
              dateString,
              EXPECTED_DATE_FORMATS,
              e);
      recordLastModifiedZoneDateTime = Optional.empty();
    }
    return recordLastModifiedZoneDateTime;
  }


  static Function<String, Optional<Integer>> dataCollYearDateFunction() {

    return dateString -> {
      Optional<LocalDateTime> localDateTime = TimeUtility.getLocalDateTime(dateString);
      if (localDateTime.isPresent()) {
        int yearValue = localDateTime.get().getYear();
        return Optional.of(yearValue);
      }
      return Optional.empty();
    };
  }
}