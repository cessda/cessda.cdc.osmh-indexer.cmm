package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedServiceImpl;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
@WebMvcTest(SupportedAPIVersionsController.class)
@Import({APISupportedServiceImpl.class, HandlerConfigurationProperties.class})
@ActiveProfiles("test")
public class SupportedAPIVersionsControllerTest {

  @Autowired
  private MockMvc mockMvc;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void shouldReturnTheSupportedVersionsSuccessfully() throws Exception {

    // Given
    List<String> mockVersionList = new ArrayList<>();
    mockVersionList.add("v0");
    String expectedVersions = MAPPER.writeValueAsString(mockVersionList);

    // When
    this.mockMvc.perform(get("/SupportedVersions").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isOk())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedVersions));
  }

  @Test
  public void shouldReturnUnSuccessfulResponsePathsBeyondTheSupportedVersionsPath() throws Exception {

    // Given
    Map<String, String> message = new HashMap<>();
    message.put("message", "The given url is not found!");
    String expectedMessage = MAPPER.writeValueAsString(message);

    // When
    this.mockMvc.perform(get("/SupportedVersions/invalid_path").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isNotFound())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedMessage));
  }
}
