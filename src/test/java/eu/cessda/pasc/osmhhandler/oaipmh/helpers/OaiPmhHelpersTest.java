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
package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendGetRecordParams;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.decodeStudyNumber;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Test class for {@link OaiPmhHelpers}
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class OaiPmhHelpersTest {


  @Autowired
  private HandlerConfigurationProperties handlerConfigurationProperties;

  @Test
  public void ShouldAppendMetaDataPrefixForGivenFSD() throws CustomHandlerException {

    // Given
    String fsdEndpoint = "http://services.fsd.uta.fi/v0/oai";
    String expectedReqUrl = "http://services.fsd.uta.fi/v0/oai?verb=GetRecord&identifier=15454&metadataPrefix=oai_ddi25";

    // When
    String builtUrl = appendGetRecordParams(fsdEndpoint, "15454", handlerConfigurationProperties.getOaiPmh());

    then(builtUrl).isEqualTo(expectedReqUrl);
  }

  @Test
  public void ShouldAppendMetaDataPrefixForGivenUKDS() throws CustomHandlerException {

    // Given
    String fsdEndpoint = "https://oai.ukdataservice.ac.uk:8443/oai/provider";
    String expectedReqUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=15454&metadataPrefix=ddi";

    // When
    String builtUrl = appendGetRecordParams(fsdEndpoint, "15454", handlerConfigurationProperties.getOaiPmh());

    then(builtUrl).isEqualTo(expectedReqUrl);
  }

  @Test(expected = CustomHandlerException.class)
  public void ShouldThrowExceptionForANonConfiguredRepo() throws CustomHandlerException {
    // When
    appendGetRecordParams("http://services.inthe.future/v0/oai", "15454", handlerConfigurationProperties.getOaiPmh());
  }

  @Test
  public void shouldDecodeStudyNumberSpecialCharactersBackToOriginalForm() {

    // Given
    String studyNumberWithRestCharactersEncoded = "oai_cl_dbk_dt_gesis_dt_org_cl_DBK_sl_ZA0001";

    // When
    String result = decodeStudyNumber(studyNumberWithRestCharactersEncoded);

    then(result).isEqualTo("oai:dbk.gesis.org:DBK/ZA0001");
  }
}