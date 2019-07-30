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
package eu.cessda.pasc.osmhhandler.oaipmh.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class APISupportedServiceImplTest {

  @Autowired
  APISupportedServiceImpl apiSupportedServiceImpl;

  @Test
  public void shouldReturnTheListOfSupportedServices() {

    List<String> supportedApiVersions = apiSupportedServiceImpl.getSupportedVersion();

    then(supportedApiVersions).contains("v0");
    then(supportedApiVersions).hasSize(1);
  }

  @Test
  public void shouldReturnTrueForSupportedAPIVersion() {
    boolean isSupportedVersion = apiSupportedServiceImpl.isSupportedVersion("v0");
    then(isSupportedVersion).isTrue();
  }

  @Test
  public void shouldReturnFalseForUnSupportedAPIVersion() {
    boolean isSupportedVersion = apiSupportedServiceImpl.isSupportedVersion("v2");
    then(isSupportedVersion).isFalse();
  }

  @Test
  public void shouldReturnTheListOfSupportedRecordTypes() {
    List<String> supportedRecordTypes = apiSupportedServiceImpl.getSupportedRecordTypes();

    then(supportedRecordTypes).contains("StudyGroup", "Study", "Variable", "Question", "CMM");
    then(supportedRecordTypes).hasSize(5);
  }
}