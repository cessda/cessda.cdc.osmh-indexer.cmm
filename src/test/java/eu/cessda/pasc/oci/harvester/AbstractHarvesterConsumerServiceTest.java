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
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbstractHarvesterConsumerServiceTest {

    private final AbstractHarvesterConsumerService abstractHarvesterConsumerService = new AbstractHarvesterConsumerService() {
        @Override
        public Stream<Record> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
            return Stream.empty();
        }

        @Override
        protected Optional<CMMStudy> getRecordFromRemote(Repo repo, Record recordHeader) {
            return Optional.empty();
        }
    };

    @Test
    public void shouldReturnAnInactiveRecordIfMarkedDeleted() {
        var header = RecordHeader.builder().deleted(true).build();

        var study = abstractHarvesterConsumerService.getRecord(null, header);
        assertTrue(study.isPresent());
        assertFalse(study.get().isActive());
    }

    @Test
    public void shouldWarnOnNotParsableDate() {
        var header = RecordHeader.builder().lastModified("Not a date").build();

        // When
        var records = AbstractHarvesterConsumerService.filterRecord(header, LocalDateTime.now());

        // Then the record should be filtered
        assertFalse(records);
    }

    @Test
    public void shouldNotFilterOnNullLastModifiedDate() {
        // Construct an object to be used for identity purposes
        var header = RecordHeader.builder().lastModified(LocalDateTime.now().toString()).build();

        // When
        var records = AbstractHarvesterConsumerService.filterRecord(header, null);

        // Then the same object should be returned
        assertTrue(records);
    }
}