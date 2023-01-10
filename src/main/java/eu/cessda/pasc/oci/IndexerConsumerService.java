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
package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.RecordHeaderParser;
import eu.cessda.pasc.oci.parser.RecordXMLParser;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.io.Files.getFileExtension;
import static net.logstash.logback.argument.StructuredArguments.value;

@Service
@Slf4j
public class IndexerConsumerService {

    protected static final String FAILED_TO_GET_STUDY_ID = "[{}] Failed to get StudyId [{}]: {}";
    protected static final String LIST_RECORD_HEADERS_FAILED = "[{}] ListRecordHeaders failed: {}";
    protected static final String LIST_RECORD_HEADERS_FAILED_WITH_MESSAGE = LIST_RECORD_HEADERS_FAILED + ": {}";
    protected static final String FAILED_TO_GET_STUDY_ID_WITH_MESSAGE = FAILED_TO_GET_STUDY_ID + ": {}";
    private final RecordHeaderParser recordHeaderParser;
    private final RecordXMLParser recordXMLParser;
    private final LanguageExtractor languageExtractor;

    @Autowired
    public IndexerConsumerService(LanguageExtractor languageExtractor, RecordHeaderParser recordHeaderParser, RecordXMLParser recordXMLParser) {
        this.languageExtractor = languageExtractor;
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

    /**
     * Queries the remote repository for records. Records are filtered if lastModifiedDate is provided.
     *
     * @param repo             the repository to query.
     * @param lastModifiedDate to filter headers on, can be null.
     * @return a list of records retrieved from the remote repository.
     */
    @SuppressWarnings({"resource", "StreamResourceLeak", "UnstableApiUsage"}) // The stream will always be closed
    public Map<String, List<CMMStudyOfLanguage>> getRecords(Repo repo, LocalDateTime lastModifiedDate) {
        log.debug("[{}] Parsing record headers.", repo.getCode());

        Stream<Record> stream = Stream.empty();

        try {
            /*
            * Repositories are indexed from their path by default, but can be indexed directly from
            * OAI-PMH repositories if a path is not defined but a URL is.
            */
            if (repo.getPath() != null) {
                stream = Files.find(repo.getPath(), 1, (path, attributes) ->
                    // Find XML files in the source directory.
                    attributes.isRegularFile() && getFileExtension(path.toString()).equals("xml")
                ).flatMap(path -> {
                    try {
                        return recordHeaderParser.getRecordHeaders(repo, path);
                    } catch (XMLParseException e) {
                        log.warn("[{}]: Failed to parse study from [{}]: {}", repo.getCode(), path, e.toString());
                        return Stream.empty();
                    }
                });
            } else if (repo.getUrl() != null) {
                var recordHeaders = recordHeaderParser.getRecordHeaders(repo);
                stream = recordHeaders.parallelStream().map(recordHeader -> new Record(recordHeader, new Record.Request(repo.getUrl(), repo.getPreferredMetadataParam()), null));
            } else {
                throw new IllegalArgumentException("Repo " + repo.getCode() + " has no URL or path defined");
            }

            var studies = new AtomicInteger();

            var studiesByLanguage = stream
                .filter(header -> filterRecord(header.getRecordHeader(), lastModifiedDate))
                .map(header -> getRecord(repo, header))
                .flatMap(Optional::stream)
                .flatMap(cmmStudy -> {
                    // Extract language specific variants of the record
                    var extractedStudies = languageExtractor.extractFromStudy(cmmStudy, repo);
                    if (!extractedStudies.isEmpty()) {
                        studies.getAndIncrement();
                    }
                    return extractedStudies.entrySet().stream();
                })
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

            log.info("[{}] Retrieved [{}] studies.",
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value("present_cmm_record", studies.get())
            );

            return studiesByLanguage;
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
        }
        catch (IndexerException | IOException e) {
            log.error(LIST_RECORD_HEADERS_FAILED_WITH_MESSAGE,
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value(LoggingConstants.EXCEPTION_NAME, e.getClass().getName()),
                value(LoggingConstants.REASON, e.getMessage())
            );
        } finally {
            stream.close();
        }
        return Collections.emptyMap();
    }

    /**
     * Retrieve a record from a record header.
     * @param repo the repository that the record originated from
     * @param recordVar the record header.
     * @return an {@link Optional} containing a {@link CMMStudy} or an empty optional if an error occurred.
     */
    Optional<CMMStudy> getRecord(Repo repo, Record recordVar) {
        // Handle deleted records
        if (recordVar.getRecordHeader().isDeleted()) {
            return Optional.empty();
        }

        try (var identifierClosable = MDC.putCloseable(LoggingConstants.STUDY_ID, recordVar.getRecordHeader().getIdentifier())) {
            return recordXMLParser.getRecord(repo, recordVar);
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
