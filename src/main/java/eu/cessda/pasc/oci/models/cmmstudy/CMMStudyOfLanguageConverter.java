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
package eu.cessda.pasc.oci.models.cmmstudy;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

/**
 * CMMStudyOfLanguage Serialize/deserialize helpers.
 * <p>
 * Then you can deserialize a JSON string with
 * <pre>
 * {@code
 *  CMMStudyOfLanguage data = Converter.fromJsonString("{\"json\":\"String\"}");
 *  }
 * </pre>
 *
 * @author moses AT doravenetures DOT com
 */
@Slf4j
public class CMMStudyOfLanguageConverter {

  // Serialize/deserialize helpers

  public static CMMStudyOfLanguage fromJsonString(String json) throws IOException {
    return getObjectReader().readValue(json);
  }

  public static Optional<String> toJsonString(CMMStudyOfLanguage obj) {
    try {
      return Optional.ofNullable(getObjectWriter().writeValueAsString(obj));
    } catch (JsonProcessingException e) {
      log.error("Failed to write Object as string [{}].  Returning empty Option.", e.getMessage());
      return Optional.empty();
    }
  }

  private static ObjectReader reader;
  private static ObjectWriter writer;

  private static void instantiateMapper() {
    ObjectMapper mapper = new ObjectMapper();
    reader = mapper.readerFor(CMMStudyOfLanguage.class);
    writer = mapper.writerFor(CMMStudyOfLanguage.class);
  }

  private static ObjectReader getObjectReader() {
    if (reader == null) instantiateMapper();
    return reader;
  }

  private static ObjectWriter getObjectWriter() {
    if (writer == null) instantiateMapper();
    return writer;
  }
}
