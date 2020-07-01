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
import eu.cessda.pasc.oci.exception.HandlerNotFoundException;
import eu.cessda.pasc.oci.helpers.LoggingConstants;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.value;

/**
 * Default OSMH Consumer Service implementation
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class RemoteHarvesterConsumerService extends AbstractHarvesterConsumerService {

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
            log.error("[{}] ListRecordHeaders failed: {}", value(LoggingConstants.REPO_NAME, repo.getName()), e.toString());
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<CMMStudy> getRecord(Repo repo, String studyNumber) {

        log.debug("Querying repo {} for studyNumber {}.", repo.getName(), studyNumber);

        try {
            URI finalUrl = constructGetRecordUrl(repo, studyNumber);
            try (InputStream recordJsonStream = daoBase.getInputStream(finalUrl)) {
                return Optional.of(cmmStudyConverter.fromJsonString(recordJsonStream));
            }
        } catch (ExternalSystemException e) {
            log.warn("[{}] Failed to get StudyId [{}]: {}: Response body [{}].",
                    value(LoggingConstants.REPO_NAME, repo.getName()),
                    value(LoggingConstants.STUDY_ID, studyNumber),
                    e.toString(),
                    value(LoggingConstants.REASON, e.getExternalResponse().getBody())
            );
        } catch (IOException e) {
            log.error("[{}] Failed to get StudyId [{}]: {}",
                    value(LoggingConstants.REPO_NAME, repo.getName()),
                    value(LoggingConstants.STUDY_ID, studyNumber),
                    e.toString()
            );
        }
        return Optional.empty();
    }

    public URI constructListRecordUrl(Repo repo) {
        Harvester harvester = appConfigurationProperties.getEndpoints().getHarvesters().get(repo.getHandler().toUpperCase());
        if (harvester != null) {
            UriComponentsBuilder finalUrlBuilder = UriComponentsBuilder.fromUri(harvester.getUrl())
                    .path(harvester.getVersion())
                    .path("/ListRecordHeaders")
                    .queryParam("Repository", URLEncoder.encode(repo.getUrl().toString(), StandardCharsets.UTF_8));
            URI finalUrl = finalUrlBuilder.build(true).toUri();
            log.trace("[{}] Final ListHeaders Handler url [{}] constructed.", repo.getName(), finalUrl);
            return finalUrl;
        } else {
            throw new HandlerNotFoundException(repo);
        }
    }

    public URI constructGetRecordUrl(Repo repo, String studyNumber) {
        Harvester harvester = appConfigurationProperties.getEndpoints().getHarvesters().get(repo.getHandler().toUpperCase());
        if (harvester != null) {
            String encodedStudyID = StudyIdentifierEncoder.encodeStudyIdentifier().apply(studyNumber);
            UriComponentsBuilder finalUrlBuilder = UriComponentsBuilder.fromUri(harvester.getUrl())
                    .path(harvester.getVersion())
                    .path("/GetRecord/CMMStudy/")
                    .path(encodedStudyID)
                    .queryParam("Repository", URLEncoder.encode(repo.getUrl().toString(), StandardCharsets.UTF_8));
            if (repo.getDefaultLanguage() != null) {
                finalUrlBuilder.queryParam("defaultLanguage", repo.getDefaultLanguage());
            }
            URI finalUrl = finalUrlBuilder.build(true).toUri();
            log.trace("[{}] Final GetRecord Handler url [{}] constructed.", repo.getUrl(), finalUrl);
            return finalUrl;
        } else {
            throw new HandlerNotFoundException(repo);
        }
    }
}
