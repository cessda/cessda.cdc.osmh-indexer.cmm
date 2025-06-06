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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * Tests related to {@link RecordXMLParser}
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
public class RecordXMLParserTest {

    private final Repo repo = ReposTestData.getUKDSRepo();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ParserTestUtilities utils = new ParserTestUtilities(objectMapper);

    // Class under test
    private final CMMStudyMapper cmmStudyMapper = new CMMStudyMapper();

    public RecordXMLParserTest() throws IOException {
        // Needed because TimeUtility only works properly in UTC timezones
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyCompliantCmmDdiRecord() throws IOException, ProcessingException, JSONException, IndexerException, URISyntaxException {
        // Given
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record.json");
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/synthetic_compliant_cmm.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        then(result).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(result.getFirst());

        String actualJson = objectMapper.writeValueAsString(result.getFirst());

        // Check if the JSON generated differs from the expected source
        assertEquals(expectedJson, actualJson, true);
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldHarvestedContentForLanguageSpecificDimensionFromElementWithCorrectXmlLangAttribute() throws IOException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/oai-fsd_uta_fi-FSD3187.xml");

        // When
        var optionalResult = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        then(optionalResult).hasSize(1);

        var result = optionalResult.getFirst();

        // Verifies timeMeth extraction
        then(result.typeOfTimeMethods().size()).isEqualTo(2);
        then(result.typeOfTimeMethods().get("fi").getFirst().term()).isEqualTo("Pitkittäisaineisto: trendi/toistuva poikkileikkausaineisto");
        then(result.typeOfTimeMethods().get("en").getFirst().term()).isEqualTo("Longitudinal: Trend/Repeated cross-section");

        // Verifies unitTypes extraction
        then(result.unitTypes().size()).isEqualTo(2);
        then(result.unitTypes().get("fi").getFirst().term()).isEqualTo("Henkilö");
        then(result.unitTypes().get("en").getFirst().term()).isEqualTo("Individual");
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_1683.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        // Then
        then(record).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(record.getFirst());
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldOnlyExtractSingleDateAsStartDateForRecordsWithASingleDateAttr() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_1683.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));
        then(record).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(record.getFirst());
        final ObjectMapper mapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(record.getFirst());
        final JsonNode actualTree = mapper.readTree(jsonString);

        then(actualTree.get("dataCollectionPeriodStartdate").asText()).isEqualTo("1976-01-01T00:00:00Z");
        then(actualTree.get("dataCollectionPeriodEnddate")).isNull();
        then(actualTree.get("dataCollectionYear").asInt()).isEqualTo(1976);
    }

    @Test
    public void shouldExtractDefaultLanguageFromCodebookXMLLagIfPresent() throws IOException, JSONException, IndexerException, URISyntaxException {

        // Given
        String expectedCmmStudyJsonString = ResourceHandler.getResourceAsString("json/ddi_record_1683_with_codebookXmlLag.json");
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_1683_with_codebookXmlLag.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));
        String actualCmmStudyJsonString = objectMapper.writeValueAsString(record.getFirst());

        // then
        assertEquals(expectedCmmStudyJsonString, actualCmmStudyJsonString, false);
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithRepeatedAbstractConcatenated() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        Map<String, String> expectedAbstract = new HashMap<>();
        expectedAbstract.put("de", "de de");
        expectedAbstract.put("fi", "Haastattelu<br>Jyväskylä");
        expectedAbstract.put("en", "1. The data<br>2. The datafiles");

        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_2305_fsd_repeat_abstract.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        then(record).hasSize(1);
        then(record.getFirst().abstractField().size()).isEqualTo(3);
        then(record.getFirst().abstractField()).isEqualTo(expectedAbstract);
        utils.validateCMMStudyResultAgainstSchema(record.getFirst());
    }

    @Test // https://github.com/cessda/cessda.cdc.versions/issues/135
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithOutParTitleWhenThereIsALangDifferentFromDefault() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        Map<String, String> expectedTitle = new HashMap<>();
        expectedTitle.put("en", "Machinery of Government, 1976-1977");
        expectedTitle.put("no", "2 - Et Machinery of Government, 1976-1977");
        expectedTitle.put("yy", "Enquête sociale européenne");

        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_1683.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        then(record).hasSize(1);
        then(record.getFirst().titleStudy().size()).isEqualTo(3);
        then(record.getFirst().titleStudy()).isEqualTo(expectedTitle);
        utils.validateCMMStudyResultAgainstSchema(record.getFirst());
    }

    @Test()
    public void shouldReturnEmptyOptionalFromOaiPmhDDI2_5MetadataRecord_MarkedAsNotActive() throws IOException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_1031_deleted.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        // Then
        then(record).isEmpty();
    }

    @Test
    public void shouldThrowExceptionForRecordWithErrorElement() throws IOException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_WithError.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        // Then
        then(record).isEmpty();
    }

    @Test
    public void shouldExtractAllRequiredCMMFieldsForAGivenAUKDSRecord() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_ukds_example.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        then(result).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(result.getFirst());
        assertThatCmmRequiredFieldsAreExtracted(result.getFirst());
    }

    @Test
    public void shouldHaveNullDataCollectionYearWhenRequiredFieldsAreNotPreset() throws FileNotFoundException, URISyntaxException, XMLParseException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_ukds_example.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        then(result).hasSize(1);
        then(result.getFirst().dataCollectionYear()).isNull();
    }

    private void assertThatCmmRequiredFieldsAreExtracted(CMMStudy record) throws IOException {
        var expectedJson = ResourceHandler.getResource("json/ddi_record_ukds_example_extracted.json");
        final JsonNode actualTree = objectMapper.valueToTree(record);
        final JsonNode expectedTree = objectMapper.readTree(expectedJson);

        // CMM Model Schema required fields
        assertAll(
            () -> assertEquals("abstract", expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), true),
            () -> assertEquals("titleStudy", expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), true),
            () -> assertEquals("studyUrl", expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), true),
            () -> Assertions.assertEquals(expectedTree.get("studyNumber").toString(), actualTree.get("studyNumber").toString(), "studyNumber"),
            () -> assertEquals("publisher", expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), true)
        );
    }

    @Test
    public void shouldOverrideGlobalLanguageDefaultIfAPerRepositoryOverrideIsSpecified() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        var repository = ReposTestData.getUKDSLanguageOverrideRepository();

        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/ddi_record_ukds_example.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(repository, Path.of(recordXML.toURI()));

        then(result).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(result.getFirst());

        // Assert the language is as expected
        Assert.assertNotNull(result.getFirst().titleStudy().get("zz"));
    }

    @Test
    public void shouldReturnMultipleCMMStudyInstancesIfMultipleRecordsArePresent() throws IOException, JSONException, ProcessingException, URISyntaxException, XMLParseException {
        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_2_5/synthetic_list_records_response.xml");
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record.json");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(recordXML.toURI()));

        then(result).hasSize(2);

        for (var study : result) {
            utils.validateCMMStudyResultAgainstSchema(study);
        }

        var actualJson = objectMapper.writeValueAsString(result.getFirst());

        // Check if the JSON for the first study differs from the expected source
        assertEquals(expectedJson, actualJson, true);
    }

    @Test(expected = XMLParseException.class)
    public void shouldThrowIfAnIOErrorOccurs() throws FileNotFoundException, URISyntaxException, XMLParseException {
        // Given
        var invalidXML = ResourceHandler.getResource("xml/invalid-xml");

        // Expect parsing to fail
        new RecordXMLParser(cmmStudyMapper).getRecord(repo, Path.of(invalidXML.toURI()));
    }
}
