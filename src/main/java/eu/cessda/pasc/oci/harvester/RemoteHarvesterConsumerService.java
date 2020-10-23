/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.harvester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import eu.cessda.pasc.oci.LoggingConstants;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.exception.HTTPException;
import eu.cessda.pasc.oci.exception.HandlerNotFoundException;
import eu.cessda.pasc.oci.http.HttpClient;
import eu.cessda.pasc.oci.models.ErrorMessage;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.configurations.Harvester;
import eu.cessda.pasc.oci.models.configurations.Repo;
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

import static net.logstash.logback.argument.StructuredArguments.value;

/**
 * Default OSMH Consumer Service implementation
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class RemoteHarvesterConsumerService extends AbstractHarvesterConsumerService {

    private final HttpClient httpClient;
    private final ObjectReader recordHeaderObjectReader;
    private final CMMStudyConverter cmmStudyConverter;
    private final AppConfigurationProperties appConfigurationProperties;
    private final ObjectReader errorMessageObjectReader;

    @Autowired
    public RemoteHarvesterConsumerService(AppConfigurationProperties appConfigurationProperties, CMMStudyConverter cmmStudyConverter, HttpClient httpClient, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.cmmStudyConverter = cmmStudyConverter;
        this.appConfigurationProperties = appConfigurationProperties;
        this.recordHeaderObjectReader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class));
        this.errorMessageObjectReader = mapper.readerFor(ErrorMessage.class);
    }

    @Override
    public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
        try {
            URI finalUrl = constructListRecordUrl(repo);
            try (InputStream recordHeadersJsonStream = httpClient.getInputStream(finalUrl)) {
                List<RecordHeader> recordHeadersUnfiltered = recordHeaderObjectReader.readValue(recordHeadersJsonStream);
                log.info("[{}] Retrieved [{}] record headers.", repo.getCode(), recordHeadersUnfiltered.size());
                return filterRecords(recordHeadersUnfiltered, lastModifiedDate);
            }
        }
        catch (HTTPException e) {
            try {
                ErrorMessage errorMessage = errorMessageObjectReader.readValue(e.getExternalResponse().getBody());
                log.error(LIST_RECORD_HEADERS_FAILED_WITH_MESSAGE,
                    value(LoggingConstants.REPO_NAME, repo.getCode()),
                    value(LoggingConstants.EXCEPTION_NAME, errorMessage.getException()),
                    value(LoggingConstants.REASON, errorMessage.getMessage())
                );
            } catch (IOException jsonException) {
                log.error(LIST_RECORD_HEADERS_FAILED + ": Response body: {}",
                    value(LoggingConstants.REPO_NAME, repo.getCode()),
                    e.toString(),
                    value(LoggingConstants.REASON, e.getExternalResponse().getBodyAsString())
                );
                log.debug(jsonException.toString());
            }
        } catch (IOException | IllegalArgumentException | URISyntaxException e) {
            log.error(LIST_RECORD_HEADERS_FAILED, value(LoggingConstants.REPO_NAME, repo.getCode()), e.toString());
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<CMMStudy> getRecordFromRemote(Repo repo, RecordHeader recordHeader) {

        log.debug("[{}] Querying repository handler {} for studyNumber {}.", repo.getHandler(), repo.getCode(), recordHeader.getIdentifier());

        try {
            URI finalUrl = constructGetRecordUrl(repo, recordHeader.getIdentifier());
            try (InputStream recordJsonStream = httpClient.getInputStream(finalUrl)) {
                return Optional.of(cmmStudyConverter.fromJsonStream(recordJsonStream));
            }
        } catch (HTTPException e) {
            try {
                ErrorMessage errorMessage = errorMessageObjectReader.readValue(e.getExternalResponse().getBody());
                log.warn(FAILED_TO_GET_STUDY_ID_WITH_MESSAGE,
                    value(LoggingConstants.REPO_NAME, repo.getCode()),
                    value(LoggingConstants.STUDY_ID, recordHeader.getIdentifier()),
                    value(LoggingConstants.EXCEPTION_NAME, errorMessage.getException()),
                    value(LoggingConstants.REASON, errorMessage.getMessage())
                );
            } catch (IOException jsonException) {
                log.warn(FAILED_TO_GET_STUDY_ID + ": Response body: {}",
                    value(LoggingConstants.REPO_NAME, repo.getCode()),
                    value(LoggingConstants.STUDY_ID, recordHeader.getIdentifier()),
                    e.toString(),
                    value(LoggingConstants.REASON, e.getExternalResponse().getBodyAsString())
                );
                log.debug(jsonException.toString());
            }
        } catch (IOException | IllegalArgumentException | URISyntaxException e) {
            log.error(FAILED_TO_GET_STUDY_ID,
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value(LoggingConstants.STUDY_ID, recordHeader.getIdentifier()),
                e.toString()
            );
        }
        return Optional.empty();
    }

    private URI constructListRecordUrl(Repo repo) throws URISyntaxException {
        var harvester = appConfigurationProperties.getEndpoints().getHarvesters().get(repo.getHandler().toUpperCase());
        if (harvester != null) {
            String finalUrlString = String.format("%s/%s/ListRecordHeaders?Repository=%s",
                harvester.getUrl(),
                harvester.getVersion(),
                URLEncoder.encode(repo.getUrl().toString(), StandardCharsets.UTF_8)
            );
            var finalUrl = new URI(finalUrlString);
            log.trace("[{}] Final ListHeaders Handler url [{}] constructed.", repo.getName(), finalUrlString);
            return finalUrl;
        } else {
            throw new HandlerNotFoundException(repo);
        }
    }

    private URI constructGetRecordUrl(Repo repo, String studyNumber) throws URISyntaxException {
        Harvester harvester = appConfigurationProperties.getEndpoints().getHarvesters().get(repo.getHandler().toUpperCase());
        if (harvester != null) {
            String encodedStudyID = StudyIdentifierEncoder.encodeStudyIdentifier(studyNumber);
            String finalUrlString = String.format("%s/%s/GetRecord/CMMStudy/%s?Repository=%s",
                harvester.getUrl(),
                harvester.getVersion(),
                encodedStudyID,
                URLEncoder.encode(repo.getUrl().toString(), StandardCharsets.UTF_8)
            );
            if (repo.getDefaultLanguage() != null) {
                finalUrlString += "&defaultLanguage=" + repo.getDefaultLanguage();
            }
            URI finalUrl = new URI(finalUrlString);
            log.trace("[{}] Final GetRecord Handler url [{}] constructed.", repo.getUrl(), finalUrl);
            return finalUrl;
        } else {
            throw new HandlerNotFoundException(repo);
        }
    }
}
