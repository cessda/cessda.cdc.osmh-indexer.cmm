/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.configurations;

import eu.cessda.pasc.oci.models.oai.configuration.OaiPmh;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Loads Configurations from application*.yml
 *
 * @author moses AT doraventures DOT com
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "osmhhandler")
@Qualifier("HandlerConfigurationProperties")
@Getter
public class HandlerConfigurationProperties {

  private final OaiPmh oaiPmh = new OaiPmh();

  public HandlerConfigurationProperties() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }
}
