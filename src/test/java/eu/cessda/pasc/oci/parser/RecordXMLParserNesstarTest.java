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
package eu.cessda.pasc.oci.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.exception.HarvesterException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.http.HttpClient;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class RecordXMLParserNesstarTest {
    private static final String STUDY_IDENTIFIER = "http://fors-getdata.unil.ch:80/obj/fStudy/ch.sidos.ddi.468.7773";
    private static final Map<String, String> ABSTRACT_FIXTURE = Map.of(
        "de", "de de",
        "fi", "Haastattelu",
        "en", "First: English abstract<br>Second: English abstract",
        "xy", "Jyväskylä<br>Jyväskylä"
    );
    private static final Map<String, String> TITLE_FIXTURE = Map.of(
        "xy", "European Social Survey in Switzerland - 2004",
        "yy", "Enquête sociale européenne"
    );

    private final Repo nesstarRepo = ReposTestData.getNSDRepo();
    private final URI fullRecordURL = URI.create(nesstarRepo.getUrl() + "?verb=GetRecord&identifier=" + URLEncoder.encode(STUDY_IDENTIFIER, UTF_8) + "&metadataPrefix=oai_ddi");
    private final Record recordHeader = new Record(RecordHeader.builder().identifier(STUDY_IDENTIFIER).build(), null);
    
    private final HttpClient httpClient = Mockito.mock(HttpClient.class);
    private final CMMStudyMapper cmmStudyMapper = new CMMStudyMapper();
    private final CMMStudyConverter cmmConverter = new CMMStudyConverter();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyComplaintCmmDdiRecord() throws IOException, ProcessingException, JSONException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordURL))
            .willReturn(ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_compliant_cmm_nesstar.xml"));

        // When
        var result = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);

        // Then
        then(result).isNotNull();
        validateCMMStudyResultAgainstSchema(result);
        assertFieldsAreExtractedAsExpected(result);
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord() throws IOException, ProcessingException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordURL))
            .willReturn(ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_compliant_cmm_nesstar.xml"));

        // When
        var record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);

        // Then
        then(record).isNotNull();
        validateCMMStudyResultAgainstSchema(record);
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldOnlyExtractSingleDateAsStartDateForRecordsWithASingleDateAttr() throws IOException, ProcessingException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordURL))
            .willReturn(ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_compliant_cmm_nesstar_single_date.xml"));

        // When
        var record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);
        then(record).isNotNull();
        validateCMMStudyResultAgainstSchema(record);
        var jsonString = cmmConverter.toJsonString(record);
        var actualTree = mapper.readTree(jsonString);

        // Then
        then(actualTree.get("dataCollectionPeriodStartdate").asText()).isEqualTo("2004-09-16");
        then(actualTree.get("dataCollectionPeriodEnddate")).isNull();
        then(actualTree.get("dataCollectionYear").asInt()).isEqualTo(2004);
    }

    @Test
    public void shouldExtractDefaultLanguageFromCodebookXMLLagIfPresent() throws HarvesterException, IOException {

        // Given
        var expectedCmmStudyJsonString = ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");
        given(httpClient.getInputStream(fullRecordURL)).willReturn(expectedCmmStudyJsonString);

        // When
        var record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);

        // Then
        then(record.getTitleStudy().containsKey("xy")).isTrue();
        then(record.getAbstractField().containsKey("xy")).isTrue();
        then(record.getKeywords().containsKey("xy")).isTrue();
        then(record.getDataAccessFreeTexts().containsKey("xy")).isTrue();
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithRepeatedAbstractConcatenated() throws HarvesterException, IOException, ProcessingException {

        // Given
        given(httpClient.getInputStream(fullRecordURL))
            .willReturn(ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_compliant_cmm_nesstar_repeated_abstract.xml"));

        // When
        var record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);

        // Then
        then(record).isNotNull();
        then(record.getAbstractField().size()).isEqualTo(4);
        then(record.getAbstractField()).isEqualTo(ABSTRACT_FIXTURE);
        validateCMMStudyResultAgainstSchema(record);
    }

    @Test // https://bitbucket.org/cessda/cessda.cdc.version2/issues/135
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithOutParTitleWhenThereIsALangDifferentFromDefault() throws HarvesterException, IOException, ProcessingException {

        // Given
        given(httpClient.getInputStream(fullRecordURL))
            .willReturn(ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_compliant_cmm_nesstar_with_perTitl_xml_lang.xml"));

        // When
        var record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);

        // Then
        then(record).isNotNull();
        then(record.getTitleStudy().size()).isEqualTo(2);
        then(record.getTitleStudy()).isEqualTo(TITLE_FIXTURE);
        validateCMMStudyResultAgainstSchema(record);
    }

    @Test()
    public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord_MarkedAsNotActive()
        throws HarvesterException, IOException {

        // Given
        given(httpClient.getInputStream(fullRecordURL)).willReturn(
            ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_nesstar_deleted_record.xml")
        );

        // When
        var record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);

        // Then
        then(record).isNotNull();
        then(record.isActive()).isFalse();
    }

    @Test(expected = OaiPmhException.class)
    public void shouldThrowExceptionForRecordWithErrorElement() throws HarvesterException, IOException {

        // Given
        given(httpClient.getInputStream(fullRecordURL)).willReturn(
            ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_nesstar_record_with_error.xml")
        );

        // When
        new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);

        // Then --> an exception is thrown.
    }

    @Test
    public void shouldExtractAllRequiredCMMFieldsForAGivenAUKDSRecord() throws HarvesterException, IOException, ProcessingException, JSONException {

        // Given
        given(httpClient.getInputStream(fullRecordURL)).willReturn(
            ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_compliant_cmm_nesstar.xml")
        );

        // When
        var result = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(nesstarRepo, recordHeader);

        // Then
        then(result).isNotNull();
        validateCMMStudyResultAgainstSchema(result);
        assertThatCmmRequiredFieldsAreExtracted(result);
    }

    private void validateCMMStudyResultAgainstSchema(CMMStudy record) throws IOException, ProcessingException {

        then(record.isActive()).isTrue(); // No need to carry on validating other fields if marked as inActive

        var jsonString = cmmConverter.toJsonString(record);

        var jsonNodeRecord = JsonLoader.fromString(jsonString);
        var schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/json/schema/CMMStudySchema.json");

        var validate = schema.validate(jsonNodeRecord);
        if (!validate.isSuccess()) {
            fail("Validation not successful: " + validate);
        }
    }

    @SuppressWarnings("PreferJavaTimeOverload")
    private void assertFieldsAreExtractedAsExpected(CMMStudy record) throws JSONException, IOException {

        var jsonString = cmmConverter.toJsonString(record);
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record_nesstar.json");
        final var actualTree = mapper.readTree(jsonString);
        final var expectedTree = mapper.readTree(expectedJson);

        // This following could be compared with one single Uber Json compare, but probably best this way to easily know
        // which field test assertion line below that fails.
        then(actualTree.get("publicationYear")).isEqualTo(expectedTree.get("publicationYear"));
        then(actualTree.get("dataCollectionPeriodStartdate")).isEqualTo(expectedTree.get("dataCollectionPeriodStartdate"));
        then(actualTree.get("dataCollectionPeriodEnddate")).isEqualTo(expectedTree.get("dataCollectionPeriodEnddate"));
        then(actualTree.get("dataCollectionYear")).isEqualTo(expectedTree.get("dataCollectionYear"));
        assertEquals(expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), false);
        assertEquals(expectedTree.get("classifications").toString(), actualTree.get("classifications").toString(), false);
        assertEquals(expectedTree.get("keywords").toString(), actualTree.get("keywords").toString(), false);
        assertEquals(expectedTree.get("typeOfTimeMethods").toString(), actualTree.get("typeOfTimeMethods").toString(), false);
        assertEquals(expectedTree.get("studyAreaCountries").toString(), actualTree.get("studyAreaCountries").toString(), false);
        assertEquals(expectedTree.get("pidStudies").toString(), actualTree.get("pidStudies").toString(), false);
        assertEquals(expectedTree.get("unitTypes").toString(), actualTree.get("unitTypes").toString(), false);
        assertEquals(expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), false);
        assertEquals(expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), false);
        assertEquals(expectedTree.get("creators").toString(), actualTree.get("creators").toString(), false);
        assertEquals(expectedTree.get("samplingProcedureFreeTexts").toString(), actualTree.get("samplingProcedureFreeTexts").toString(), false);
        assertEquals(expectedTree.get("typeOfModeOfCollections").toString(), actualTree.get("typeOfModeOfCollections").toString(), false);
        assertEquals(expectedTree.get("dataCollectionFreeTexts").toString(), actualTree.get("dataCollectionFreeTexts").toString(), false);
        assertEquals(expectedTree.get("dataAccessFreeTexts").toString(), actualTree.get("dataAccessFreeTexts").toString(), false);
        assertEquals(expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), false);
    }

    private void assertThatCmmRequiredFieldsAreExtracted(CMMStudy record) throws JSONException, IOException {

        var jsonString = cmmConverter.toJsonString(record);
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record_nesstar.json");
        final var actualTree = mapper.readTree(jsonString);
        final var expectedTree = mapper.readTree(expectedJson);

        // CMM Model Schema required fields
        assertEquals(expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), true);
        assertEquals(expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), true);
        assertEquals(expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), true);
        then(expectedTree.get("studyNumber").toString()).isEqualTo(actualTree.get("studyNumber").toString());
        assertEquals(expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), true);
    }

    @Test
    public void shouldOverrideGlobalLanguageDefaultIfAPerRepositoryOverrideIsSpecified() throws HarvesterException, IOException, ProcessingException {

        // Given
        given(httpClient.getInputStream(fullRecordURL)).willReturn(
            ResourceHandler.getResourceAsStream("xml/nesstar/synthetic_compliant_cmm_nesstar_no_language.xml")
        );

        var langRepo = ReposTestData.getNSDRepo();
        langRepo.setDefaultLanguage("zz");

        // When
        var result = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(langRepo, recordHeader);

        then(result).isNotNull();
        validateCMMStudyResultAgainstSchema(result);

        // Assert the language is as expected
        Assert.assertNotNull(result.getTitleStudy().get("zz"));
    }
}
