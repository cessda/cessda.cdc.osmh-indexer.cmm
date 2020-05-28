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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.LIST_RECORDER_HEADERS_BODY_EXAMPLE;
import static org.assertj.core.api.Java6BDDAssertions.then;


/**
 * Test for the Harvester
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
public class PascHarvesterDaoTest extends AbstractSpringTestProfileContext {

  @Autowired
  private AppConfigurationProperties appConfigurationProperties;

  private final HttpClientMock httpClient = new HttpClientMock();

  @Test
  public void shouldReturnSuccessfulHttpResponseListRecord() throws IOException {

      // Given
      String expectedUrl = "http://cdc-osmh-repo:9091/v0/ListRecordHeaders?" +
              "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

      httpClient.onGet(expectedUrl).doReturnJSON(LIST_RECORDER_HEADERS_BODY_EXAMPLE, StandardCharsets.UTF_8);

      // When
      DaoBaseImpl daoBase = new DaoBaseImpl(httpClient);
      try (InputStream recordHeaders = daoBase.getInputStream(expectedUrl)) {
          then(new String(recordHeaders.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(LIST_RECORDER_HEADERS_BODY_EXAMPLE);
          httpClient.verify().get().called(1);
      }
  }

  @Test
  public void shouldReturnSuccessfulHttpResponseForGetRecord() throws IOException {

      // Given
      String expectedUrl = "http://cdc-osmh-repo:9091/v0/GetRecord/CMMStudy/998?" +
              "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

      httpClient.onGet(expectedUrl).doReturnJSON(expectedUrl, StandardCharsets.UTF_8);

      // When
      DaoBaseImpl daoBase = new DaoBaseImpl(httpClient);
      try (InputStream recordHeaders = daoBase.getInputStream(expectedUrl)) {
          then(new String(recordHeaders.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(expectedUrl);
          httpClient.verify().get().called(1);
      }
  }
}
