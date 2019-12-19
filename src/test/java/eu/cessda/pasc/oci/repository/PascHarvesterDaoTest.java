/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import static eu.cessda.pasc.oci.data.RecordTestData.LIST_RECORDER_HEADERS_BODY_EXAMPLE;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;


/**
 * Test for the Harvester
 *
 * @author moses AT doravenetures DOT com
 */
@RunWith(SpringRunner.class)
public class PascHarvesterDaoTest extends AbstractSpringTestProfileContext {

  @Autowired
  private RestTemplate restTemplate;
  private MockRestServiceServer serverMock;

  @Autowired
  private PascHarvesterDao pascHarvesterDao;


  @Before
  public void setUp() {
    serverMock = MockRestServiceServer.bindTo(restTemplate).build();
  }

  @Test
  public void shouldReturnSuccessfulHttpResponseListRecord() throws ExternalSystemException {

    // Given
    String expectedUrl = "http://cdc-osmh-repo:9091/v0/ListRecordHeaders?" +
        "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

    serverMock.expect(once(), requestTo(expectedUrl))
        .andExpect(method(GET))
        .andRespond(MockRestResponseCreators.withSuccess(LIST_RECORDER_HEADERS_BODY_EXAMPLE, MediaType.APPLICATION_JSON)
        );

    // When
    String recordHeaders = pascHarvesterDao.listRecordHeaders(expectedUrl);

    then(recordHeaders).isEqualTo(LIST_RECORDER_HEADERS_BODY_EXAMPLE);
  }

  @Test
  public void shouldReturnSuccessfulHttpResponseForGetRecord() throws ExternalSystemException {

    // Given
    FileHandler fileHandler = new FileHandler();
    String recordUkds998 = fileHandler.getFileWithUtil("record_ukds_998.json");
    String expectedUrl = "http://cdc-osmh-repo:9091/v0/GetRecord/CMMStudy/998?" +
        "Repository=https://oai.ukdataservice.ac.uk:8443/oai/provider";

    serverMock.expect(once(), requestTo(expectedUrl))
        .andExpect(method(GET))
        .andRespond(MockRestResponseCreators.withSuccess(recordUkds998, MediaType.APPLICATION_JSON)
        );

    // When
    String record = pascHarvesterDao.getRecord(expectedUrl);

    then(record).isEqualTo(recordUkds998);
  }
}
