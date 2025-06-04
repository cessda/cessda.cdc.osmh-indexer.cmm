/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import org.junit.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * @author moses AT doraventures DOT com
 */
@SuppressWarnings("DirectInvocationOnMock")
public class StatusServiceTest {

  private final AppConfigurationProperties appConfigurationProperties = new AppConfigurationProperties(null, null, null, null);

  @Test
  public void shouldPrintOutConfiguration() throws JsonProcessingException {

    //given
    ObjectWriter prettyWriter = mock(ObjectWriter.class);
    ObjectMapper objectMapper = mock(ObjectMapper.class);
    given(objectMapper.writerWithDefaultPrettyPrinter()).willReturn(prettyWriter);
    given(prettyWriter.writeValueAsString(any())).willReturn("{\"test\":\"value\"}");
    StatusService statusService = new StatusService(appConfigurationProperties, objectMapper);

    // when
    String outPut = statusService.printPaSCHandlerOaiPmhConfig();
    InOrder verifier = inOrder(prettyWriter);

    //then
    verifier.verify(prettyWriter, calls(1)).writeValueAsString(any());
    then(outPut).startsWith("Config for DDI 2.5: [{\"test\":\"value\"}]");
  }
}
