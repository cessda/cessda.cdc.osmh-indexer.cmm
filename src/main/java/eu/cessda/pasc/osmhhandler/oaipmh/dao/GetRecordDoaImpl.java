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
package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.UtilitiesConfiguration;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Data access object for fetching Record from remote repositories implementation
 *
 * @author moses AT doraventures DOT com
 */
@Repository
public class GetRecordDoaImpl extends DaoBase implements GetRecordDoa {

  @Autowired
  public GetRecordDoaImpl(UtilitiesConfiguration configuration) {
    super(configuration);
  }

  @Override
  public String getRecordXML(String studyFullUrl) throws CustomHandlerException {

    return postForStringResponse(studyFullUrl);
  }
}
