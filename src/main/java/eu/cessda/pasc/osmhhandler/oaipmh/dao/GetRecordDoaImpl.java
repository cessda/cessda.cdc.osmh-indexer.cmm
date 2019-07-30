/**
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
package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendGetRecordParams;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.decodeStudyNumber;

/**
 * Data access object for fetching Record from remote repositories implementation
 *
 * @author moses AT doraventures DOT com
 */
@Repository
public class GetRecordDoaImpl extends DaoBase implements GetRecordDoa {

  private final HandlerConfigurationProperties oaiPmhConfig;

  @Autowired
  public GetRecordDoaImpl(HandlerConfigurationProperties oaiPmhConfig) {
    this.oaiPmhConfig = oaiPmhConfig;
  }

  @Override
  public String getRecordXML(String repoUrl, String studyIdentifier) throws CustomHandlerException {

    String decodedStudyId = decodeStudyNumber(studyIdentifier);
    final String fullUrl = appendGetRecordParams(repoUrl, decodedStudyId, oaiPmhConfig.getOaiPmh());
    return postForStringResponse(fullUrl);
  }
}
