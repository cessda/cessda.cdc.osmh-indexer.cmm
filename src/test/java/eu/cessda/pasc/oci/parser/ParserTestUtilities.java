/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;

import java.io.IOException;

import static org.assertj.core.api.Assertions.fail;

public class ParserTestUtilities {
    private final ObjectMapper objectMapper;

    public ParserTestUtilities(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateCMMStudyResultAgainstSchema(CMMStudy record) throws IOException, ProcessingException {
        var jsonString = objectMapper.writeValueAsString(record);

        var jsonNodeRecord = JsonLoader.fromString(jsonString);
        var schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/json/schema/CMMStudy.schema.json");

        var validate = schema.validate(jsonNodeRecord);
        if (!validate.isSuccess()) {
            fail("Validation not successful: " + validate);
        }
    }
}
