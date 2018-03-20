package eu.cessda.pasc.oci.helpers;

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

  @Test
  public void shouldReturnMissingForInvalidDateValue() {

    // Given
    Optional<LocalDateTime> localDateTime = TimeUtility.getLocalDateTime("invalid-date-string");

    // When
    then(localDateTime.isPresent()).isFalse();
  }
}