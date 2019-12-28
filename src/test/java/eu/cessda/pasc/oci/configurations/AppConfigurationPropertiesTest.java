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
package eu.cessda.pasc.oci.configurations;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Configurations loader tests
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
public class AppConfigurationPropertiesTest extends AbstractSpringTestProfileContext{

  @Autowired
  AppConfigurationProperties appConfigurationProperties;

  @Test
  public void shouldReturnConfigurationsForOSMHHandler() {

    then(appConfigurationProperties.getHarvester().getUrl()).isEqualTo("http://localhost:9091");
    then(appConfigurationProperties.getHarvester().getVersion()).isEqualTo("v0");

    List<Repo> repos = appConfigurationProperties.getEndpoints().getRepos();
    then(repos).isNotNull();
    then(repos).hasAtLeastOneElementOfType(Repo.class);
  }
}
