/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Represents errors when parsing date strings into {@link LocalDateTime} objects.
 */
public class DateNotParsedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String dateString;
    private final List<String> expectedDateFormats;

    /**
     * Constructs a new {@link DateNotParsedException} with the specified date string,
     * expected date formats and cause.
     *
     * @param dateString          the date string that caused this exception.
     * @param expectedDateFormats the expected date formats.
     * @param cause               the {@link Throwable} that caused this exception.
     */
    DateNotParsedException(String dateString, String[] expectedDateFormats, ParseException cause) {
        super(
            String.format("Failed parsing using expected date formats %s: %s",
                Arrays.toString(expectedDateFormats), cause.toString()
            ),
            cause
        );
        this.dateString = dateString;
        this.expectedDateFormats = List.of(expectedDateFormats);
    }

    /**
     * Get the date string that caused this exception.
     */
    public String getDateString() {
        return dateString;
    }

    /**
     * Get the expected date formats used by the parser. The returned list is immutable.
     */
    public List<String> getExpectedDateFormats() {
        return expectedDateFormats;
    }
}
