/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
 * @author moses AT doraventures DOT com
 */
@Slf4j
@Service
@Getter
public class CMMStudyOfLanguageConverter {

    private final ObjectReader reader;
    private final ObjectWriter writer;

    public CMMStudyOfLanguageConverter() {
        this(new ObjectMapper());
    }

    @Autowired
    private CMMStudyOfLanguageConverter(ObjectMapper mapper) {
        reader = mapper.readerFor(CMMStudyOfLanguage.class);
        writer = mapper.writerFor(CMMStudyOfLanguage.class);
    }

    public CMMStudyOfLanguage fromJsonString(String json) throws IOException {
        return reader.readValue(json);
    }
}
