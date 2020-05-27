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
package eu.cessda.pasc.oci.service.impl;

import eu.cessda.pasc.oci.helpers.TimeUtility;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service responsible for consuming harvested and Metadata via OSMH Harvester
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
public abstract class AbstractHarvesterConsumerService implements HarvesterConsumerService {

  protected static final String REPO_NAME = "repo_name";
  protected static final String REPO_ENDPOINT_URL = "repo_endpoint_url";
  protected static final String REASON = "rejection_reason";

  protected List<RecordHeader> filterRecords(List<RecordHeader> unfilteredRecordHeaders, LocalDateTime ingestedLastModifiedDate) {
    if (ingestedLastModifiedDate != null) {
      List<RecordHeader> filteredHeaders = unfilteredRecordHeaders.stream()
              .filter(isHeaderTimeGreater(ingestedLastModifiedDate))
              .collect(Collectors.toList());

      log.info("Returning [{}] filtered recordHeaders by date greater than [{}] | out of [{}] unfiltered.",
              filteredHeaders.size(),
              ingestedLastModifiedDate,
              unfilteredRecordHeaders.size()
      );

      return filteredHeaders;
    }

    log.debug("Nothing filterable. No date specified.");
    return unfilteredRecordHeaders;
  }

  private Predicate<RecordHeader> isHeaderTimeGreater(LocalDateTime lastModifiedDate) {
    return recordHeader -> {
      String lastModified = recordHeader.getLastModified();
      Optional<LocalDateTime> currentHeaderLastModified = TimeUtility.getLocalDateTime(lastModified);
      return currentHeaderLastModified
              .map(localDateTime -> localDateTime.isAfter(lastModifiedDate))
              .orElseGet(() -> {
                log.warn("Could not parse RecordIdentifier lastModifiedDate [{}]. Filtering out from list.", lastModified);
                return false;
              });
    };
  }
}
