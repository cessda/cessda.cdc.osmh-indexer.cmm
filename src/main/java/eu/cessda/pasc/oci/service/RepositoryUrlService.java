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
import eu.cessda.pasc.oci.models.configurations.Harvester;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.helpers.StudyIdentifierEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class RepositoryUrlService
{

    private static final String LIST_RECORD_TEMPLATE = "%s/%s/ListRecordHeaders?Repository=%s";
    private static final String GET_RECORD_TEMPLATE = "%s/%s/GetRecord/CMMStudy/%s?Repository=%s";

    private final AppConfigurationProperties appConfigurationProperties;

    @Autowired
    public RepositoryUrlService(AppConfigurationProperties appConfigurationProperties) {
        this.appConfigurationProperties = appConfigurationProperties;
    }

    public URI constructListRecordUrl(Repo repo) throws URISyntaxException {
        Harvester harvester = appConfigurationProperties.getEndpoints().getHarvesters().get(repo.getHandler());
        String finalUrlString = String.format(LIST_RECORD_TEMPLATE,
                harvester.getUrl(),
                harvester.getVersion(),
                URLEncoder.encode(repo.getUrl().toString(), StandardCharsets.UTF_8)
        );
        URI finalUrl = new URI(finalUrlString);
        log.info("[{}] Final ListHeaders Handler url [{}] constructed.", repo.getUrl(), finalUrlString);
        return finalUrl;
    }

    public URI constructGetRecordUrl(Repo repo, String studyNumber) throws URISyntaxException {
        Harvester harvester = appConfigurationProperties.getEndpoints().getHarvesters().get(repo.getHandler());
        String encodedStudyID = StudyIdentifierEncoder.encodeStudyIdentifier().apply(studyNumber);
        String finalUrlString = String.format(GET_RECORD_TEMPLATE,
                harvester.getUrl(),
                harvester.getVersion(),
                encodedStudyID,
                URLEncoder.encode(repo.getUrl().toString(), StandardCharsets.UTF_8)
        );
        URI finalUrl = new URI(finalUrlString);
        log.trace("[{}] Final GetRecord Handler url [{}] constructed.", repo.getUrl(), finalUrl);
        return finalUrl;
    }
}