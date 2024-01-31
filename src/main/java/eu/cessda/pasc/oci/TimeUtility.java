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
import org.apache.commons.lang.time.DateUtils;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@UtilityClass
public class TimeUtility {

    private static final String[] EXPECTED_DATE_FORMATS = new String[]{
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-dd-MM HH:mm:ss.SSS",
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM", "yyyy"
    };

    /**
     * Attempts to parse the date string into a LocalDateTime using multiple expected date formats.
     *
     * @param dateString to parse as a {@link LocalDateTime}.
     * @return the {@link Optional} of {@link LocalDateTime}, or an {@link Optional#empty()} if the date failed to parse.
     * @throws IllegalArgumentException if the string is {@code null}
     */
    public static LocalDateTime getLocalDateTime(String dateString) throws DateNotParsedException {
        try {
            var date = DateUtils.parseDate(dateString, EXPECTED_DATE_FORMATS);
            return date.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (ParseException e) {
            throw new DateNotParsedException(dateString, EXPECTED_DATE_FORMATS, e);
        }
    }
}
