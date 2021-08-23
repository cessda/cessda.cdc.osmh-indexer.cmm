/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.exception.HarvesterException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.RecordHeaderParser;
import eu.cessda.pasc.oci.parser.RecordXMLParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.stream.Collectors;

import static eu.cessda.pasc.oci.mock.data.ReposTestData.getUKDSRepo;

/**
 * Tests related to {@link LocalHarvesterConsumerService}
 *
 * @author moses AT doraventures DOT com
 */
public class LocalHarvesterConsumerServiceTest {


    private static final Repo UKDS_REPO = getUKDSRepo();
    private static final Record STUDY_NUMBER = new Record(RecordHeader.builder().identifier("oai:ukds/5436").build(), null);
    private final RecordHeaderParser recordHeaderParser = Mockito.mock(RecordHeaderParser.class);
    private final RecordXMLParser recordXMLParser = Mockito.mock(RecordXMLParser.class);
    /**
     * Class to test
     */
    private LocalHarvesterConsumerService remoteHarvesterConsumerService;

    @Before
    public void setUp() {
        remoteHarvesterConsumerService = new LocalHarvesterConsumerService(recordHeaderParser, recordXMLParser);
    }

    @Test
    public void shouldLogOaiErrorCodeAndMessageWhenAnOaiExceptionIsThrown() throws HarvesterException {
        // When;
        Mockito.when(recordHeaderParser.getRecordHeaders(UKDS_REPO)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument, "Invalid argument"));

        // Then
        var recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(UKDS_REPO, null).collect(Collectors.toList());
        Assert.assertTrue(recordHeaders.isEmpty());
    }

    @Test
    public void shouldLogOaiErrorCodeWhenAnOaiExceptionIsThrown() throws HarvesterException {
        // When
        Mockito.when(recordHeaderParser.getRecordHeaders(UKDS_REPO)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument));

        // Then
        var recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(UKDS_REPO, null).collect(Collectors.toList());
        Assert.assertTrue(recordHeaders.isEmpty());
    }

    @Test
    public void shouldLogWhenACustomHandlerExceptionIsThrown() throws HarvesterException {
        // When
        Mockito.when(recordHeaderParser.getRecordHeaders(UKDS_REPO)).thenThrow(HarvesterException.class);

        // Then
        var recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(UKDS_REPO, null).collect(Collectors.toList());
        Assert.assertTrue(recordHeaders.isEmpty());
    }

    @Test
    public void getRecordShouldLogOaiErrorCodeAndMessageWhenAnOaiExceptionIsThrown() throws HarvesterException {
        // When
        Mockito.when(recordXMLParser.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument, "Invalid argument"));

        // Then
        var record = remoteHarvesterConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
        Assert.assertTrue(record.isEmpty());
    }

    @Test
    public void getRecordShouldLogOaiErrorCodeWhenAnOaiExceptionIsThrown() throws HarvesterException {
        // When
        Mockito.when(recordXMLParser.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument));

        // Then
        var record = remoteHarvesterConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
        Assert.assertTrue(record.isEmpty());
    }

    @Test
    public void getRecordShouldLogWhenACustomHandlerExceptionIsThrown() throws HarvesterException {
        // When
        Mockito.when(recordXMLParser.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(HarvesterException.class);

        // Then
        var record = remoteHarvesterConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
        Assert.assertTrue(record.isEmpty());
    }
}
