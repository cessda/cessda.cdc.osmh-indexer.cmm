/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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

  private final AppConfigurationProperties appConfigurationProperties;
  private final ObjectWriter prettyPrinter;

  @Autowired
  public StatusService(AppConfigurationProperties appConfigurationProperties, ObjectMapper objectMapper) {
    this.appConfigurationProperties = appConfigurationProperties;
    this.prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();
  }

  @ManagedOperation(description = "Prints out the CDC Handler OAI-PMH DDI 2.5 Configuration")
  public String printPaSCHandlerOaiPmhConfig() throws JsonProcessingException {
    return "Config for DDI 2.5: [" + prettyPrinter.writeValueAsString(appConfigurationProperties.getOaiPmh()) + "]";
  }
}
