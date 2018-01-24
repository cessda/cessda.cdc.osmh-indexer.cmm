package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedServiceImpl;
import eu.cessda.pasc.osmhhandler.oaipmh.service.GetRecordService;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.THE_GIVEN_URL_IS_NOT_FOUND;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.UNSUPPORTED_API_VERSION;
import static eu.cessda.pasc.osmhhandler.oaipmh.mock.data.CMMStudyTestData.getCMMStudy;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@WebMvcTest(GetRecordController.class)
@Import({APISupportedServiceImpl.class, PaSCHandlerOaiPmhConfig.class})
@ActiveProfiles("test")
public class GetRecordControllerTest {

  @Autowired
  private MockMvc mockMvc;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @MockBean
  GetRecordService getRecordService;

  @Test
  public void shouldReturnRecordSuccessfully() throws Exception {

    // Given
    given(this.getRecordService.getRecord("http://kirkedata.nsd.uib.no", "StudyID222"))
        .willReturn(getCMMStudy());

    String expectedCMMStudyJsonString = MAPPER.writeValueAsString(getCMMStudy());

    // When
    this.mockMvc.perform(get("/v0/GetRecord/CMMStudy/StudyID222?Repository=http://kirkedata.nsd.uib.no")
            .accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isOk())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedCMMStudyJsonString));
  }

  @Test
  public void shouldReturnErrorMessageForNonSupportedAPIVersion() throws Exception {

    // Given
    Map<String, String> message = new HashMap<>();
    String invalidVersion = "v99";
    message.put("message", String.format(UNSUPPORTED_API_VERSION, invalidVersion));
    String expectedMessage = MAPPER.writeValueAsString(message);

    // When
    this.mockMvc.perform(
        get("/" + invalidVersion + "/GetRecord/CMMStudy/StudyID222?Repository=http://kirkedata.nsd.uib.no")
            .accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isBadRequest())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedMessage));
  }

  @Test
  public void shouldReturnErrorMessageForInvalidPathThatStartsWithANonSupportedAPIVersion() throws Exception {

    // Given
    Map<String, String> message = new HashMap<>();
    String invalidVersion = "v99";
    message.put("message", THE_GIVEN_URL_IS_NOT_FOUND);
    String expectedMessage = MAPPER.writeValueAsString(message);

    // When
    this.mockMvc.perform(
        get("/" + invalidVersion + "/GetRecord/CMMInvalid/StudyID222/?Repository=http://kirkedata.nsd.uib.no")
            .accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isNotFound())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedMessage));
  }
}