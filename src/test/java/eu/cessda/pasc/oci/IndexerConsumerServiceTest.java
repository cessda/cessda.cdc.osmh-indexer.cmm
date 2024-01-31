/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.parser.RecordXMLParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static eu.cessda.pasc.oci.mock.data.ReposTestData.getUKDSRepo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests related to {@link IndexerConsumerService}
 *
 * @author moses AT doraventures DOT com
 */
public class IndexerConsumerServiceTest {


    private static final Repo UKDS_REPO = getUKDSRepo();

    private final RecordXMLParser recordXMLParser = Mockito.mock(RecordXMLParser.class);
    private final LanguageExtractor languageExtractor = Mockito.mock(LanguageExtractor.class);
    /**
     * Class to test
     */
    private IndexerConsumerService indexerConsumerService;

    @Before
    public void setUp() {
        indexerConsumerService = new IndexerConsumerService(languageExtractor, recordXMLParser);
    }

    @Test
    public void shouldLogWhenAnXMLParseExceptionIsThrown() throws IndexerException {
        // When
        Mockito.when(recordXMLParser.getRecord(eq(UKDS_REPO), any(Path.class))).thenThrow(XMLParseException.class);

        // Then
        var recordHeaders = indexerConsumerService.getRecords(UKDS_REPO);
        Assert.assertTrue(recordHeaders.isEmpty());
    }

    @Test
    public void getRecordShouldLogWhenACustomHandlerExceptionIsThrown() throws IndexerException {
        // When
        Mockito.when(recordXMLParser.getRecord(eq(UKDS_REPO), any(Path.class))).thenThrow(XMLParseException.class);

        // Then
        var record = indexerConsumerService.getRecord(UKDS_REPO, Path.of("."));
        Assert.assertTrue(record.isEmpty());
    }

    @Test
    public void shouldThrowIfAPathIsNotConfigured() {
        // Given
        var ukdsEndpoint  = ReposTestData.getGesisEnRepo();

        // When
        assertThatThrownBy(() -> indexerConsumerService.getRecords(ukdsEndpoint))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
