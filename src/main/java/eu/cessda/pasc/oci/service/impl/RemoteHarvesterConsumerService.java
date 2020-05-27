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
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.repository.HarvesterDao;
import eu.cessda.pasc.oci.service.RepositoryUrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

  private final HarvesterDao harvesterDao;
  private final RepositoryUrlService repositoryUrlService;
  private final ObjectReader recordHeaderObjectReader;
  private final CMMStudyConverter cmmStudyConverter;

  @Autowired
  public RemoteHarvesterConsumerService(HarvesterDao harvesterDao, RepositoryUrlService repositoryUrlService,
                                        ObjectMapper mapper, CMMStudyConverter cmmStudyConverter) {
    this.harvesterDao = harvesterDao;
    this.repositoryUrlService = repositoryUrlService;
    this.cmmStudyConverter = cmmStudyConverter;

    CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
    this.recordHeaderObjectReader = mapper.readerFor(collectionType);
  }

  @Override
  public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
    try {
      URI finalUrl = repositoryUrlService.constructListRecordUrl(repo);
      try (InputStream recordHeadersJsonStream = harvesterDao.listRecordHeaders(finalUrl)) {
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
      finalUrl = repositoryUrlService.constructGetRecordUrl(repo, studyNumber);
      try (InputStream recordJsonStream = harvesterDao.getRecord(finalUrl)) {
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
}
