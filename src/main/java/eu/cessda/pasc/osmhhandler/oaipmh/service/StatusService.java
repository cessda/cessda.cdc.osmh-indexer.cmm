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

package eu.cessda.pasc.osmhhandler.oaipmh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

/**
 * Diagnostics Managed Resource Service class.
 *
 * @author moses AT doraventures DOT com
 */
@Service
@ManagedResource
@Slf4j
public class StatusService {

  private final HandlerConfigurationProperties handlerConfigurationProperties;
  private final ObjectMapper objectMapper;

  @Autowired
  public StatusService(HandlerConfigurationProperties handlerConfigurationProperties, ObjectMapper objectMapper) {
    this.handlerConfigurationProperties = handlerConfigurationProperties;
    this.objectMapper = objectMapper;
  }

  @ManagedOperation(description = "Prints out the PaSC Handler Oai-Pmh DDI 2.5 Configurations.")
  public String printPaSCHandlerOaiPmhConfig() throws JsonProcessingException {
    ObjectWriter prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();
    return "Config for Rest: [" + prettyPrinter.writeValueAsString(handlerConfigurationProperties.getRestTemplateProps()) + "]\n" +
            "Config for DDI 2.5: [" + prettyPrinter.writeValueAsString(handlerConfigurationProperties.getOaiPmh()) + "]";
  }
}
