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
package eu.cessda.pasc.oci.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.configurations.Harvester;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.repository.DaoBase;
import eu.cessda.pasc.oci.service.helpers.StudyIdentifierEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static net.logstash.logback.argument.StructuredArguments.value;

/**
 * Default OSMH Consumer Service implementation
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class RemoteHarvesterConsumerService extends AbstractHarvesterConsumerService {

    private static final String LIST_RECORD_TEMPLATE = "%s/%s/ListRecordHeaders?Repository=%s";
    private static final String GET_RECORD_TEMPLATE = "%s/%s/GetRecord/CMMStudy/%s?Repository=%s";

    private final DaoBase daoBase;
    private final ObjectReader recordHeaderObjectReader;
    private final CMMStudyConverter cmmStudyConverter;
    private final AppConfigurationProperties appConfigurationProperties;

    @Autowired
    public RemoteHarvesterConsumerService(AppConfigurationProperties appConfigurationProperties, CMMStudyConverter cmmStudyConverter, DaoBase daoBase, ObjectMapper mapper) {
        this.daoBase = daoBase;
        this.cmmStudyConverter = cmmStudyConverter;
        this.appConfigurationProperties = appConfigurationProperties;
        this.recordHeaderObjectReader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class));
    }

    @Override
  public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
    try {
        URI finalUrl = constructListRecordUrl(repo);
        try (InputStream recordHeadersJsonStream = daoBase.getInputStream(finalUrl)) {
            List<RecordHeader> recordHeadersUnfiltered = recordHeaderObjectReader.readValue(recordHeadersJsonStream);
            return filterRecords(recordHeadersUnfiltered, lastModifiedDate);
        }
    } catch (IOException e) {
      log.error("ListRecordHeaders failed for repo [{}] [{}]. {}.",
              value(REPO_NAME, repo.getName()), value(REPO_ENDPOINT_URL, repo.getUrl()), e.toString());
    } catch (URISyntaxException e) {
      log.error("Unable to construct URL [{}]", e.toString());
    }
    return Collections.emptyList();
  }

  @Override
  public Optional<CMMStudy> getRecord(Repo repo, String studyNumber) {

    log.debug("Querying repo {} for studyNumber {}.", repo.getName(), studyNumber);

    URI finalUrl = null;
    try {
        finalUrl = constructGetRecordUrl(repo, studyNumber);
        try (InputStream recordJsonStream = daoBase.getInputStream(finalUrl)) {
            return Optional.of(cmmStudyConverter.fromJsonString(recordJsonStream));
        }
    } catch (ExternalSystemException e) {
      log.warn("{}. Response detail from handler [{}], URL to handler for harvesting record [{}] with [{}] from repo [{}] [{}].",
              e.toString(),
              value(REASON, e.getExternalResponse().getExternalResponseBody()),
              finalUrl,
              keyValue("study_id", studyNumber),
              value(REPO_NAME, repo.getName()),
              value(REPO_ENDPOINT_URL, repo.getUrl())
      );
    } catch (IOException e) {
        log.error("Unable to parse GetRecord response: {}.", e.toString());
    } catch (URISyntaxException e) {
        log.error("Unable to construct URL: {}.", e.toString());
    }
      return Optional.empty();
  }

    public URI constructListRecordUrl(Repo repo) throws URISyntaxException {
        Harvester harvester = appConfigurationProperties.getEndpoints().getHarvesters().get(repo.getHandler());
        String finalUrlString = String.format(LIST_RECORD_TEMPLATE,
                harvester.getUrl(),
                harvester.getVersion(),
                URLEncoder.encode(repo.getUrl().toString(), StandardCharsets.UTF_8)
        );
        URI finalUrl = new URI(finalUrlString);
        log.trace("[{}] Final ListHeaders Handler url [{}] constructed.", repo.getName(), finalUrlString);
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
