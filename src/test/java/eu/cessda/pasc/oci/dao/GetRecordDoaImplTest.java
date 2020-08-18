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
import eu.cessda.pasc.oci.exception.HTTPException;
import eu.cessda.pasc.oci.repository.DaoBaseImpl;
import org.junit.Test;

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
public class GetRecordDoaImplTest {


    private final HttpClientMock httpClient = new HttpClientMock();

    @Test
    public void shouldReturnXMLPayloadOfGivenRecordIdentifierFromGivenRepoURL() throws IOException {

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
    @Test(expected = HTTPException.class)
    public void shouldThrowExceptionWhenRemoteServerResponseIsNotSuccessful() throws IOException {

        // Given
        String expectedFullGetRecordUrl = "https://the.remote.endpoint/?verb=GetRecord&identifier=1683&metadataPrefix=ddi";

        httpClient.onGet(expectedFullGetRecordUrl).doReturn(500, expectedFullGetRecordUrl);

        // When
        var recordDoa = new DaoBaseImpl(httpClient);
        recordDoa.getInputStream(URI.create(expectedFullGetRecordUrl));
    }
}
