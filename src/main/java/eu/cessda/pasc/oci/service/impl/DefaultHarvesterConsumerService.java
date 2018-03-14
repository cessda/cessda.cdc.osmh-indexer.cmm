package eu.cessda.pasc.oci.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
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
    List<RecordHeader> recordHeaders = new ArrayList<>();

    try {
      String recordHeadersJsonString = harvesterDao.listRecordHeaders(repo.getUrl());
      CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
      recordHeaders = mapper.readValue(recordHeadersJsonString, collectionType);
    } catch (ExternalSystemException e) {
      log.error("ExternalSystemException! ListRecordHeaders failed for repo [{}]. Error[{}]", repo, e.getMessage(), e);
    } catch (IOException e) {
      log.error("Error, Unable to pass ListRecordHeaders response error[{}]", e.getMessage(), e);
    }

    return recordHeaders;
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
}
