package eu.cessda.pasc.osmhhandler.oaipmh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import eu.cessda.pasc.osmhhandler.oaipmh.FileHandler;
import eu.cessda.pasc.osmhhandler.oaipmh.dao.GetRecordDoa;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.mock.data.CMMStudyTestData;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMConverter;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GetRecordServiceImplTest {

  @MockBean
  GetRecordDoa getRecordDoa;

  @Autowired
  GetRecordService recordService;

  @Autowired
  FileHandler fileHandler;

  @Test
  public void shouldReturnValidCMMStudyRecordFromAFullyComplaintCmmDdiRecord() throws Exception {

    // Given
    given(getRecordDoa.getRecordXML("", "")).willReturn(
        CMMStudyTestData.getXMLString("xml/synthetic_compliant_cmm.xml")
    );

    // When
    CMMStudy record = recordService.getRecord("", "");

    then(record).isNotNull();
    validateCMMStudyAgainstSchema(record);
    validateContentIsExtractedAsExpected(record);
  }

  @Test
  public void shouldOnlyExtractSingleDateAsStartDateForRecordsWithASingleDateAttr() throws Exception {

    // Given
    given(getRecordDoa.getRecordXML("", "")).willReturn(
        CMMStudyTestData.getXMLString("xml/ddi_record_1683.xml")
    );

    // When
    CMMStudy record = recordService.getRecord("", "");

    then(record).isNotNull();
    validateCMMStudyAgainstSchema(record);
    final ObjectMapper mapper = new ObjectMapper();

    String jsonString = CMMConverter.toJsonString(record);
    final JsonNode actualTree = mapper.readTree(jsonString);

    then(actualTree.get("dataCollectionPeriodStartdate").asText()).isEqualTo("1976-01-01T00:00:00Z");
    then(actualTree.get("dataCollectionPeriodEnddate")).isNull();
  }

  private void validateContentIsExtractedAsExpected(CMMStudy record) throws IOException, JSONException {
    final ObjectMapper mapper = new ObjectMapper();

    String jsonString = CMMConverter.toJsonString(record);
    String expectedJson = CMMStudyTestData.getXMLString("json/synthetic_compliant_record.json");
    final JsonNode actualTree = mapper.readTree(jsonString);
    final JsonNode expectedTree = mapper.readTree(expectedJson);

    then(expectedTree.get("publicationYear").toString()).isEqualTo(actualTree.get("publicationYear").toString());
    then(expectedTree.get("dataCollectionPeriodStartdate").toString()).isEqualTo(actualTree.get("dataCollectionPeriodStartdate").toString());
    then(expectedTree.get("dataCollectionPeriodEnddate").toString()).isEqualTo(actualTree.get("dataCollectionPeriodEnddate").toString());
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

    // TODO repeat for each individual element.  Final goal is to use one single Uber Json compare
  }

  @Test
  public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord() throws Exception {

    // Given
    String repoUrl = "";
    String studyIdentifier = "";

    given(getRecordDoa.getRecordXML(repoUrl, studyIdentifier)).willReturn(
        CMMStudyTestData.getXMLString("xml/ddi_record_1683.xml")
    );

    // When
    CMMStudy record = recordService.getRecord(repoUrl, studyIdentifier);

    then(record).isNotNull();
    validateCMMStudyAgainstSchema(record);
  }

  @Test
  public void shouldReturnCMMStudyRecordWithRepeatedAbstractConcatenated() throws Exception {

    Map<String, String> expectedAbstract = new HashMap<>();
    expectedAbstract.put("de", "de de");
    expectedAbstract.put("fi", "Haastattelu+<br>Jyväskylä");
    expectedAbstract.put("en", "1. The data+<br>2. The datafiles");

    given(getRecordDoa.getRecordXML("", "")).willReturn(
        CMMStudyTestData.getXMLString("xml/ddi_record_2305_fsd_repeat_abstract.xml")
    );

    // When
    CMMStudy record = recordService.getRecord("", "");

    then(record).isNotNull();
    then(record.getAbstractField().size()).isEqualTo(3);
    then(record.getAbstractField()).isEqualTo(expectedAbstract);
    validateCMMStudyAgainstSchema(record);
  }

  @Test()
  public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord_MarkedAsNotActive()
      throws CustomHandlerException {

    // Given
    String repoUrl = "";
    String studyIdentifier = "";

    given(getRecordDoa.getRecordXML(repoUrl, studyIdentifier)).willReturn(
        CMMStudyTestData.getXMLString("xml/ddi_record_1031_deleted.xml")
    );

    // When
    CMMStudy record = recordService.getRecord(repoUrl, studyIdentifier);

    then(record).isNotNull();
    then(record.isActive()).isFalse();
  }

  @Test(expected = ExternalSystemException.class)
  public void shouldThrowExceptionForRecordWithErrorElement() throws CustomHandlerException {

    // Given
    String repoUrl = "www.myurl.com";
    String studyIdentifier = "Id12214";

    given(getRecordDoa.getRecordXML(repoUrl, studyIdentifier)).willReturn(
        CMMStudyTestData.getXMLString("xml/ddi_record_WithError.xml")
    );

    // When
    recordService.getRecord(repoUrl, studyIdentifier);

    // Then an exception is thrown.
  }

  private void validateCMMStudyAgainstSchema(CMMStudy record) throws IOException, ProcessingException, JSONException {

    then(record.isActive()).isTrue(); // No need to carry on validating other fields if marked as inActive

    String jsonString = CMMConverter.toJsonString(record);
    JSONObject json = new JSONObject(jsonString);
    System.out.println("RETRIEVED STUDY JSON: \n" + json.toString(4));

    JsonNode jsonNodeRecord = JsonLoader.fromString(jsonString);
    final JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/json/schema/CMMStudySchema.json");

    ProcessingReport validate = schema.validate(jsonNodeRecord);
    if (!validate.isSuccess()) {
      fail("Validation not successful : " + validate.toString());
    }
  }
}
