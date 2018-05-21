package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import org.assertj.core.api.Java6BDDAssertions;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;


/**
 * @author moses@doraventures.com
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