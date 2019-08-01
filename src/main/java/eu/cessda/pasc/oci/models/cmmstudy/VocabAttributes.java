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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * VocabAttributes pojo to hold the control vocab attributes of element:
 * <p>
 * {@code
 * <keyword xml:lang="sv" ID="w8357xx45" vocab="ELSST" vocabURI="http://sv.ac.sv">
 * </keyword>
 * }
 *
 * @author moses AT doravenetures DOT com
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class VocabAttributes {
  private String vocab;
  private String vocabUri;
  private String id;
}
