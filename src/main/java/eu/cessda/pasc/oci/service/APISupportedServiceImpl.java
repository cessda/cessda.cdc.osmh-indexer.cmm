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

package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configuration.HandlerConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author moses AT doraventures DOT com
 */
@Service
public class APISupportedServiceImpl implements APISupportedService {

  private final HandlerConfigurationProperties pmhConfig;

  @Autowired
  public APISupportedServiceImpl(@Qualifier("HandlerConfigurationProperties") HandlerConfigurationProperties pmhConfig) {
    this.pmhConfig = pmhConfig;
  }

  @Override
  public List<String> getSupportedVersion() {
    return pmhConfig.getOaiPmh().getSupportedApiVersions();
  }

  @Override
  public boolean isSupportedVersion(String version) {
    return getSupportedVersion().contains(version);
  }

  @Override
  public List<String> getSupportedRecordTypes() {
    return pmhConfig.getOaiPmh().getSupportedRecordTypes();
  }
}
