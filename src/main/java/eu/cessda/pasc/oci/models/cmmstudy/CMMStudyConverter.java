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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

/**
 * CMMStudy Serialize/deserialize helpers.
 * <p>
 * Then you can deserialize a JSON string with
 * <pre>
 * {@code
 *  CMMStudy data = Converter.fromJsonString("{\"json\":\"String\"}");
 *  }
 * </pre>
 *
 * @author moses AT doravenetures DOT com
 */
public class CMMStudyConverter {

  // Serialize/deserialize helpers

  public static CMMStudy fromJsonString(String json) throws IOException {
    return getObjectReader().readValue(json);
  }

  private static ObjectReader reader;

  private static void instantiateMapper() {
    ObjectMapper mapper = new ObjectMapper();
    reader = mapper.readerFor(CMMStudy.class);
  }

  private static ObjectReader getObjectReader() {
    if (reader == null) instantiateMapper();
    return reader;
  }
}
