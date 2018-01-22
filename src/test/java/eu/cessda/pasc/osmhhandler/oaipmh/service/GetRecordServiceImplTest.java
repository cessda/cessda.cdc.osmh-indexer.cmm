package eu.cessda.pasc.osmhhandler.oaipmh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import eu.cessda.pasc.osmhhandler.oaipmh.dao.GetRecordDoa;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.helpers.FileHandler;
import eu.cessda.pasc.osmhhandler.oaipmh.mock.data.CMMStudyMock;
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

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

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
  public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord()
      throws IOException, ProcessingException, InternalSystemException, JSONException {

    // Given
    String repoUrl = "";
    String studyIdentifier = "";

    given(getRecordDoa.getRecordXML(repoUrl, studyIdentifier)).willReturn(CMMStudyMock.getDdiRecord1683());

    // When
    CMMStudy record = recordService.getRecord(repoUrl, studyIdentifier);

    then(record).isNotNull();
    validateCMMStudyAgainstSchema(record);
  }

  private void validateCMMStudyAgainstSchema(CMMStudy record) throws IOException, ProcessingException, JSONException {

    String jsonString = CMMConverter.toJsonString(record);

    JSONObject json = new JSONObject(jsonString); // Convert text to object
    System.out.println("RETRIEVED STUDY JSON: \n" + json.toString(4));


    JsonNode jsonNodeRecord = JsonLoader.fromString(jsonString);
    final JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/json/schema/CMMStudySchema.json");

    ProcessingReport validate = schema.validate(jsonNodeRecord);
    if (!validate.isSuccess()){
      fail("Validation not successful : " + validate.toString());
    }
  }
}