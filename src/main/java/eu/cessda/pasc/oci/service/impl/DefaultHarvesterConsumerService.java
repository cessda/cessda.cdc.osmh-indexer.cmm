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
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.cessda.pasc.oci.helpers.TimeUtility;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.repository.HarvesterDao;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import eu.cessda.pasc.oci.service.RepositoryUrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static eu.cessda.pasc.oci.service.helpers.StudyIdentifierEncoder.decodeStudyNumber;

/**
 * Default OSMH Consumer Service implementation
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class DefaultHarvesterConsumerService implements HarvesterConsumerService {

  private final HarvesterDao harvesterDao;
  private final RepositoryUrlService repositoryUrlService;
  private final ObjectMapper mapper;

  @Autowired
  public DefaultHarvesterConsumerService(HarvesterDao harvesterDao, RepositoryUrlService repositoryUrlService,
                                         ObjectMapper mapper) {
    this.harvesterDao = harvesterDao;
    this.repositoryUrlService = repositoryUrlService;
    this.mapper = mapper;
  }

  @Override
  public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
    List<RecordHeader> recordHeadersUnfiltered = new ArrayList<>();

    try {
      String finalUrl = repositoryUrlService.constructListRecordUrl(repo.getUrl());
      String recordHeadersJsonString = harvesterDao.listRecordHeaders(finalUrl);
      CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
      recordHeadersUnfiltered = mapper.readValue(recordHeadersJsonString, collectionType);
    } catch (ExternalSystemException e) {
      log.error("ListRecordHeaders failed for repo [{}]. CDC Handler Error object Msg [{}].", repo, e.getMessage(), e);
    } catch (IOException e) {
      log.error("Error, Unable to pass ListRecordHeaders response error message [{}].", e.getMessage(), e);
    }

    return filterRecords(recordHeadersUnfiltered, lastModifiedDate);
  }

  @Override
  public Optional<CMMStudy> getRecord(Repo repo, String studyNumber) {

    String finalUrl = "";
    try {
      finalUrl = repositoryUrlService.constructGetRecordUrl(repo.getUrl(), studyNumber);
      String recordJsonString = harvesterDao.getRecord(finalUrl);
      return Optional.ofNullable(CMMStudyConverter.fromJsonString(recordJsonString));
    } catch (ExternalSystemException e) {
      log.warn("Short Exception msg [{}], " +
                      "HttpServerErrorException response detail from handler's [{}], " +
                      "URL to handler for harvesting [{}].",
              e.getMessage(),
              e.getExternalResponseBody(),
              decodeStudyNumber().apply(finalUrl));
    } catch (IOException e) {
      log.error("Error, Unable to pass GetRecord response error message [{}].", e.getMessage(), e);
    }

    return Optional.empty();
  }

  private List<RecordHeader> filterRecords(List<RecordHeader> unfilteredRecordHeaders, LocalDateTime ingestedLastModifiedDate) {

    Optional<LocalDateTime> ingestedLastModifiedDateOption = Optional.ofNullable(ingestedLastModifiedDate);
    Optional<List<RecordHeader>> filteredRecordHeaders = ingestedLastModifiedDateOption.map(
        lastModifiedDate -> unfilteredRecordHeaders
            .stream()
            .filter(isHeaderTimeGreater(lastModifiedDate))
            .collect(Collectors.toList()));

    if (filteredRecordHeaders.isPresent()) {
      List<RecordHeader> filteredHeaders = filteredRecordHeaders.get();
      log.info("Returning [{}] filtered recordHeaders by date greater than [{}] | out of [{}] unfiltered.",
          filteredHeaders.size(),
          ingestedLastModifiedDate,
          unfilteredRecordHeaders.size());

      return filteredHeaders;
    }

    log.info("Nothing filterable by date [{}].", ingestedLastModifiedDate);
    return unfilteredRecordHeaders;
  }

  private Predicate<RecordHeader> isHeaderTimeGreater(LocalDateTime lastModifiedDate) {
    return recordHeader -> {
      String lastModified = recordHeader.getLastModified();
      Optional<LocalDateTime> currentHeaderLastModified = TimeUtility.getLocalDateTime(lastModified);
      return currentHeaderLastModified
          .map(localDateTime -> localDateTime.isAfter(lastModifiedDate))
          .orElseGet(() -> {
                log.warn("Could not parse RecordIdentifier lastModifiedDate [{}]. Filtering out from list.", lastModified);
                return false;
              }
          );
    };
  }
}
