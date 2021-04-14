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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.TimeUtility;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.exception.HTTPException;
import eu.cessda.pasc.oci.http.HttpClient;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.ErrorMessage;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.*;
import static eu.cessda.pasc.oci.mock.data.ReposTestData.getUKDSRepo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author moses AT doraventures DOT com
 */
public class RemoteHarvesterConsumerServiceTest {

    private final HttpClient httpClient = Mockito.mock(HttpClient.class);
    private final AppConfigurationProperties appConfigurationProperties = Mockito.mock(AppConfigurationProperties.class);

    private static final CMMStudyConverter cmmStudyConverter = new CMMStudyConverter();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final RecordHeader STUDY_NUMBER = RecordHeader.builder().identifier("4124325").build();

    /**
     * Class to test
     */
    private RemoteHarvesterConsumerService remoteHarvesterConsumerService;

    @Before
    public void setUp() {
        Mockito.when(appConfigurationProperties.getEndpoints()).thenReturn(ReposTestData.getEndpoints());
        remoteHarvesterConsumerService = new RemoteHarvesterConsumerService(appConfigurationProperties, cmmStudyConverter, httpClient, objectMapper);
    }

    @Test
    public void shouldReturnASuccessfulResponseForListingRecordHeaders() throws IOException {

        when(httpClient.getInputStream(any(URI.class))).thenReturn(
            new ByteArrayInputStream(LIST_RECORDER_HEADERS_BODY_EXAMPLE.getBytes(StandardCharsets.UTF_8))
        );
        Repo repo = getUKDSRepo();

        List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(repo, null);
        assertThat(recordHeaders).hasSize(2);
    }

    @Test
    public void shouldReturnFilterRecordHeadersByLastModifiedDate() throws IOException, DateNotParsedException {
        Repo repo = getUKDSRepo();
        var lastModifiedDateCutOff = TimeUtility.getLocalDateTime("2018-02-01T07:48:38Z");

        when(httpClient.getInputStream(any(URI.class))).thenReturn(
            new ByteArrayInputStream(LIST_RECORDER_HEADERS_X6.getBytes(StandardCharsets.UTF_8))
        );
        List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(repo, lastModifiedDateCutOff);

        assertThat(recordHeaders).hasSize(2);
        for (RecordHeader recordHeader : recordHeaders) {
            var currentLastModified = TimeUtility.getLocalDateTime(recordHeader.getLastModified());
            then(currentLastModified).isGreaterThan(lastModifiedDateCutOff);
        }
    }

    @Test
    public void shouldFilterRecordHeadersByLastModifiedDateAndInvalidLastDateTimeStrings() throws IOException, DateNotParsedException {
        Repo repo = getUKDSRepo();
        var lastModifiedCutOff = TimeUtility.getLocalDateTime("2018-02-10T07:48:38Z");

        when(httpClient.getInputStream(any(URI.class))).thenReturn(
            new ByteArrayInputStream(LIST_RECORDER_HEADERS_WITH_INVALID_DATETIME.getBytes(StandardCharsets.UTF_8))
        );
        List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(repo, lastModifiedCutOff);

        assertThat(recordHeaders).hasSize(1);
        for (RecordHeader recordHeader : recordHeaders) {
            var currentLastModified = TimeUtility.getLocalDateTime(recordHeader.getLastModified());
            then(currentLastModified).isGreaterThan(lastModifiedCutOff);
        }
    }

    @Test
    public void shouldReturnUnFilterRecordHeadersWhenLastModifiedDateIsNull() throws IOException {

        // FIXME: add more mocks to LIST_RECORDER_HEADERS_BODY_EXAMPLE with very old timestamps to filter out
        when(httpClient.getInputStream(any(URI.class))).thenReturn(
            new ByteArrayInputStream(LIST_RECORDER_HEADERS_X6.getBytes(StandardCharsets.UTF_8))
        );
        Repo repo = getUKDSRepo();

        List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(repo, null);
        assertThat(recordHeaders).hasSize(6);
    }

    @Test
    public void shouldReturnEmptyHeaderListWhenExternalSystemExceptionIsThrown() throws IOException {

        when(httpClient.getInputStream(any(URI.class))).thenThrow(new IOException("Mocked!"));
        Repo repo = getUKDSRepo();

        List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(repo, null);
        assertThat(recordHeaders).isEmpty();
    }

    @Test
    public void shouldReturnEmptyListWhenIOExceptionIsThrown() throws IOException {

        when(httpClient.getInputStream(any(URI.class))).thenThrow(IOException.class);
        Repo repo = getUKDSRepo();

        List<RecordHeader> recordHeaders = remoteHarvesterConsumerService.listRecordHeaders(repo, null);
        assertThat(recordHeaders).isEmpty();
    }

    @Test
    public void shouldReturnEmptyCMMStudyListWhenExternalSystemExceptionIsThrown() throws IOException {
        // Given
        Repo repoMock = mock(Repo.class);
        when(repoMock.getUrl()).thenReturn(URI.create("https://oai.ukdataservice.ac.uk:8443/oai/provider"));
        when(repoMock.getHandler()).thenReturn("NESSTAR");

        var jsonMessage = objectMapper.writeValueAsString(new ErrorMessage("eu.cessda.Exception", "Mocked server error!", null));

        when(httpClient.getInputStream(any(URI.class))).thenThrow(new HTTPException(500, jsonMessage.getBytes(StandardCharsets.UTF_8)));

        Optional<CMMStudy> actualRecord = remoteHarvesterConsumerService.getRecord(repoMock, STUDY_NUMBER);

        // Then exception is thrown caught and an empty list returned
        then(actualRecord.isPresent()).isFalse();
    }

    @Test
    public void shouldLogBodyIfErrorMessageJsonCannotBeDecoded() throws IOException {
        // Given
        Repo repoMock = mock(Repo.class);
        when(repoMock.getUrl()).thenReturn(URI.create("https://oai.ukdataservice.ac.uk:8443/oai/provider"));
        when(repoMock.getHandler()).thenReturn("NESSTAR");

        when(httpClient.getInputStream(any(URI.class))).thenThrow(new HTTPException(500, "Not a JSON string".getBytes(StandardCharsets.UTF_8)));

        Optional<CMMStudy> actualRecord = remoteHarvesterConsumerService.getRecord(repoMock, STUDY_NUMBER);

        // Then exception is thrown caught and an empty list returned
        then(actualRecord.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnEmptyCMMStudyListWhenIOExceptionIsThrown() throws IOException {
        // Given
        Repo repoMock = mock(Repo.class);
        when(repoMock.getUrl()).thenReturn(URI.create("https://oai.ukdataservice.ac.uk:8443/oai/provider"));
        when(repoMock.getHandler()).thenReturn("NESSTAR");

        when(httpClient.getInputStream(any(URI.class))).thenThrow(IOException.class);

        Optional<CMMStudy> actualRecord = remoteHarvesterConsumerService.getRecord(repoMock, STUDY_NUMBER);

        // Then exception is thrown caught and an empty list returned
        then(actualRecord.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnASuccessfulResponseGetRecord() throws IOException {
        String recordUkds998 = ResourceHandler.getResourceAsString("record_ukds_998.json");
        var recordID = RecordHeader.builder().identifier("998").build();
        URI expectedUrl = URI.create("http://localhost:9091/v0/GetRecord/CMMStudy/998?Repository=" +
            URLEncoder.encode("https://oai.ukdataservice.ac.uk:8443/oai/provider", StandardCharsets.UTF_8));

        when(httpClient.getInputStream(expectedUrl)).thenReturn(
            new ByteArrayInputStream(recordUkds998.getBytes(StandardCharsets.UTF_8))
        );
        Repo repo = getUKDSRepo();

        Optional<CMMStudy> cmmStudy = remoteHarvesterConsumerService.getRecord(repo, recordID);

        assertThat(cmmStudy).isPresent();
        then(cmmStudy.get().getStudyNumber()).isEqualTo("998");
        then(cmmStudy.get().getLastModified()).isEqualTo("2018-02-22T07:48:38Z");
        then(cmmStudy.get().getKeywords()).hasSize(1);
        then(cmmStudy.get().getKeywords().get("en")).hasSize(62);
        then(cmmStudy.get().getStudyXmlSourceUrl())
            .isEqualTo("http://services.fsd.uta.fi/v0/oai?verb=GetRecord&identifier=http://my-example_url:80/obj/fStudy" +
                "/ch.sidos.ddi.468.7773&metadataPrefix=oai_ddi25");
    }

    @Test
    public void shouldReturnDeletedRecordMarkedAsInactive() throws IOException {
        String recordUkds1031 = ResourceHandler.getResourceAsString("record_ukds_1031_deleted.json");
        var recordID = RecordHeader.builder().identifier("1031").build();
        URI expectedUrl = URI.create("http://localhost:9091/v0/GetRecord/CMMStudy/1031?Repository=" +
            URLEncoder.encode("https://oai.ukdataservice.ac.uk:8443/oai/provider", StandardCharsets.UTF_8));

        when(httpClient.getInputStream(expectedUrl)).thenReturn(
            new ByteArrayInputStream(recordUkds1031.getBytes(StandardCharsets.UTF_8))
        );
        Repo repo = getUKDSRepo();

        Optional<CMMStudy> cmmStudy = remoteHarvesterConsumerService.getRecord(repo, recordID);

        assertThat(cmmStudy).isPresent();
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

    @Test
    public void shouldAddLanguageOverrideIfPresent() throws IOException {
        String recordUkds998 = ResourceHandler.getResourceAsString("record_ukds_998.json");
        var recordID = RecordHeader.builder().identifier("998").build();
        URI expectedUrl = URI.create("http://localhost:9091/v0/GetRecord/CMMStudy/" +
            recordID.getIdentifier() +
            "?Repository=" +
            URLEncoder.encode("https://oai.ukdataservice.ac.uk:8443/oai/provider", StandardCharsets.UTF_8) +
            "&defaultLanguage=zz"
        );

        when(httpClient.getInputStream(expectedUrl)).thenReturn(
            new ByteArrayInputStream(recordUkds998.getBytes(StandardCharsets.UTF_8))
        );

        Repo repo = getUKDSRepo();
        repo.setDefaultLanguage("zz");

        Optional<CMMStudy> cmmStudy = remoteHarvesterConsumerService.getRecord(repo, recordID);

        assertThat(cmmStudy).isPresent();
    }
}
