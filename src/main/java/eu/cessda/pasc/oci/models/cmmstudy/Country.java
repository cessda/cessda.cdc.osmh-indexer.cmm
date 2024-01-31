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
package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Country pojo to hold
 * - "abbr": "ISO 3166 2-letter code for country 1",
 * - "country": "The name of the country in a given Language"
 * - "searchField": "The name of the country to use in the search filter."
 *
 * @param isoCode     ISO country code.
 * @param elementText The name of the country as present in the metadata.
 * @param searchField The name of the country to use in the search filter.
 *                    This is derived from the ISO country code and is always in English.
 * @author moses AT doraventures DOT com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Country(
    @JsonProperty("abbr") String isoCode,
    @JsonProperty("country") String elementText,
    @JsonProperty("searchField") String searchField
) {
    public Country(String isoCode, String elementText) {
        this(isoCode, elementText, null);
    }
}
