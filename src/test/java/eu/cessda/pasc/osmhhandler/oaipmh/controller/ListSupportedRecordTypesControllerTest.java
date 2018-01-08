package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedServiceImpl;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMVC tests for the Controller
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ListSupportedRecordTypesController.class)
@Import({APISupportedServiceImpl.class, PaSCHandlerOaiPmhConfig.class})
public class ListSupportedRecordTypesControllerTest {

  @Autowired
  private MockMvc mockMvc;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void shouldReturnTheSupportedRecordTypesSuccessfully() throws Exception {

    // Given
    String expectedRecords= "[\"StudyGroup\", \"Study\", \"Variable\", \"CMM\", \"Question\"]";

    // When
    this.mockMvc.perform(get("/v0/ListSupportedRecordTypes").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isOk())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedRecords));
  }

  @Test
  public void shouldReturnErrorMessageForUnsupportedAPI() throws Exception {

    // Given
    Map<String, String> message = new HashMap<>();
    message.put("message", "Unsupported API-version");
    String expectedMessage = MAPPER.writeValueAsString(message);

    // When
    this.mockMvc.perform(get("/v1/ListSupportedRecordTypes").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isBadRequest())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedMessage));
  }

  @Test
  public void shouldReturnErrorMessageForInvalidPath() throws Exception {

    // Given
    Map<String, String> message = new HashMap<>();
    message.put("message", "The given url is not found!");
    String expectedMessage = MAPPER.writeValueAsString(message);

    // When
    this.mockMvc.perform(get("/v0/ListSupportedRecordTypes/anythinelse").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isNotFound())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedMessage));
  }

  @Test
  public void shouldReturnErrorMessageForInvalidPathThatStartsWithANonSupportedAPIVesion() throws Exception {

    // Given
    Map<String, String> message = new HashMap<>();
    message.put("message", "The given url is not found!");
    String expectedMessage = MAPPER.writeValueAsString(message);

    // When
    this.mockMvc.perform(get("/v1_non_supported/ListSupportedRecordTypes/anythinelse").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isNotFound())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedMessage));
  }
}
