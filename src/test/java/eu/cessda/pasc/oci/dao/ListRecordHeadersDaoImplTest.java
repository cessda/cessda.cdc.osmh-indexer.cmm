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
package eu.cessda.pasc.oci.dao;

import com.pgssoft.httpclient.HttpClientMock;
import eu.cessda.pasc.oci.http.HttpClientImpl;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
public class ListRecordHeadersDaoImplTest {

    private final HttpClientMock httpClient = new HttpClientMock();

    @Test
    public void shouldReturnXmlPayloadOfRecordHeadersFromRemoteRepository() throws IOException {

        // Given
        String fullListRecordHeadersUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";

        httpClient.onGet(fullListRecordHeadersUrl).doReturn(fullListRecordHeadersUrl, StandardCharsets.UTF_8);

        // When
        var listRecordHeadersDao = new HttpClientImpl(httpClient);
        try (InputStream inputStream = listRecordHeadersDao.getInputStream(URI.create(fullListRecordHeadersUrl))) {
            String recordHeadersXML = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Actual: " + recordHeadersXML);

            then(recordHeadersXML).isNotNull();
            then(recordHeadersXML).isNotEmpty();
            then(recordHeadersXML).contains(fullListRecordHeadersUrl);
        }
    }

    @Test
    public void shouldReturnXmlPayloadOfGivenSpecSetRecordHeadersFromRemoteRepository() throws IOException {

        // Given
        String fullListRecordHeadersUrl = "http://services.fsd.uta.fi/v0/oai?verb=ListIdentifiers&metadataPrefix=oai_ddi25&set=study_groups:energia";

        httpClient.onGet(fullListRecordHeadersUrl).doReturn(fullListRecordHeadersUrl, StandardCharsets.UTF_8);

        // When
        var listRecordHeadersDao = new HttpClientImpl(httpClient);
        try (InputStream inputStream = listRecordHeadersDao.getInputStream(URI.create(fullListRecordHeadersUrl))) {
            String recordHeadersXML = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            System.out.println("Actual: " + recordHeadersXML);

            then(recordHeadersXML).isNotNull();
            then(recordHeadersXML).isNotEmpty();
            then(recordHeadersXML).contains(fullListRecordHeadersUrl);
        }
    }
}