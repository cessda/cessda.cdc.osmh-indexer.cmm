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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.helpers.FakeHarvester;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.helpers.StudyIdentifierEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class RepositoryUrlService
{

    private static final String LIST_RECORD_TEMPLATE = "%s/%s/ListRecordHeaders?Repository=%s";
    private static final String GET_RECORD_TEMPLATE = "%s/%s/GetRecord/CMMStudy/%s?Repository=%s";

    private final FakeHarvester fakeHarvester;
    private final AppConfigurationProperties appConfigurationProperties;


    @Autowired
    public RepositoryUrlService( FakeHarvester fakeHarvester, AppConfigurationProperties appConfigurationProperties )
    {
        this.fakeHarvester = fakeHarvester;
        this.appConfigurationProperties = appConfigurationProperties;
    }

    public String constructListRecordUrl( String repositoryUrl )
    {
        Optional<Repo> repoOptional = fakeHarvester.getRepoConfigurationProperties(repositoryUrl);
        if (repoOptional.isPresent()) {
            String finalUrl = String.format(LIST_RECORD_TEMPLATE,
                    repoOptional.get().getHandler(),
                    appConfigurationProperties.getHarvester().getVersion(),
                    repositoryUrl);
            log.info("[{}] Final ListHeaders Handler url [{}] constructed.", repositoryUrl, finalUrl);
            return finalUrl;
        } else {
            throw new IllegalStateException("Couldn't construct Final ListHeaders Handler url for repository" + repositoryUrl);
        }
    }

    public String constructGetRecordUrl( String repositoryUrl, String studyNumber ) {
        Optional<Repo> repoOptional = fakeHarvester.getRepoConfigurationProperties(repositoryUrl);
        if (repoOptional.isPresent()) {
            String encodedStudyID = StudyIdentifierEncoder.encodeStudyIdentifier().apply(studyNumber);
            String finalUrl = String.format(GET_RECORD_TEMPLATE,
                    repoOptional.get().getHandler(),
                    appConfigurationProperties.getHarvester().getVersion(),
                    encodedStudyID,
                    repositoryUrl);
            log.trace("[{}] Final GetRecord Handler url [{}] constructed.", repositoryUrl, finalUrl);
            return finalUrl;
        } else {
            throw new IllegalStateException("Couldn't construct Final GetRecord Handler url for repository " + repositoryUrl + " and studyNumber " + studyNumber);
        }
    }
}