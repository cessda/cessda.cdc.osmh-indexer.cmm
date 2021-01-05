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

import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AbstractHarvesterConsumerServiceTest {

    private final AbstractHarvesterConsumerService abstractHarvesterConsumerService = new AbstractHarvesterConsumerService() {
        @Override
        public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
            return Collections.emptyList();
        }

        @Override
        protected Optional<CMMStudy> getRecordFromRemote(Repo repo, RecordHeader recordHeader) {
            return Optional.empty();
        }
    };

    @Test
    public void shouldReturnAnInactiveRecordIfMarkedDeleted() {
        var header = RecordHeader.builder().deleted(true).build();

        var study = abstractHarvesterConsumerService.getRecord(null, header);
        Assert.assertTrue(study.isPresent());
        Assert.assertFalse(study.get().isActive());
    }
}