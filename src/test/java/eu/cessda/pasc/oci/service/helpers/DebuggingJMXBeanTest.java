/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import org.assertj.core.api.Java6BDDAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Java6BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DebuggingJMXBeanTest extends EmbeddedElasticsearchServer {

  // Class under test
  private DebuggingJMXBean debuggingJMXBean;
  @Autowired
  private AppConfigurationProperties appConfigurationProperties;

  @Before
  public void init() {
    startup(ELASTICSEARCH_HOME);
    ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(getClient());
    debuggingJMXBean = new DebuggingJMXBean(elasticsearchTemplate, appConfigurationProperties);
  }

  @After
  public void shutdown() {
    closeNodeResources();
    Java6BDDAssertions.then(this.node.isClosed()).isTrue();
  }

  @Test
  public void shouldPrintElasticsearchDetails() {
    then(debuggingJMXBean.printElasticSearchInfo()).isEqualTo("Printed Health");
  }

  @Test
  public void shouldPrintCurrentlyConfiguredRepoEndpoints() {
    // When
    String actualRepos = debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints();
    then(actualRepos).isNotEmpty();
  }
}
