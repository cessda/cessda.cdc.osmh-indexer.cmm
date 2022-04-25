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

import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.LoggingConstants;
import eu.cessda.pasc.oci.TimeUtility;
import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.RecordHeaderParser;
import eu.cessda.pasc.oci.parser.RecordXMLParser;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static net.logstash.logback.argument.StructuredArguments.value;

@Service
@Slf4j
public class IndexerConsumerService implements HarvesterConsumerService {

    protected static final String FAILED_TO_GET_STUDY_ID = "[{}] Failed to get StudyId [{}]: {}";
    protected static final String LIST_RECORD_HEADERS_FAILED = "[{}] ListRecordHeaders failed: {}";
    protected static final String LIST_RECORD_HEADERS_FAILED_WITH_MESSAGE = LIST_RECORD_HEADERS_FAILED + ": {}";
    protected static final String FAILED_TO_GET_STUDY_ID_WITH_MESSAGE = FAILED_TO_GET_STUDY_ID + ": {}";
    private final RecordHeaderParser recordHeaderParser;
    private final RecordXMLParser recordXMLParser;

    @Autowired
    public IndexerConsumerService(RecordHeaderParser recordHeaderParser, RecordXMLParser recordXMLParser) {
        this.recordHeaderParser = recordHeaderParser;
        this.recordXMLParser = recordXMLParser;
    }

    /**
     * Filter records that are newer than the specified last modified date.
     * <p/>
     * If ingestedLastModifiedDate is null no filtering will be performed and the returned list will have the same contents as unfilteredRecordHeaders.
     *
     * @param recordHeader an unfiltered record header.
     * @param ingestedLastModifiedDate the last modified date to filter by, can be null.
     * @return a list of filtered records.
     */
    protected static boolean filterRecord(RecordHeader recordHeader, LocalDateTime ingestedLastModifiedDate) {
        return ingestedLastModifiedDate == null || isHeaderTimeGreater(recordHeader, ingestedLastModifiedDate);
    }

    /**
     * Creates an inactive {@link CMMStudy} using the details in the record header.
     * <p>
     *
     * @param recordHeader the deleted record header
     */
    private static CMMStudy createInactiveRecord(RecordHeader recordHeader) {
        return CMMStudy.builder().active(false)
            .studyNumber(recordHeader.getIdentifier())
            .lastModified(recordHeader.getLastModified())
            .build();
    }

    private static boolean isHeaderTimeGreater(RecordHeader recordHeader, LocalDateTime lastModifiedDate) {
        String lastModified = recordHeader.getLastModified();
        try {
            var currentHeaderLastModified = TimeUtility.getLocalDateTime(lastModified);
            return currentHeaderLastModified.isAfter(lastModifiedDate);
        } catch (DateNotParsedException e) {
            log.warn("Could not parse lastModifiedDate. Filtering out from list: {}", e.toString());
            return false;
        }
    }

    @Override
    public Stream<Record> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
        try {
            return recordHeaderParser.getRecordHeaders(repo).filter(header -> filterRecord(header.getRecordHeader(), lastModifiedDate));
        } catch (OaiPmhException e) {
            // Check if there was a message attached to the OAI error response
            e.getOaiErrorMessage().ifPresentOrElse(
                oaiErrorMessage -> log.error(LIST_RECORD_HEADERS_FAILED_WITH_MESSAGE,
                    repo.getCode(),
                    value(LoggingConstants.OAI_ERROR_CODE, e.getCode()),
                    value(LoggingConstants.OAI_ERROR_MESSAGE, oaiErrorMessage)
                ),
                () -> log.error(LIST_RECORD_HEADERS_FAILED,
                    repo.getCode(),
                    value(LoggingConstants.OAI_ERROR_CODE, e.getCode())
                )
            );
        } catch (IndexerException e) {
            log.error(LIST_RECORD_HEADERS_FAILED_WITH_MESSAGE,
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value(LoggingConstants.EXCEPTION_NAME, e.getClass().getName()),
                value(LoggingConstants.REASON, e.getMessage())
            );
        }
        return Stream.empty();
    }

    @Override
    public Optional<CMMStudy> getRecord(Repo repo, Record recordVar) {
        // Handle deleted records
        if (recordVar.getRecordHeader().isDeleted()) {
            return Optional.of(createInactiveRecord(recordVar.getRecordHeader()));
        }

        try (var identifierClosable = MDC.putCloseable(LoggingConstants.STUDY_ID, recordVar.getRecordHeader().getIdentifier())) {
            return Optional.of(recordXMLParser.getRecord(repo, recordVar));
        } catch (OaiPmhException e) {
            e.getOaiErrorMessage().ifPresentOrElse(
                oaiErrorMessage -> log.warn(FAILED_TO_GET_STUDY_ID_WITH_MESSAGE,
                    repo.getCode(),
                    value(LoggingConstants.STUDY_ID, recordVar.getRecordHeader().getIdentifier()),
                    value(LoggingConstants.OAI_ERROR_CODE, e.getCode()),
                    value(LoggingConstants.OAI_ERROR_MESSAGE, oaiErrorMessage)
                ),
                () -> log.warn(FAILED_TO_GET_STUDY_ID,
                    repo.getCode(),
                    value(LoggingConstants.STUDY_ID, recordVar.getRecordHeader().getIdentifier()),
                    value(LoggingConstants.OAI_ERROR_CODE, e.getCode())
                )
            );
        } catch (IndexerException e) {
            log.warn(FAILED_TO_GET_STUDY_ID_WITH_MESSAGE,
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value(LoggingConstants.STUDY_ID, recordVar.getRecordHeader().getIdentifier()),
                value(LoggingConstants.EXCEPTION_NAME, e.getClass().getName()),
                value(LoggingConstants.REASON, e.getMessage())
            );
        }
        return Optional.empty();
    }
}
