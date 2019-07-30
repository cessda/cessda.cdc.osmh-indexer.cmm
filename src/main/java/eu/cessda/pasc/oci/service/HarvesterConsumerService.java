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
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for consuming harvested and Metadata via OSMH Harvester
 *
 * @author moses AT doravenetures DOT com
 */
public interface HarvesterConsumerService {

  /**
   * Queries the remote repo for RecordHeaders.  RecordHeaders are filtered if lastModifiedDate is provided.
   *
   * @param repo repository details.
   * @param lastModifiedDate to filter headers on.
   * @return List RecordHeaders.
   */
  List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate);

  /**
   * Queries the remote repo for a Record.
   *
   * @param repo repository details.
   * @param studyNumber the remote Study Identifier.
   * @return Record instance.
   */
  Optional<CMMStudy> getRecord(Repo repo, String studyNumber);
}
