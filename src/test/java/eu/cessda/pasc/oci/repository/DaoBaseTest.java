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
package eu.cessda.pasc.oci.repository;

import com.pgssoft.httpclient.HttpClientMock;
import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static eu.cessda.pasc.oci.data.RecordTestData.LIST_RECORDER_HEADERS_BODY_EXAMPLE;
import static org.assertj.core.api.Java6BDDAssertions.then;


/**
 * Test for the DaoBase
 *
 * @author moses AT doraventures DOT com
 */
@RunWith( SpringRunner.class )
public class DaoBaseTest extends AbstractSpringTestProfileContext
{
    @Autowired
    private AppConfigurationProperties appConfigurationProperties;

    private HttpClientMock httpClient = new HttpClientMock();

    @Test
    public void shouldPostForStringResponse() throws ExternalSystemException, IOException {

        // Given
        String expectedUrl = "http://cdc-osmh-repo:9091/v0/ListRecordHeaders?" +
                "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

        httpClient.onGet(expectedUrl).doReturnJSON(LIST_RECORDER_HEADERS_BODY_EXAMPLE, StandardCharsets.UTF_8);

        // When
        DaoBase daoBase = new DaoBase(httpClient, appConfigurationProperties);
        try (InputStream recordHeaders = daoBase.postForStringResponse(expectedUrl)) {
            then(new String(recordHeaders.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(LIST_RECORDER_HEADERS_BODY_EXAMPLE);
            httpClient.verify().get().called(1);
        }
    }

    @Test(expected = ExternalSystemException.class)
    public void shouldThrowExternalSystemException() throws ExternalSystemException, IOException {

        // Given
        String expectedUrl = "http://cdc-osmh-repo:9091/v0/ListRecordHeaders?" +
                "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

        httpClient.onGet(expectedUrl).doReturn(400, "The exception wasn't thrown.");

        // When
        DaoBase daoBase = new DaoBase(httpClient, appConfigurationProperties);
        try (InputStream inputStream = daoBase.postForStringResponse(expectedUrl)) {
            Assert.fail(new String(inputStream.readAllBytes(), Charset.defaultCharset()));
        }
        // then exception should be thrown.
    }
}