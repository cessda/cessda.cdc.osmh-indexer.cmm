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
package eu.cessda.pasc.oci.harvester;

import eu.cessda.pasc.oci.configurations.UtilitiesConfiguration;
import eu.cessda.pasc.oci.exception.HarvesterException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.http.HttpClient;
import eu.cessda.pasc.oci.mock.data.RecordHeadersMock;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static eu.cessda.pasc.oci.parser.OaiPmhHelpers.appendListRecordResumptionToken;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;


/**
 * Tests related to {@link RecordHeaderParser}
 *
 * @author moses AT doraventures DOT com
 */
public class RecordHeaderParserTest {

    private final HttpClient httpClient = Mockito.mock(HttpClient.class);

    private final RecordHeaderParser recordHeaderParser;

    public RecordHeaderParserTest() throws ParserConfigurationException {
        recordHeaderParser = new RecordHeaderParser(httpClient, new UtilitiesConfiguration(null).documentBuilderFactory());
    }

    @Test
    public void shouldReturnRecordHeadersForGivenRepo() throws IOException, HarvesterException {

        // Given
        Repo ukdsEndpoint = ReposTestData.getUKDSRepo();
        String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";
        String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLResumptionEmpty();
        given(httpClient.getInputStream(URI.create(fullListRecordRepoUrl))).willReturn(
            new ByteArrayInputStream(mockRecordHeadersXml.getBytes(StandardCharsets.UTF_8))
        );

        // When
        List<RecordHeader> recordHeaders = recordHeaderParser.getRecordHeaders(ukdsEndpoint);

        then(recordHeaders).hasSize(3);
        then(recordHeaders).extracting("identifier").containsOnly("850229", "850232", "850235");
        then(recordHeaders).extracting("lastModified").containsOnly("2017-11-20T10:37:18Z");
        then(recordHeaders).extracting("type").containsOnly("Study");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenRequestForHeaderFails() throws IOException, HarvesterException {

        // Given
        Repo ukdsEndpoint = ReposTestData.getUKDSRepo();
        String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";
        String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLResumptionTokenNotMockedForInvalid();
        given(httpClient.getInputStream(URI.create(fullListRecordRepoUrl))).willReturn(
            new ByteArrayInputStream(mockRecordHeadersXml.getBytes(StandardCharsets.UTF_8))
        );

        // When
        recordHeaderParser.getRecordHeaders(ukdsEndpoint);
    }

    @Test
    public void shouldRecursivelyLoopThroughTheOaiPMHResponseResumptionTokenToRetrieveReposCompleteListSize()
        throws IOException, HarvesterException {

        // Given
        Repo ukdsEndpoint = ReposTestData.getUKDSRepo();
        URI fullListRecordRepoUrl = URI.create(ukdsEndpoint.getUrl().toString() + "?verb=ListIdentifiers&metadataPrefix=ddi");
        String identifiersXML = RecordHeadersMock.getListIdentifiersXML();

        String resumptionToken01 = "0/3/7/ddi/null/2016-06-01/null";
        URI repoUrlWithResumptionToken01 = appendListRecordResumptionToken(ukdsEndpoint.getUrl(), resumptionToken01);
        String identifiersXMLWithResumption = RecordHeadersMock.getListIdentifiersXMLWithResumption();

        String resumptionToken02 = "3/6/7/ddi/null/2017-01-01/null";
        URI repoUrlWithResumptionToken02 = appendListRecordResumptionToken(ukdsEndpoint.getUrl(), resumptionToken02);
        String identifiersXMLWithResumptionLastList = RecordHeadersMock.getListIdentifiersXMLWithResumptionLastList();

        given(httpClient.getInputStream(fullListRecordRepoUrl)).willReturn(
            new ByteArrayInputStream(identifiersXML.getBytes(StandardCharsets.UTF_8))
        );

        given(httpClient.getInputStream(repoUrlWithResumptionToken01)).willReturn(
            new ByteArrayInputStream(identifiersXMLWithResumption.getBytes(StandardCharsets.UTF_8))
        );

        given(httpClient.getInputStream(repoUrlWithResumptionToken02)).willReturn(
            new ByteArrayInputStream(identifiersXMLWithResumptionLastList.getBytes(StandardCharsets.UTF_8))
        );

        // When
        List<RecordHeader> recordHeaders = recordHeaderParser.getRecordHeaders(ukdsEndpoint);

        then(recordHeaders).hasSize(7);
        then(recordHeaders).extracting("identifier")
            .containsOnly("850229", "850232", "850235", "7753", "8300", "8301", "998");
        then(recordHeaders).extracting("lastModified")
            .containsOnly("2017-11-20T10:37:18Z", "2018-01-11T07:43:20Z", "2018-01-11T07:43:39Z");
        then(recordHeaders).extracting("type").containsOnly("Study");
    }

    @Test(expected = OaiPmhException.class)
    public void shouldThrowExceptionForRecordHeadersInvalidMetadataToken() throws IOException, HarvesterException {

        // Given
        Repo ukdsEndpoint = ReposTestData.getUKDSRepo();

        String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";

        String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLWithInvalidMetadataTokenError();
        given(httpClient.getInputStream(URI.create(fullListRecordRepoUrl))).willReturn(
            new ByteArrayInputStream(mockRecordHeadersXml.getBytes(StandardCharsets.UTF_8))
        );

        // When
        recordHeaderParser.getRecordHeaders(ukdsEndpoint);
    }

    @Test
    public void shouldReturnDeletedRecordHeaderWhenStudyIsDeleted() throws IOException, HarvesterException {
        // Given
        Repo ukdsEndpoint = ReposTestData.getUKDSRepo();

        String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";

        String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLWithADeletedRecord();
        given(httpClient.getInputStream(URI.create(fullListRecordRepoUrl))).willReturn(
            new ByteArrayInputStream(mockRecordHeadersXml.getBytes(StandardCharsets.UTF_8))
        );

        // When
        List<RecordHeader> recordHeaders = recordHeaderParser.getRecordHeaders(ukdsEndpoint);
        Assert.assertTrue(recordHeaders.get(0).isDeleted());
    }
}
