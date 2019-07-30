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
package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for cleaning html characters
 *
 * @author moses AT doraventures DOT com
 */
class HTMLFilter {

  private HTMLFilter() throws InternalSystemException {
    throw new InternalSystemException("Unsupported operation");
  }

  static final Function<String, String> CLEAN_CHARACTER_RETURNS_STRATEGY = candidate ->
      candidate.replace("\n", "").trim();

  static final Consumer<Map<String, String>> CLEAN_MAP_VALUES = candidateMap ->
      candidateMap.replaceAll((key, value) -> CLEAN_CHARACTER_RETURNS_STRATEGY.apply(value));
}
