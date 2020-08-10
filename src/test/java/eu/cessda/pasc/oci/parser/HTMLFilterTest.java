/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static eu.cessda.pasc.oci.parser.HTMLFilter.cleanCharacterReturns;
import static org.assertj.core.api.BDDAssertions.then;

public class HTMLFilterTest {

  @Test
  public void shouldCleanOutHtmlReturnCharacters() {

    // Given
    String raw = "\n \"Arma sunt necessaria\" (Arms are necessary) Guns, " +
        "Gun Culture and Cultural Origins of the Second \nAmendment to the U.S. Constitution\n";

    // When
      String actualCleanText = cleanCharacterReturns(raw);

    then(actualCleanText).isEqualTo("\"Arma sunt necessaria\" (Arms are necessary) Guns, " +
        "Gun Culture and Cultural Origins of the Second Amendment to the U.S. Constitution");
  }

  @Test
  public void shouldCleanOutHtmlReturnCharactersFromMap() {

    // Given
    Map<String, String> titleMap = new HashMap<>();
    titleMap.put("en", " \n\"Arma sunt necessaria\" (Arms are necessary) Guns Constitution\n");
    titleMap.put("sv", "\n\"Arma sunt necessaria\" (Arms are necessary) Guns Constitution\n");
    titleMap.put("fi", "\n Documentation pour \"European Social Survey in Switzerland - 2004\"");

    // When
      titleMap.replaceAll((key, value) -> HTMLFilter.cleanCharacterReturns(value));

    then(titleMap.get("en")).isEqualTo("\"Arma sunt necessaria\" (Arms are necessary) Guns Constitution");
    then(titleMap.get("sv")).isEqualTo("\"Arma sunt necessaria\" (Arms are necessary) Guns Constitution");
    then(titleMap.get("fi")).isEqualTo("Documentation pour \"European Social Survey in Switzerland - 2004\"");
  }
}