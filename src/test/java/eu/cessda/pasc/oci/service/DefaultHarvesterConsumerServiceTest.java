package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.repository.HarvesterDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static eu.cessda.pasc.oci.data.RecordTestData.LIST_RECORDER_HEADERS_BODY_EXAMPLE;
import static eu.cessda.pasc.oci.data.ReposTestData.getUKDSRepo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.when;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
public class DefaultHarvesterConsumerServiceTest extends AbstractSpringTestProfileContext {

  @Mock
  private HarvesterDao harvesterDao;

  @InjectMocks
  @Autowired
//  HarvesterConsumerService consumerService;
      DefaultHarvesterConsumerService consumerService;

  @Test
  public void shouldReturnASuccessfulResponseForListingRecordHeaders() throws ExternalSystemException {

    when(harvesterDao.listRecordHeaders(anyString())).thenReturn(LIST_RECORDER_HEADERS_BODY_EXAMPLE);
    Repo repo = getUKDSRepo();

    List<RecordHeader> recordHeaders = consumerService.listRecordHeaders(repo);
    assertThat(recordHeaders).hasSize(2);
    recordHeaders.forEach(System.out::println);
  }

  @Test
  public void shouldReturnASuccessfulResponseGetRecord() throws ExternalSystemException {
    FileHandler fileHandler = new FileHandler();
    String recordUkds998 = fileHandler.getFileWithUtil("record_ukds_998.json");
    String recordID = "998";

    when(harvesterDao.getRecord(anyString(), anyString())).thenReturn(recordUkds998);
    Repo repo = getUKDSRepo();

    Optional<CMMStudy> cmmStudy = consumerService.getRecord(repo, recordID);

    assertThat(cmmStudy.isPresent()).isTrue();
    then(cmmStudy.get().getStudyNumber()).isEqualTo("998");
    then(cmmStudy.get().getLastModified()).isEqualTo("2018-02-22T07:48:38Z");
    then(cmmStudy.get().getKeywords()).hasSize(1);
    then(cmmStudy.get().getKeywords().get("en")).hasSize(62);
  }

  @Test
  public void shouldReturnDeletedRecordMarkedAsInactive() throws ExternalSystemException {
    FileHandler fileHandler = new FileHandler();
    String recordUkds1031 = fileHandler.getFileWithUtil("record_ukds_1031_deleted.json");
    String recordID = "1031";

    when(harvesterDao.getRecord(anyString(), matches(recordID))).thenReturn(recordUkds1031);
    Repo repo = getUKDSRepo();

    Optional<CMMStudy> cmmStudy = consumerService.getRecord(repo, recordID);

    assertThat(cmmStudy.isPresent()).isTrue();
    then(cmmStudy.get().getStudyNumber()).isEqualTo("1031");
    then(cmmStudy.get().getLastModified()).isEqualTo("2017-05-02T08:31:32Z");
    then(cmmStudy.get().isActive()).isFalse();
    then(cmmStudy.get().getDataCollectionPeriodEnddate()).isNull();
    then(cmmStudy.get().getAbstractField()).isNull();
    then(cmmStudy.get().getTitleStudy()).isNull();
    then(cmmStudy.get().getPublisher()).isNull();
    then(cmmStudy.get().getKeywords()).isNull();
    then(cmmStudy.get().getCreators()).isNull();
  }
}