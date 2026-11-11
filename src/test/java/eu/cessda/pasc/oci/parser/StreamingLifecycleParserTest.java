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
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.lifecycle.DDIObject;
import eu.cessda.pasc.oci.models.lifecycle.StudyUnit;
import org.json.JSONException;
import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static com.google.common.io.Files.getNameWithoutExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class StreamingLifecycleParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ParserTestUtilities utils = new ParserTestUtilities(objectMapper);

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyFragmentRecord() throws IOException, XMLStreamException, ProcessingException, JSONException, URISyntaxException, UnsupportedDDIException {
        // Given
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record_ddi_3_fragments.json");
        var repo = ReposTestData.getUKDSRepo();
        var recordXML = ResourceHandler.getResource("xml/ddi_3_3/compliant_fragments_cmm_ddi_3_3.xml");

        // When
        var xmlInputFactory = XMLInputFactory.newFactory();
        var source = new StreamSource(recordXML.openStream(), recordXML.toString());

        // Parse the document
        var parser = StreamingLifecycleParser.parseDocument(xmlInputFactory, source);

        /*
         * OAI-PMH Request
         */
        var uri = parser.getRequest().map(URI::create).orElse(null);

        /*
         * OAI-PMH Header
         */
        var recordHeader = parser.getRecordHeader();
        final String studyNumber;
        final String lastModified;
        if (recordHeader.isPresent()) {
            var h = recordHeader.get();
            studyNumber = h.identifier();
            lastModified = h.datestamp();
        } else {
            // Derive the study number from the file name
            studyNumber = getNameWithoutExtension(recordXML.toString());
            lastModified = OffsetDateTime.now(ZoneId.systemDefault()).toString();
        }

        var objectsByType = parser.getObjectsById().values();

        // Should parse all supported types of objects
        assertThat(objectsByType).hasOnlyElementsOfType(DDIObject.class);

        // Get top level study unit, should only be 1
        var studyUnitList = parser.getObjectsByType(StudyUnit.class);
        assertThat(studyUnitList).hasSize(1);

        var allComponents = parser.getObjectsById();

        // Get study unit
        var studyUnit = studyUnitList.getFirst();

        // Get study
        var streamingMapper = new StreamingLifecycleMapper();
        var cmmStudy = streamingMapper.parseFragmentedStudy(repo, uri, studyNumber, lastModified, allComponents, studyUnit);

        utils.validateCMMStudyResultAgainstSchema(cmmStudy);

        String actualJson = objectMapper.writeValueAsString(cmmStudy);

        // Check if the JSON generated differs from the expected source
        assertEquals(expectedJson, actualJson, true);
    }
}
