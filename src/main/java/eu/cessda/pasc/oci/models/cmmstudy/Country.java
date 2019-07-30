/**
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Country pojo to hold
 * - "abbr": "ISO 3166 2-letter code for country 1",
 * - "country": "The name of the country in a given Language"
 *
 * @author moses@doraventures.com
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Country {
  @JsonProperty("abbr")
  private String iso2LetterCode;
  @JsonProperty("country")
  private String countryName;
}
