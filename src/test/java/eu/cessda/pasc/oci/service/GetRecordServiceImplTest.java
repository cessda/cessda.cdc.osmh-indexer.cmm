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

package eu.cessda.pasc.oci.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import eu.cessda.pasc.oci.dao.GetRecordDoa;
import eu.cessda.pasc.oci.exception.CustomHandlerException;
import eu.cessda.pasc.oci.exception.InternalSystemException;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.mock.data.CMMStudyTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMConverter;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GetRecordServiceImplTest {

  private final static String REPO_URL = "http://services.fsd.uta.fi/v0/oai";
  private final static String RECORD_IDENTIFIER = "http://my-example_url:80/obj/fStudy/ch.sidos.ddi.468.7773";
  private final static String FULL_RECORD_URL = REPO_URL + "?verb=GetRecord&identifier=" + RECORD_IDENTIFIER + "&metadataPrefix=oai_ddi25";
  private final CMMConverter cmmConverter = new CMMConverter();


  @MockBean
  GetRecordDoa getRecordDoa;

  @Autowired
  GetRecordService recordService;

  @Autowired
  FileHandler fileHandler;

  @Test
  public void shouldReturnValidCMMStudyRecordFromAFullyComplaintCmmDdiRecord() throws CustomHandlerException, IOException, ProcessingException {

    // Given
    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/synthetic_compliant_cmm.xml")
    );

    // When
    CMMStudy result = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);

    then(result).isNotNull();
    validateCMMStudyResultAgainstSchema(result);
    assertFieldsAreExtractedAsExpected(result);
  }

  @Test
  public void shouldHarvestedContentForLanguageSpecificDimensionFromElementWithCorrectXmlLangAttribute() throws CustomHandlerException, FileNotFoundException {

    // Given
    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/oai-fsd_uta_fi-FSD3187.xml")
    );

    // When
    CMMStudy result = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);

    then(result).isNotNull();

    // Verifies timeMeth extraction
    then(result.getTypeOfTimeMethods().size()).isEqualTo(2);
    then(result.getTypeOfTimeMethods().get("fi").get(0).getTerm()).isEqualTo("Pitkittäisaineisto: trendi/toistuva poikkileikkausaineisto");
    then(result.getTypeOfTimeMethods().get("en").get(0).getTerm()).isEqualTo("Longitudinal: Trend/Repeated cross-section");

    // Verifies unitTypes extraction
    then(result.getUnitTypes().size()).isEqualTo(2);
    then(result.getUnitTypes().get("fi").get(0).getTerm()).isEqualTo("Henkilö");
    then(result.getUnitTypes().get("en").get(0).getTerm()).isEqualTo("Individual");
  }

  @Test
  public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord() throws CustomHandlerException, IOException, ProcessingException {

    // Given
    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/ddi_record_1683.xml")
    );

    // When
    CMMStudy record = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);

    // Then
    then(record).isNotNull();
    validateCMMStudyResultAgainstSchema(record);
  }

  @Test
  public void shouldOnlyExtractSingleDateAsStartDateForRecordsWithASingleDateAttr() throws CustomHandlerException, IOException, ProcessingException {

    // Given
    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/ddi_record_1683.xml")
    );

    // When
    CMMStudy record = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);
    then(record).isNotNull();
    validateCMMStudyResultAgainstSchema(record);
    final ObjectMapper mapper = new ObjectMapper();
    String jsonString = cmmConverter.toJsonString(record);
    final JsonNode actualTree = mapper.readTree(jsonString);

    then(actualTree.get("dataCollectionPeriodStartdate").asText()).isEqualTo("1976-01-01T00:00:00Z");
    then(actualTree.get("dataCollectionPeriodEnddate")).isNull();
    then(actualTree.get("dataCollectionYear").asInt()).isEqualTo(1976);
  }

  @Test
  public void shouldExtractDefaultLanguageFromCodebookXMLLagIfPresent() throws CustomHandlerException, IOException {

    // Given
    String expectedCmmStudyJsonString = CMMStudyTestData.getContent("json/ddi_record_1683_with_codebookXmlLag.json");
    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/ddi_record_1683_with_codebookXmlLag.xml")
    );

    // When
    CMMStudy record = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);
    String actualCmmStudyJsonString = cmmConverter.toJsonString(record);

    // then
    assertEquals(expectedCmmStudyJsonString, actualCmmStudyJsonString, false);
  }

  @Test
  public void shouldReturnCMMStudyRecordWithRepeatedAbstractConcatenated() throws CustomHandlerException, IOException, ProcessingException {

    Map<String, String> expectedAbstract = new HashMap<>();
    expectedAbstract.put("de", "de de");
    expectedAbstract.put("fi", "Haastattelu+<br>Jyväskylä");
    expectedAbstract.put("en", "1. The data+<br>2. The datafiles");

    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/ddi_record_2305_fsd_repeat_abstract.xml")
    );

    // When
    CMMStudy record = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);

    then(record).isNotNull();
    then(record.getAbstractField().size()).isEqualTo(3);
    then(record.getAbstractField()).isEqualTo(expectedAbstract);
    validateCMMStudyResultAgainstSchema(record);
  }

  @Test // https://bitbucket.org/cessda/cessda.cdc.version2/issues/135
  public void shouldReturnCMMStudyRecordWithOutParTitleWhenThereIsALangDifferentFromDefault() throws CustomHandlerException, IOException, ProcessingException {

    Map<String, String> expectedTitle = new HashMap<>();
    expectedTitle.put("en", "Machinery of Government, 1976-1977");
    expectedTitle.put("no", "2 - Et Machinery of Government, 1976-1977");
    expectedTitle.put("yy", "Enquête sociale européenne");

    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/ddi_record_1683.xml")
    );

    // When
    CMMStudy record = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);

    then(record).isNotNull();
    then(record.getTitleStudy().size()).isEqualTo(3);
    then(record.getTitleStudy()).isEqualTo(expectedTitle);
    validateCMMStudyResultAgainstSchema(record);
  }

  @Test()
  public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord_MarkedAsNotActive()
          throws CustomHandlerException, FileNotFoundException {

    // Given
    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/ddi_record_1031_deleted.xml")
    );

    // When
    CMMStudy record = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);

    // Then
    then(record).isNotNull();
    then(record.isActive()).isFalse();
  }

  @Test(expected = InternalSystemException.class)
  public void shouldThrowExceptionForRecordWithErrorElement() throws CustomHandlerException, FileNotFoundException {

    // Given
    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/ddi_record_WithError.xml")
    );

    // When
    recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);

    // Then an exception is thrown.
  }

  @Test
  public void shouldExtractAllRequiredCMMFieldsForAGivenAUKDSRecord() throws CustomHandlerException, IOException, ProcessingException {

    // Given
    given(getRecordDoa.getRecordXML(FULL_RECORD_URL)).willReturn(
            CMMStudyTestData.getContentAsStream("xml/ddi_record_ukds_example.xml")
    );

    // When
    CMMStudy result = recordService.getRecord(REPO_URL, RECORD_IDENTIFIER);

    then(result).isNotNull();
    validateCMMStudyResultAgainstSchema(result);
    assertThatCmmRequiredFieldsAreExtracted(result);
  }

  private void validateCMMStudyResultAgainstSchema(CMMStudy record) throws IOException, ProcessingException {

    then(record.isActive()).isTrue(); // No need to carry on validating other fields if marked as inActive

    String jsonString = cmmConverter.toJsonString(record);
    JSONObject json = new JSONObject(jsonString);
    System.out.println("RETRIEVED STUDY JSON: \n" + json.toString(4));

    JsonNode jsonNodeRecord = JsonLoader.fromString(jsonString);
    final JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/json/schema/CMMStudySchema.json");

    ProcessingReport validate = schema.validate(jsonNodeRecord);
    if (!validate.isSuccess()) {
      fail("Validation not successful : " + validate.toString());
    }
  }

  private void assertFieldsAreExtractedAsExpected(CMMStudy record) throws IOException {

    final ObjectMapper mapper = new ObjectMapper();
    String jsonString = cmmConverter.toJsonString(record);
    String expectedJson = CMMStudyTestData.getContent("json/synthetic_compliant_record.json");
    final JsonNode actualTree = mapper.readTree(jsonString);
    final JsonNode expectedTree = mapper.readTree(expectedJson);

    // This following could be compared with one single Uber Json compare, but probably best this way to easily know
    // which field test assertion line below that fails.
    then(expectedTree.get("publicationYear").toString()).isEqualTo(actualTree.get("publicationYear").toString());
    then(expectedTree.get("dataCollectionPeriodStartdate").toString()).isEqualTo(actualTree.get("dataCollectionPeriodStartdate").toString());
    then(expectedTree.get("dataCollectionPeriodEnddate").toString()).isEqualTo(actualTree.get("dataCollectionPeriodEnddate").toString());
    then(expectedTree.get("dataCollectionYear").asInt()).isEqualTo(actualTree.get("dataCollectionYear").asInt());
    assertEquals(expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), true);
    assertEquals(expectedTree.get("classifications").toString(), actualTree.get("classifications").toString(), true);
    assertEquals(expectedTree.get("keywords").toString(), actualTree.get("keywords").toString(), true);
    assertEquals(expectedTree.get("typeOfTimeMethods").toString(), actualTree.get("typeOfTimeMethods").toString(), true);
    assertEquals(expectedTree.get("studyAreaCountries").toString(), actualTree.get("studyAreaCountries").toString(), true);
    assertEquals(expectedTree.get("pidStudies").toString(), actualTree.get("pidStudies").toString(), true);
    assertEquals(expectedTree.get("unitTypes").toString(), actualTree.get("unitTypes").toString(), true);
    assertEquals(expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), true);
    assertEquals(expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), true);
    assertEquals(expectedTree.get("creators").toString(), actualTree.get("creators").toString(), true);
    assertEquals(expectedTree.get("fileLanguages").toString(), actualTree.get("fileLanguages").toString(), true);
    assertEquals(expectedTree
        .get("typeOfSamplingProcedures").toString(), actualTree.get("typeOfSamplingProcedures").toString(), true);
    assertEquals(expectedTree
        .get("samplingProcedureFreeTexts").toString(), actualTree.get("samplingProcedureFreeTexts").toString(), true);
    assertEquals(expectedTree
        .get("typeOfModeOfCollections").toString(), actualTree.get("typeOfModeOfCollections").toString(), true);
    assertEquals(expectedTree
        .get("dataCollectionFreeTexts").toString(), actualTree.get("dataCollectionFreeTexts").toString(), true);
    assertEquals(expectedTree
        .get("dataAccessFreeTexts").toString(), actualTree.get("dataAccessFreeTexts").toString(), true);
    assertEquals(expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), true);
    assertEquals(expectedTree.get("studyXmlSourceUrl").toString(), actualTree.get("studyXmlSourceUrl").toString(), true);
  }

  private void assertThatCmmRequiredFieldsAreExtracted(CMMStudy record) throws IOException {

    final ObjectMapper mapper = new ObjectMapper();
    String jsonString = cmmConverter.toJsonString(record);
    String expectedJson = CMMStudyTestData.getContent("json/ddi_record_ukds_example_extracted.json");
    final JsonNode actualTree = mapper.readTree(jsonString);
    final JsonNode expectedTree = mapper.readTree(expectedJson);

    // CMM Model Schema required fields
    assertEquals(expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), true);
    assertEquals(expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), true);
    assertEquals(expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), true);
    then(expectedTree.get("studyNumber").toString()).isEqualTo(actualTree.get("studyNumber").toString());
    assertEquals(expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), true);
  }
}
