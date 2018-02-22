package eu.cessda.pasc.oci.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.cessda.pasc.oci.dao.HarvesterDao;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default OSMH Consumer Service implementation
 *
 * @author moses@doraventures.com
 */
@Service
@Slf4j
public class DefaultConsumerService implements ConsumerService {

  private HarvesterDao harvesterDao;
  private ObjectMapper mapper;

  @Autowired
  public DefaultConsumerService(HarvesterDao harvesterDao, ObjectMapper mapper) {
    this.harvesterDao = harvesterDao;
    this.mapper = mapper;
  }

  @Override
  public List<RecordHeader> listRecorderHeadersBody(Repo repo) {
    List<RecordHeader> recordHeaders = new ArrayList<>();

    try {
      String recordHeadersJsonString = harvesterDao.listRecordHeaders(repo.getUrl());
      CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, RecordHeader.class);
      recordHeaders = mapper.readValue(recordHeadersJsonString, collectionType);
    } catch (IOException e) {
      log.error("Error, Unable to pass ListRecordHeaders response error[{}]", e.getMessage(), e);
    } catch (ExternalSystemException e) {
      log.error("ExternalSystemException, Unable to List RecordHeaders response error[{}]", e.getMessage(), e);
    }

    return recordHeaders;
  }
}
