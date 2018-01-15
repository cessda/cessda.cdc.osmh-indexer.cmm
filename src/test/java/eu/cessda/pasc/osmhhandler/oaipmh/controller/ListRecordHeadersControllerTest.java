package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.mock.data.RecordHeadersMock;
import eu.cessda.pasc.osmhhandler.oaipmh.models.response.RecordHeader;
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedServiceImpl;
import eu.cessda.pasc.osmhhandler.oaipmh.service.ListRecordHeadersServiceImpl;
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
import java.util.List;
import java.util.Map;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.THE_GIVEN_URL_IS_NOT_FOUND;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.UNSUPPORTED_API_VERSION;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ListRecordHeadersController.class)
@Import({APISupportedServiceImpl.class, PaSCHandlerOaiPmhConfig.class})
@ActiveProfiles("test")
public class ListRecordHeadersControllerTest {

  @Autowired
  private MockMvc mockMvc;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @MockBean
  ListRecordHeadersServiceImpl listRecordHeadersService;

  @Test
  public void shouldReturnListRecordHeadersSuccessfully() throws Exception {

    // Given
    given(this.listRecordHeadersService.getRecordHeaders("http://kirkedata.nsd.uib.no"))
        .willReturn(RecordHeadersMock.getRecordHeaders());

    List<RecordHeader> expectedRecordHeaders = RecordHeadersMock.getRecordHeaders();
    String expectedRecordsJsonString = MAPPER.writeValueAsString(expectedRecordHeaders);

    // When
    this.mockMvc.perform(get("/v0/ListRecordHeaders?Repository=http://kirkedata.nsd.uib.no").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isOk())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedRecordsJsonString));
  }

  @Test
  public void shouldReturnErrorMessageForNonSupportedAPIVersion() throws Exception {

    // Given
    Map<String, String> message = new HashMap<>();
    message.put("message", UNSUPPORTED_API_VERSION);
    String expectedMessage = MAPPER.writeValueAsString(message);

    // When
    this.mockMvc.perform(get("/v122/ListRecordHeaders/?Repository=http://kirkedata.nsd.uib.no").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isBadRequest())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedMessage));
  }

  @Test
  public void shouldReturnErrorMessageForInvalidPathThatStartsWithANonSupportedAPIVersion() throws Exception {

    // Given
    Map<String, String> message = new HashMap<>();
    message.put("message", THE_GIVEN_URL_IS_NOT_FOUND);
    String expectedMessage = MAPPER.writeValueAsString(message);

    // When
    this.mockMvc.perform(get("/v122/ListRecordHeaders/xxx?Repository=http://kirkedata.nsd.uib.no").accept(MediaType.APPLICATION_JSON_VALUE))

        // Then
        .andExpect(status().isNotFound())
        .andExpect(status().reason(new IsNull<>()))
        .andExpect(content().json(expectedMessage));
  }
}