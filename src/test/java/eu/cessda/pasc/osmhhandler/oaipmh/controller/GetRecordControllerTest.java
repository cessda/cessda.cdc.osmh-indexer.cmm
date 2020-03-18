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

package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
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
 * @author moses AT doraventures DOT com
 */
@SuppressWarnings("ProhibitedExceptionDeclared")
@RunWith(SpringRunner.class)
@WebMvcTest(GetRecordController.class)
@Import({APISupportedServiceImpl.class, HandlerConfigurationProperties.class})
@ActiveProfiles("test")
public class GetRecordControllerTest {

  @Autowired
  private MockMvc mockMvc;
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final CustomHandlerException mockedCustomHandlerException = new CustomHandlerException("Mocked!");

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
  public void shouldCatchAndBuildRelevantEntityMessageWhenACustomHandlerExceptionIsThrownReturn() throws Exception {

    // Given
    given(this.getRecordService.getRecord("http://kirkedata.nsd.uib.no", "StudyID222")).willThrow(mockedCustomHandlerException);

    // When
    this.mockMvc.perform(get("/v0/GetRecord/CMMStudy/StudyID222?Repository=http://kirkedata.nsd.uib.no")
            .accept(MediaType.APPLICATION_JSON_VALUE))

            // Then
            .andExpect(status().isInternalServerError())
            .andExpect(status().reason(new IsNull<>()))
            .andExpect(content().json("{\"message\":\"CustomHandlerException occurred whilst getting record message [" + mockedCustomHandlerException.toString() + "], for studyID [StudyID222]\"}"));
  }

  @Test
  public void shouldCatchAndBuildRelevantEntityMessageWhenAnExceptionIsThrownReturn() throws Exception {

    // Given
    given(this.getRecordService.getRecord("http://kirkedata.nsd.uib.no", "StudyID222")).willThrow(mockedCustomHandlerException);

    // When
    this.mockMvc.perform(get("/v0/GetRecord/CMMStudy/StudyID222?Repository=http://kirkedata.nsd.uib.no")
            .accept(MediaType.APPLICATION_JSON_VALUE))

            // Then
            .andExpect(status().isInternalServerError())
            .andExpect(status().reason(new IsNull<>()))
            .andExpect(content().json("{\"message\":\"CustomHandlerException occurred whilst getting record message [" + mockedCustomHandlerException.toString() + "], for studyID [StudyID222]\"}"));
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

  @SuppressWarnings("unchecked")
  @Test
  public void shouldReturnErrorMessageWhenJsonProcessingExceptionIsThrown() throws Exception {
    given(this.getRecordService.getRecord("http://kirkedata.nsd.uib.no", "StudyID222")).willThrow(JsonProcessingException.class);

    // When
    this.mockMvc.perform(get("/v0/GetRecord/CMMStudy/StudyID222?Repository=http://kirkedata.nsd.uib.no")
            .accept(MediaType.APPLICATION_JSON_VALUE))

            // Then
            .andExpect(status().isInternalServerError())
            .andExpect(status().reason(new IsNull<>()))
            .andExpect(content().json("{\"message\":\"JsonProcessingException occurred whilst getting record message [N/A], for studyID [StudyID222].\"}"));
  }
}