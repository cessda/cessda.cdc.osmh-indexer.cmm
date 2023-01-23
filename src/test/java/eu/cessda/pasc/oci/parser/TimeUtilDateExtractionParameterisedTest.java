/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.TimeUtility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.TimeZone;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Parameterised test for various date possibilities
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(Parameterized.class)
public class TimeUtilDateExtractionParameterisedTest {

    private final String fInput;
    private final int fExpected;

    public TimeUtilDateExtractionParameterisedTest(String fInput, int fExpected) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
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
    public void shouldExtractDataCollYearDateFunction() throws DateNotParsedException {

        // When
	    var localDateTime = TimeUtility.getLocalDateTime(fInput);
	    var actualYearDate = localDateTime.getYear();

        // Then
        then(actualYearDate).isEqualTo(fExpected);
    }
}