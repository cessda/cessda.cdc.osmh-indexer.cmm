package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.EXPECTED_DATE_FORMATS;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.UTC_ID;

@Slf4j
class TimeUtility {

  private TimeUtility() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

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
      recordLastModifiedZoneDateTime = Optional.of(date.toInstant()
          .atZone(ZoneId.of(UTC_ID))
          .toLocalDateTime());
      return recordLastModifiedZoneDateTime;
    } catch (ParseException | IllegalArgumentException e) {
      String formatMsg = "Cannot parse date string [{}] using expected date formats [{}], Exception Message [{}]";
      log.error(formatMsg, dateString, EXPECTED_DATE_FORMATS, e.getMessage());
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