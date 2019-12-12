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
package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.helpers.FakeHarvester;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static eu.cessda.pasc.oci.repository.StudyIdentifierEncoder.encodeStudyIdentifier;

/**
 * A Pasc Harvester Service implementation
 *
 * @author moses AT doravenetures DOT com
 */
@Service
@Slf4j
public class PascHarvesterDao extends DaoBase implements HarvesterDao {

  private static final String LIST_RECORD_TEMPLATE = "%s/%s/ListRecordHeaders?Repository=%s";
  private static final String GET_RECORD_TEMPLATE = "%s/%s/GetRecord/CMMStudy/%s?Repository=%s";

  private final FakeHarvester fakeHarvester;
  private final AppConfigurationProperties appConfigurationProperties;

  @Autowired
  public PascHarvesterDao(FakeHarvester fakeHarvester,
                          AppConfigurationProperties appConfigurationProperties,
                          RestTemplate restTemplate) {
    super(restTemplate);
    this.fakeHarvester = fakeHarvester;
    this.appConfigurationProperties = appConfigurationProperties;
  }

  @Override
  public String listRecordHeaders(String spRepository) throws ExternalSystemException {

    String finalUrl = constructListRecordUrl(spRepository);
    return postForStringResponse(finalUrl);
  }

  @Override
  public String getRecord(String spRepository, String studyNumber) throws ExternalSystemException {
    String finalUrl = constructGetRecordUrl(spRepository, studyNumber);
    return postForStringResponse(finalUrl);
  }

  private String constructListRecordUrl(String repositoryUrl) {
    //pascOciConfig.getHarvesterUrl(),  Fixme: reeanable and remove the fake following urls

    String finalUrl = "replace_me";
    Optional<Repo> repoOptional = fakeHarvester.getRepoConfigurationProperties(repositoryUrl);

    if (repoOptional.isPresent()) {
      finalUrl = String.format(LIST_RECORD_TEMPLATE,
          repoOptional.get().getHandler(),
          appConfigurationProperties.getHarvester().getVersion(),
          repositoryUrl);
    }

    log.info("[{}] Final ListHeaders Handler url [{}]", repositoryUrl, finalUrl);
    return finalUrl;
  }

  private String constructGetRecordUrl(String repositoryUrl, String studyNumber) {
    //pascOciConfig.getHarvesterUrl(),  Fixme: reeanable and remove the fake following urls

    String finalUrl = "replace_me";
    Optional<Repo> repoOptional = fakeHarvester.getRepoConfigurationProperties(repositoryUrl);
    String encodedStudyID = encodeStudyIdentifier().apply(studyNumber);

    if (repoOptional.isPresent()) {
      finalUrl = String.format(GET_RECORD_TEMPLATE,
          repoOptional.get().getHandler(),
          appConfigurationProperties.getHarvester().getVersion(),
          encodedStudyID,
          repositoryUrl);
    }
    log.trace("[{}] Final GetRecord Handler url [{}]", repositoryUrl, finalUrl);
    return finalUrl;
  }
}
