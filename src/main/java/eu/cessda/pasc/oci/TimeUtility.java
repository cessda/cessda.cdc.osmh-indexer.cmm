/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

@Slf4j
@UtilityClass
public class TimeUtility {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .optionalStart().appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR)
        .optionalStart().appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH)
        .optionalStart().appendLiteral('T').append(DateTimeFormatter.ISO_TIME)
        .toFormatter();

    private static final DateTimeFormatter NESSTAR_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR)
        .appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH)
        .appendLiteral('T').append(DateTimeFormatter.ISO_LOCAL_TIME)
        .appendOffset("+HHMM", "")
        .toFormatter();

    /**
     * Attempts to parse the date string into an instance of {@link T} using multiple expected date formats.
     * <p>
     * The type {@link T} is determined by the return type of the {@code formatExtractor} function.
     *
     * @param dateString the date string to parse.
     * @param formatExtractor a function to convert the {@link TemporalAccessor} into {@link T}.
     * @return the time format.
     * @throws DateTimeParseException if unable to parse the date string.
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    public static <T> T getTimeFormat(String dateString, Function<TemporalAccessor, T> formatExtractor) {
        TemporalAccessor temporalAccessor;
        try {
            // Parse using the standard ISO formats
            temporalAccessor = DATE_TIME_FORMATTER.parse(dateString);
        } catch (DateTimeParseException e) {
            try {
                // Try parsing using the NESSTAR date format
                temporalAccessor = NESSTAR_DATE_TIME_FORMATTER.parse(dateString);
            } catch (DateTimeParseException ne) {
                // Suppress the exception thrown by the NESSTAR formatter
                e.addSuppressed(ne);
                throw e;
            }
        }
        return formatExtractor.apply(temporalAccessor);
    }
}
