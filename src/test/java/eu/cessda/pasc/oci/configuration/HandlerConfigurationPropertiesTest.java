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

package eu.cessda.pasc.oci.configuration;

import eu.cessda.pasc.oci.configurations.HandlerConfigurationProperties;
import eu.cessda.pasc.oci.models.oai.configuration.OaiPmh;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Configurations loader tests
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles("test")
public class HandlerConfigurationPropertiesTest {

  @Autowired
  HandlerConfigurationProperties handlerConfigurationProperties;

  @Test
  public void shouldReturnConfigurationsForOSMHHandler() {

    OaiPmh oaiPmh = handlerConfigurationProperties.getOaiPmh();

    then(oaiPmh).isNotNull();

    then(oaiPmh.getSupportedApiVersions()).hasSize(1);
    then(oaiPmh.getSupportedApiVersions()).contains("v0");
    then(oaiPmh.getSupportedRecordTypes()).hasSize(5);

    then(oaiPmh.getRepos()).isNotNull();
    then(oaiPmh.getRepos()).isNotEmpty();
    then(oaiPmh.getRepos()).hasSize(3);
    then(oaiPmh.getRepos().get(0).getUrl()).isEqualTo("https://data2.aussda.at/oai/");
    then(oaiPmh.getRepos().get(0).getPreferredMetadataParam()).isEqualTo("oai_ddi");
    then(oaiPmh.getRepos().get(1).getUrl()).isEqualTo("http://services.fsd.uta.fi/v0/oai");
    then(oaiPmh.getRepos().get(1).getPreferredMetadataParam()).isEqualTo("oai_ddi25");
    then(oaiPmh.getRepos().get(1).getSetSpec()).isEqualTo("study_groups:energia");
    then(oaiPmh.getRepos().get(2).getUrl()).isEqualTo("https://oai.ukdataservice.ac.uk:8443/oai/provider");
    then(oaiPmh.getRepos().get(2).getPreferredMetadataParam()).isEqualTo("ddi");
  }
}
