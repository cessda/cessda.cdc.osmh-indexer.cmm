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
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;

import static org.assertj.core.api.BDDAssertions.then;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

@SuppressWarnings("NonApiType")
public class RecordXMLParserNesstarTest {

    private final Repo nesstarRepo = ReposTestData.getNSDRepo();

    private final CMMStudyMapper cmmStudyMapper = new CMMStudyMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ParserTestUtilities utils = new ParserTestUtilities(objectMapper);

    public RecordXMLParserNesstarTest() throws IOException {
    }

    private static HashMap<String, String> getAbstractFixture() {
        var expectedAbstract = new HashMap<String, String>();
        expectedAbstract.put("de", "de de");
        expectedAbstract.put("fi", "Haastattelu");
        expectedAbstract.put("en", "First: English abstract<br>Second: English abstract");
        expectedAbstract.put("xy", "Jyväskylä<br>Jyväskylä");
        return expectedAbstract;
    }

    private static HashMap<String, String> getTitleFixture() {
        var expectedTitle = new HashMap<String, String>();
        expectedTitle.put("xy", "European Social Survey in Switzerland - 2004");
        expectedTitle.put("yy", "Enquête sociale européenne");
        return expectedTitle;
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyCompliantCmmDdiRecord() throws IOException, ProcessingException, JSONException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(result).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(result.getFirst());

        var actualJson = objectMapper.writeValueAsString(result.getFirst());
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record_nesstar.json");

        // Compare the generated JSON to the expected result
        assertEquals("Generated JSON differs from expected JSON\n", expectedJson, actualJson, true);
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(record).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(record.getFirst());
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldOnlyExtractSingleDateAsStartDateForRecordsWithASingleDateAttr() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar_single_date.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));
        then(record).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(record.getFirst());
        var jsonString = objectMapper.writeValueAsString(record.getFirst());
        var actualTree = objectMapper.readTree(jsonString);

        // Then
        then(actualTree.get("dataCollectionPeriodStartdate").asText()).isEqualTo("2004-09-16");
        then(actualTree.get("dataCollectionPeriodEnddate")).isNull();
        then(actualTree.get("dataCollectionYear").asInt()).isEqualTo(2004);
    }

    @Test
    public void shouldExtractDefaultLanguageFromCodebookXMLLagIfPresent() throws IndexerException, IOException, URISyntaxException {

        // Given
        var expectedCmmStudyJsonString = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(expectedCmmStudyJsonString.toURI())).getFirst();

        // Then
        then(record.titleStudy()).containsKey("xy");
        then(record.abstractField()).containsKey("xy");
        then(record.keywords()).containsKey("xy");
        then(record.dataAccessFreeTexts()).containsKey("xy");
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithRepeatedAbstractConcatenated() throws IndexerException, IOException, ProcessingException, URISyntaxException {

        // Given
        var expectedAbstract = getAbstractFixture();
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar_repeated_abstract.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(record).hasSize(1);
        then(record.getFirst().abstractField().size()).isEqualTo(4);
        then(record.getFirst().abstractField()).isEqualTo(expectedAbstract);
        utils.validateCMMStudyResultAgainstSchema(record.getFirst());
    }

    @Test // https://github.com/cessda/cessda.cdc.versions/issues/135
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithOutParTitleWhenThereIsALangDifferentFromDefault() throws IndexerException, IOException, ProcessingException, URISyntaxException {

        // Given
        var expectedTitle = getTitleFixture();

        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar_with_perTitl_xml_lang.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(record).hasSize(1);
        then(record.getFirst().titleStudy().size()).isEqualTo(2);
        then(record.getFirst().titleStudy()).isEqualTo(expectedTitle);
        utils.validateCMMStudyResultAgainstSchema(record.getFirst());
    }

    @Test()
    public void shouldReturnEmptyOptionalFromOaiPmhDDI2_5MetadataRecord_MarkedAsNotActive()
        throws IndexerException, IOException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_nesstar_deleted_record.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(record).isEmpty();
    }

    @Test
    public void shouldThrowExceptionForRecordWithErrorElement() throws IndexerException, IOException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_nesstar_record_with_error.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(result).isEmpty();
    }

    @Test
    public void shouldExtractAllRequiredCMMFieldsForAGivenAUKDSRecord() throws IndexerException, IOException, ProcessingException, JSONException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(result).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(result.getFirst());
        assertThatCmmRequiredFieldsAreExtracted(result.getFirst());
    }

    private void assertThatCmmRequiredFieldsAreExtracted(CMMStudy record) throws JSONException, IOException {

        var jsonString = objectMapper.writeValueAsString(record);
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record_nesstar.json");
        final var actualTree = objectMapper.readTree(jsonString);
        final var expectedTree = objectMapper.readTree(expectedJson);

        // CMM Model Schema required fields
        assertEquals(expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), true);
        assertEquals(expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), true);
        assertEquals(expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), true);
        then(expectedTree.get("studyNumber").toString()).isEqualTo(actualTree.get("studyNumber").toString());
        assertEquals(expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), true);
    }

    @Test
    public void shouldOverrideGlobalLanguageDefaultIfAPerRepositoryOverrideIsSpecified() throws IndexerException, IOException, ProcessingException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar_no_language.xml");

        var nsdRepo = ReposTestData.getNSDRepo();
        var langRepo = new Repo(
            nsdRepo.url(),
            nsdRepo.path(),
            nsdRepo.code(),
            nsdRepo.name(),
            nsdRepo.preferredMetadataParam(),
            nsdRepo.setSpec(),
            "zz"
        );

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(langRepo, Path.of(recordXML.toURI()));

        then(result).hasSize(1);
        utils.validateCMMStudyResultAgainstSchema(result.getFirst());

        // Assert the language is as expected
        Assert.assertNotNull(result.getFirst().titleStudy().get("zz"));
    }
}
