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

package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DebuggingJMXBeanTest {

  // Class under test
  private DebuggingJMXBean debuggingJMXBean;

  @Autowired
  private AppConfigurationProperties appConfigurationProperties;

  EmbeddedElasticsearchServer embeddedElasticsearchServer;

  @Before
  public void init() {
    embeddedElasticsearchServer = new EmbeddedElasticsearchServer();
    ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(embeddedElasticsearchServer.getClient());
    debuggingJMXBean = new DebuggingJMXBean(elasticsearchTemplate, appConfigurationProperties);
  }

  @After
  public void shutdown() throws IOException {
    embeddedElasticsearchServer.close();
  }

  @Test
  public void shouldPrintElasticsearchDetails() {
    assertThat(debuggingJMXBean.printElasticSearchInfo()).startsWith("ElasticSearch Client Settings Details");
  }

  @Test
  public void shouldPrintCurrentlyConfiguredRepoEndpoints() {
    // When
    String actualRepos = debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints();
    assertThat(actualRepos).isNotEmpty();
  }
}
