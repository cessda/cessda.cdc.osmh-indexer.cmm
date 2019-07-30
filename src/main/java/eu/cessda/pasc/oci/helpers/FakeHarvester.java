/**
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
package eu.cessda.pasc.oci.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * This is a temp Solution whilst changes are being made to the Harvester (Node JS app)
 * <p>
 * Acts like as a light weight OSMH Harvester:
 * <ul>
 * <li>Takes takes in a string url representing a sp repo and returns the actual handler url for that repo</li>
 * <li> Does not do anything fancy</li>
 * <li> </li>
 * </ul>
 * <p>
 * FIXME: Remove this fake and call the Harvester directly with the repo url
 *
 * @author moses@doraventures.com
 */
@Component
public class FakeHarvester {

  private AppConfigurationProperties appConfigurationProperties;

  @Autowired
  public FakeHarvester(AppConfigurationProperties appConfigurationProperties) {
    this.appConfigurationProperties = appConfigurationProperties;
  }

  public Optional<Repo> getRepoConfigurationProperties(String repositoryUrl) {
    return appConfigurationProperties.getEndpoints().getRepos()
        .stream()
        .filter(repo -> repo.getUrl().equalsIgnoreCase(repositoryUrl))
        .findFirst();
  }
}
