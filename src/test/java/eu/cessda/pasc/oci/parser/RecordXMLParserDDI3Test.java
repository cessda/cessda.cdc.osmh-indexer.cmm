/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.TimeZone;

import static org.assertj.core.api.BDDAssertions.then;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * Tests related to {@link RecordXMLParser}
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
public class RecordXMLParserDDI3Test {

    private final Repo repo = ReposTestData.getUKDSRepo();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ParserTestUtilities utils = new ParserTestUtilities(objectMapper);

    // Class under test
    private final CMMStudyMapper cmmStudyMapper = new CMMStudyMapper();

    public RecordXMLParserDDI3Test() {
        // Needed because TimeUtility only works properly in UTC timezones
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyComplaintCmmDdi32Record() throws IOException, ProcessingException, JSONException, IndexerException, URISyntaxException {
        testParsing("xml/ddi_3_2/synthetic_compliant_cmm_ddi3_2.xml", "json/synthetic_compliant_record_ddi_3.json");
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyComplaintCmmDdi33Record() throws IOException, ProcessingException, JSONException, IndexerException, URISyntaxException {
        // Given
        testParsing("xml/ddi_3_3/synthetic_compliant_cmm_ddi3_3.xml", "json/synthetic_compliant_record_ddi_3.json");
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyFragmentRecord() throws IOException, ProcessingException, JSONException, IndexerException, URISyntaxException {
        // Given
        testParsing("xml/ddi_3_3/compliant_fragments_cmm_ddi_3_3.xml", "json/synthetic_compliant_record_ddi_3_fragments.json");
    }

    private void testParsing(String sourceXML, String expectedJSON) throws IOException, XMLParseException, URISyntaxException, ProcessingException, JSONException {
        // Given
        var recordXML = ResourceHandler.getResource(sourceXML);
        var expectedJson = ResourceHandler.getResourceAsString(expectedJSON);

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        then(result).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(result.getFirst());

        String actualJson = objectMapper.writeValueAsString(result.getFirst());

        // Check if the JSON generated differs from the expected source
        assertEquals(expectedJson, actualJson, true);
    }
}
