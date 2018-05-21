package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import org.assertj.core.api.Java6BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Parameterised test for various date possibilities
 *
 * @author moses@doraventures.com
 */
@RunWith(Parameterized.class)
public class TimeUtilDateExtractionParameterisedTest {

  private String fInput;
  private int fExpected;

  public TimeUtilDateExtractionParameterisedTest(String fInput, int fExpected) {
    this.fInput = fInput;
    this.fExpected = fExpected;
  }

  // variable labels from junit v4.12
  @Parameterized.Parameters(name = "{index}: YearDateExtraction For({0}) expected to be: {1}")
  public static Collection<Object[]> data() {

    // The Test data
    return Arrays.asList(new Object[][]{
        {"1982-01-01T00:00:00Z", 1982}, // {INPUT, EXPECTED_RESULT}
        {"1915-01-01", 1915},
        {"1915-01", 1915},
        {"1915", 1915},
        {"1955", 1955},
        {"2020", 2020},
    });
  }

  @Test
  public void shouldExtractDataCollYearDateFunction() {

    // When
    Optional<Integer> actualYearDate = TimeUtility.dataCollYearDateFunction().apply(fInput);

    // Then
    Java6BDDAssertions.then(actualYearDate.isPresent()).isTrue();
    Java6BDDAssertions.then(actualYearDate.get()).isEqualTo(fExpected);
  }
}