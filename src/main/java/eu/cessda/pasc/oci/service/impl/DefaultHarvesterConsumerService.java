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

/**
 * Default OSMH Consumer Service implementation
 *
 * @author moses@doraventures.com
 */
@Service
@Slf4j
public class DefaultHarvesterConsumerService implements HarvesterConsumerService {

  private HarvesterDao harvesterDao;
  private ObjectMapper mapper;

  @Autowired
  public DefaultHarvesterConsumerService(HarvesterDao harvesterDao, ObjectMapper mapper) {
    this.harvesterDao = harvesterDao;
    this.mapper = mapper;
  }

  @Override
  public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
    List<RecordHeader> recordHeadersUnfiltered = new ArrayList<>();

    try {
      String recordHeadersJsonString = harvesterDao.listRecordHeaders(repo.getUrl());
      CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
      recordHeadersUnfiltered = mapper.readValue(recordHeadersJsonString, collectionType);
    } catch (ExternalSystemException e) {
      log.error("ExternalSystemException! ListRecordHeaders failed for repo [{}]. Error[{}]", repo, e.getMessage(), e);
    } catch (IOException e) {
      log.error("Error, Unable to pass ListRecordHeaders response error[{}]", e.getMessage(), e);
    }

    return filterRecords(recordHeadersUnfiltered, lastModifiedDate);
  }

  @Override
  public Optional<CMMStudy> getRecord(Repo repo, String studyNumber) {

    try {
      String recordHeadersJsonString = harvesterDao.getRecord(repo.getUrl(), studyNumber);
      return Optional.ofNullable(CMMStudyConverter.fromJsonString(recordHeadersJsonString));
    } catch (ExternalSystemException e) {
      log.warn("Exception msg[{}]. External system response body[{}]", e.getMessage(), e.getExternalResponseBody());
    } catch (IOException e) {
      log.error("Error, Unable to pass GetRecord response error[{}]", e.getMessage(), e);
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
      String formatMsg = "Returning [{}] filtered recordHeaders  out of [{}] unfiltered by date greater than [{}]";
      log.info(formatMsg, filteredHeaders.size(), unfilteredRecordHeaders.size(), ingestedLastModifiedDate);
      return filteredHeaders;
    }

    log.info("Nothing filterable by date [{}]", ingestedLastModifiedDate);
    return unfilteredRecordHeaders;
  }

  private Predicate<RecordHeader> isHeaderTimeGreater(LocalDateTime lastModifiedDate) {
    return recordHeader -> {
      String lastModified = recordHeader.getLastModified();
      Optional<LocalDateTime> currentHeaderLastModified = TimeUtility.getLocalDateTime(lastModified);
      return currentHeaderLastModified
          .map(localDateTime -> localDateTime.isAfter(lastModifiedDate))
          .orElse(false); // Could not parse lastDateModified therefore filtering out record header
    };
  }
}
