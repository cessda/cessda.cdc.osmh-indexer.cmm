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
package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.RecordHeaderParser;
import eu.cessda.pasc.oci.parser.RecordXMLParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static eu.cessda.pasc.oci.mock.data.ReposTestData.getUKDSRepo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to {@link IndexerConsumerService}
 *
 * @author moses AT doraventures DOT com
 */
public class IndexerConsumerServiceTest {


    private static final Repo UKDS_REPO = getUKDSRepo();
    private static final Record STUDY_NUMBER = new Record(RecordHeader.builder().identifier("oai:ukds/5436").build(), null,null);
    private final RecordHeaderParser recordHeaderParser = Mockito.mock(RecordHeaderParser.class);
    private final RecordXMLParser recordXMLParser = Mockito.mock(RecordXMLParser.class);
    private final LanguageExtractor languageExtractor = Mockito.mock(LanguageExtractor.class);
    /**
     * Class to test
     */
    private IndexerConsumerService indexerConsumerService;

    @Before
    public void setUp() {
        indexerConsumerService = new IndexerConsumerService(languageExtractor, recordHeaderParser, recordXMLParser);
    }

    @Test
    public void shouldLogOaiErrorCodeAndMessageWhenAnOaiExceptionIsThrown() throws IndexerException {
        // When;
        Mockito.when(recordHeaderParser.getRecordHeaders(UKDS_REPO)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument, "Invalid argument"));

        // Then
        var recordHeaders = indexerConsumerService.getRecords(UKDS_REPO, null);
        Assert.assertTrue(recordHeaders.isEmpty());
    }

    @Test
    public void shouldLogOaiErrorCodeWhenAnOaiExceptionIsThrown() throws IndexerException {
        // When
        Mockito.when(recordHeaderParser.getRecordHeaders(UKDS_REPO)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument));

        // Then
        var recordHeaders = indexerConsumerService.getRecords(UKDS_REPO, null);
        Assert.assertTrue(recordHeaders.isEmpty());
    }

    @Test
    public void shouldLogWhenACustomHandlerExceptionIsThrown() throws IndexerException {
        // When
        Mockito.when(recordHeaderParser.getRecordHeaders(UKDS_REPO)).thenThrow(IndexerException.class);

        // Then
        var recordHeaders = indexerConsumerService.getRecords(UKDS_REPO, null);
        Assert.assertTrue(recordHeaders.isEmpty());
    }

    @Test
    public void getRecordShouldLogOaiErrorCodeAndMessageWhenAnOaiExceptionIsThrown() throws IndexerException {
        // When
        Mockito.when(recordXMLParser.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument, "Invalid argument"));

        // Then
        var record = indexerConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
        Assert.assertTrue(record.isEmpty());
    }

    @Test
    public void getRecordShouldLogOaiErrorCodeWhenAnOaiExceptionIsThrown() throws IndexerException {
        // When
        Mockito.when(recordXMLParser.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(new OaiPmhException(OaiPmhException.Code.badArgument));

        // Then
        var record = indexerConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
        Assert.assertTrue(record.isEmpty());
    }

    @Test
    public void getRecordShouldLogWhenACustomHandlerExceptionIsThrown() throws IndexerException {
        // When
        Mockito.when(recordXMLParser.getRecord(UKDS_REPO, STUDY_NUMBER)).thenThrow(IndexerException.class);

        // Then
        var record = indexerConsumerService.getRecord(UKDS_REPO, STUDY_NUMBER);
        Assert.assertTrue(record.isEmpty());
    }

    @Test
    public void shouldReturnAnInactiveRecordIfMarkedDeleted() {
        var header = RecordHeader.builder().deleted(true).build();

        var study = indexerConsumerService.getRecord(null, new Record(header, null, null));
        assertFalse(study.isPresent());
    }

    @Test
    public void shouldWarnOnNotParsableDate() {
        var header = RecordHeader.builder().lastModified("Not a date").build();

        // When
        var records = IndexerConsumerService.filterRecord(header, LocalDateTime.now());

        // Then the record should be filtered
        assertFalse(records);
    }

    @Test
    public void shouldNotFilterOnNullLastModifiedDate() {
        // Construct an object to be used for identity purposes
        var header = RecordHeader.builder().lastModified(LocalDateTime.now().toString()).build();

        // When
        var records = IndexerConsumerService.filterRecord(header, null);

        // Then the same object should be returned
        assertTrue(records);
    }

    @Test
    public void shouldThrowIfAURLAndAPathIsNotConfigured() {
        // Given
        var ukdsEndpoint  = ReposTestData.getUKDSRepo();
        ukdsEndpoint.setUrl(null);

        // When
        assertThatThrownBy(() -> indexerConsumerService.getRecords(ukdsEndpoint, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
