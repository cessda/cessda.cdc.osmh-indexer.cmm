/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import lombok.Getter;

import java.io.Serial;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Represents errors when parsing date strings into {@link LocalDateTime} objects.
 */
@Getter
public class DateNotParsedException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The date string that caused this exception.
     */
    private final String dateString;
    /**
     * The expected date formats used by the parser.
     */
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
            "Failed parsing using expected date formats " + Arrays.toString(expectedDateFormats) + ": " + cause.toString(),
            cause
        );
        this.dateString = dateString;
        this.expectedDateFormats = List.of(expectedDateFormats);
    }
}
