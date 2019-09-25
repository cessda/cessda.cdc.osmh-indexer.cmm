/*
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
package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Publisher pojo to hold
 * - "abbr": "Control Value(CV) for institution abbreviation.",
 * - "publisher": "e.g The Social Science Data Archive"
 *
 * Publisher value can be in multiple language translations.
 *
 * @author moses AT doraventures DOT com
 */
@Builder
@Getter
public class Publisher {
  @JsonProperty("abbr")
  private String iso2LetterCode;
  @JsonProperty("publisher")
  private String publisher;
}
