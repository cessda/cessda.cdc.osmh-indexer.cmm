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
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.exception.CustomHandlerException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.impl.LocalHarvesterConsumerService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static eu.cessda.pasc.oci.mock.data.ReposTestData.getUKDSRepo;

/**
 * @author moses AT doraventures DOT com
 */
public class LocalHarvesterConsumerServiceTest {


  private static final Repo UKDS_REPO = getUKDSRepo();
  private static final String STUDY_NUMBER = "oai:ukds/5436";
  private final ListRecordHeadersService listRecordHeadersService = Mockito.mock(ListRecordHeadersService.class);
  private final GetRecordService getRecordService = Mockito.mock(GetRecordService.class);
  /**
   * Class to test
   */
  private LocalHarvesterConsumerService remoteHarvesterConsumerService;

  @Before
  public void setUp() {
    remoteHarvesterConsumerService = new LocalHarvesterConsumerService(listRecordHeadersService, getRecordService);
  }

  @Test
  public void shouldLogOaiErrorCodeAndMessageWhenAnOaiExceptionIsThrown() throws CustomHandlerException {
    // When;
    Mockito.when(listRecordHeadersService.getRecordHeaders(UKDS_REPO)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument, "Invalid argument"));

    // Then
    List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(UKDS_REPO, null);
    Assert.assertTrue(recordHeaders.isEmpty());
  }

  @Test
  public void shouldLogOaiErrorCodeWhenAnOaiExceptionIsThrown() throws CustomHandlerException {
    // When
    Mockito.when(listRecordHeadersService.getRecordHeaders(UKDS_REPO)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument));

    // Then
    List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(UKDS_REPO, null);
    Assert.assertTrue(recordHeaders.isEmpty());
  }

  @Test
  public void shouldLogWhenACustomHandlerExceptionIsThrown() throws CustomHandlerException {
    // When
    Mockito.when(listRecordHeadersService.getRecordHeaders(UKDS_REPO)).thenThrow(CustomHandlerException.class);

    // Then
    List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(UKDS_REPO, null);
    Assert.assertTrue(recordHeaders.isEmpty());
  }

  @Test
  public void getRecordShouldLogOaiErrorCodeAndMessageWhenAnOaiExceptionIsThrown() throws CustomHandlerException {
    // When
    Mockito.when(getRecordService.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument, "Invalid argument"));

    // Then
    var record = remoteHarvesterConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
    Assert.assertTrue(record.isEmpty());
  }

  @Test
  public void getRecordShouldLogOaiErrorCodeWhenAnOaiExceptionIsThrown() throws CustomHandlerException {
    // When
    Mockito.when(getRecordService.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument));

    // Then
    var record = remoteHarvesterConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
    Assert.assertTrue(record.isEmpty());
  }

  @Test
  public void getRecordShouldLogWhenACustomHandlerExceptionIsThrown() throws CustomHandlerException {
    // When
    Mockito.when(getRecordService.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(CustomHandlerException.class);

    // Then
    var record = remoteHarvesterConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
    Assert.assertTrue(record.isEmpty());
  }
}
