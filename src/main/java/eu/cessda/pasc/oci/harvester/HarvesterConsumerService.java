/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.harvester;

import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service responsible for consuming harvested and Metadata via OSMH Harvester
 *
 * @author moses AT doraventures DOT com
 */
public interface HarvesterConsumerService {
    /**
     * Queries the remote repo for RecordHeaders. RecordHeaders are filtered if lastModifiedDate is provided.
     *
     * @param repo             the repository to query.
     * @param lastModifiedDate to filter headers on, can be null.
     * @return a list of record headers retrieved from the remote repository.
     */
    Stream<Record> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate);

    Optional<CMMStudy> getRecord(Repo repo, Record record);

    default Optional<CMMStudy> getRecord(Repo repo, RecordHeader recordHeader) {
        return getRecord(repo, new Record(recordHeader, null));
    }
}
