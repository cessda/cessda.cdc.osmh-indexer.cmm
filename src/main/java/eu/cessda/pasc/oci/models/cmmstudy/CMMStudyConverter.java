/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

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
 * @author moses AT doraventures DOT com
 */
@Service
public class CMMStudyConverter {

  private final ObjectReader reader;
  private final ObjectWriter writer;

  // Serialize/deserialize helpers
  public CMMStudyConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    reader = objectMapper.readerFor(CMMStudy.class);
    writer = objectMapper.writerFor(CMMStudy.class);
  }

  @Autowired
  public CMMStudyConverter(ObjectMapper objectMapper) {
    reader = objectMapper.readerFor(CMMStudy.class);
    writer = objectMapper.writerFor(CMMStudy.class);
  }

  /**
   * Convert a JSON stream to a {@link CMMStudy} POJO.
   *
   * @param json the {@link InputStream} to parse.
   * @throws IOException if an IO error occurs when parsing the stream.
   */
  public CMMStudy fromJsonStream(InputStream json) throws IOException {
    return reader.readValue(json);
  }

  /**
   * Convert a {@link CMMStudy} to a JSON string.
   *
   * @param cmmStudy the {@link CMMStudy} to convert.
   * @throws JsonProcessingException when an error occurs creating the JSON representation.
   */
  public String toJsonString(CMMStudy cmmStudy) throws JsonProcessingException {
    return writer.writeValueAsString(cmmStudy);
  }
}
