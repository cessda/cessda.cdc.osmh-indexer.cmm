/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.LoggingConstants;
import eu.cessda.pasc.oci.exception.HarvesterException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.value;

@Service
@Slf4j
public class LocalHarvesterConsumerService extends AbstractHarvesterConsumerService {

    private final RecordHeaderParser recordHeaderParser;
    private final GetRecordService getRecordService;

    @Autowired
    public LocalHarvesterConsumerService(RecordHeaderParser recordHeaderParser, GetRecordService getRecordService) {
        this.recordHeaderParser = recordHeaderParser;
        this.getRecordService = getRecordService;
    }

    @Override
    public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
        try {
            List<RecordHeader> recordHeaders = recordHeaderParser.getRecordHeaders(repo);
            return filterRecords(recordHeaders, lastModifiedDate);
        } catch (OaiPmhException e) {
            // Check if there was a message attached to the OAI error response
            e.getOaiErrorMessage().ifPresentOrElse(
                oaiErrorMessage -> log.error("[{}] ListRecordHeaders failed. {}: {}",
                    repo.getCode(),
                    value(LoggingConstants.OAI_ERROR_CODE, e.getCode()),
                    value(LoggingConstants.OAI_ERROR_MESSAGE, oaiErrorMessage)
                ),
                () -> log.error("[{}] ListRecordHeaders failed. {}.",
                    repo.getCode(),
                    value(LoggingConstants.OAI_ERROR_CODE, e.getCode())
                )
            );
        } catch (HarvesterException e) {
            log.error("[{}] ListRecordHeaders failed:", value(LoggingConstants.REPO_NAME, repo.getCode()), e);
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<CMMStudy> getRecordFromRemote(Repo repo, RecordHeader recordHeader) {
        try {
            return Optional.of(getRecordService.getRecord(repo, recordHeader.getIdentifier()));
        } catch (OaiPmhException e) {
            e.getOaiErrorMessage().ifPresentOrElse(
                oaiErrorMessage -> log.warn(FAILED_TO_GET_STUDY_ID + ": {}",
                    repo.getCode(),
                    value(LoggingConstants.STUDY_ID, recordHeader.getIdentifier()),
                    value(LoggingConstants.OAI_ERROR_CODE, e.getCode()),
                    value(LoggingConstants.OAI_ERROR_MESSAGE, oaiErrorMessage)
                ),
                () -> log.warn(FAILED_TO_GET_STUDY_ID,
                    repo.getCode(),
                    value(LoggingConstants.STUDY_ID, recordHeader.getIdentifier()),
                    value(LoggingConstants.OAI_ERROR_CODE, e.getCode())
                )
            );
        } catch (HarvesterException e) {
            log.warn(FAILED_TO_GET_STUDY_ID,
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value(LoggingConstants.STUDY_ID, recordHeader.getIdentifier()),
                e.toString()
            );
        }
        return Optional.empty();
    }
}
