/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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
import com.pgssoft.httpclient.MockedServerResponse;
import eu.cessda.pasc.oci.configurations.HandlerConfigurationProperties;
import eu.cessda.pasc.oci.exception.CustomHandlerException;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.repository.DaoBaseImpl;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Dao Spring Test class for retrieving record's xml
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GetRecordDoaImplTest {

    @Autowired
    private HandlerConfigurationProperties handlerConfigurationProperties;

    private final HttpClientMock httpClient = new HttpClientMock();

    @SneakyThrows
    private static void throwInterruptedException(MockedServerResponse.Builder responseBuilder) {
        throw new InterruptedException("Mocked");
    }

    @Test
    public void shouldReturnXMLPayloadOfGivenRecordIdentifierFromGivenRepoURL() throws CustomHandlerException, IOException {

        // Given
        String expectedUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=1683&metadataPrefix=ddi";

        httpClient.onGet(expectedUrl).doReturnXML(expectedUrl, StandardCharsets.UTF_8);

        // When
        var recordDoa = new DaoBaseImpl(httpClient);
        try (InputStream responseXMLRecord = recordDoa.getInputStream(URI.create(expectedUrl))) {
            then(new String(responseXMLRecord.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(expectedUrl);
        }
    }

    @SuppressWarnings("resource")
    @Test(expected = ExternalSystemException.class)
    public void shouldThrowExceptionWhenRemoteServerResponseIsNotSuccessful() throws CustomHandlerException, IOException {

        // Given
        String expectedFullGetRecordUrl = "https://the.remote.endpoint/?verb=GetRecord&identifier=1683&metadataPrefix=ddi";

        httpClient.onGet(expectedFullGetRecordUrl).doReturn(500, expectedFullGetRecordUrl);

        // When
        var recordDoa = new DaoBaseImpl(httpClient);
        recordDoa.getInputStream(URI.create(expectedFullGetRecordUrl));
    }

    @Test
    public void shouldReturnEmptyStreamWhenInterrupted() throws IOException, CustomHandlerException {

        // Given
        httpClient.onGet().doAction(GetRecordDoaImplTest::throwInterruptedException);

        // When
        var recordDoa = new DaoBaseImpl(httpClient);
        try (InputStream empty = recordDoa.getInputStream(URI.create("http://error.endpoint/"))) {
            Assert.assertEquals(-1, empty.read());
        }
    }
}
