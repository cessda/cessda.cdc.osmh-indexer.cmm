/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.exception.XMLParseException;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.RecordXMLParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.io.Files.getFileExtension;
import static net.logstash.logback.argument.StructuredArguments.value;

@Service
@Slf4j
public class IndexerConsumerService {

    protected static final String FAILED_TO_GET_STUDY_ID = "[{}] Failed to get StudyId [{}]: {}";
    protected static final String LIST_RECORD_HEADERS_FAILED = "[{}] ListRecordHeaders failed: {}";
    protected static final String LIST_RECORD_HEADERS_FAILED_WITH_MESSAGE = LIST_RECORD_HEADERS_FAILED + ": {}";
    protected static final String FAILED_TO_GET_STUDY_ID_WITH_MESSAGE = FAILED_TO_GET_STUDY_ID + ": {}";
    private final RecordXMLParser recordXMLParser;
    private final LanguageExtractor languageExtractor;

    @Autowired
    public IndexerConsumerService(LanguageExtractor languageExtractor, RecordXMLParser recordXMLParser) {
        this.languageExtractor = languageExtractor;
        this.recordXMLParser = recordXMLParser;
    }

    /**
     * Filter records that are newer than the specified last modified date.
     * <p/>
     * If ingestedLastModifiedDate is null no filtering will be performed and the returned list will have the same contents as unfilteredRecordHeaders.
     *
     * @param lastModifiedString an unfiltered record header.
     * @param ingestedLastModifiedDate the last modified date to filter by, can be null.
     * @return {@code true} if lastModifiedString is greater than ingestedLastModifiedDate, {@code false} otherwise
     */
    protected static boolean filterRecord(String lastModifiedString, LocalDateTime ingestedLastModifiedDate) {
        return ingestedLastModifiedDate == null || isHeaderTimeGreater(lastModifiedString, ingestedLastModifiedDate);
    }

    private static boolean isHeaderTimeGreater(String recordLastModified, LocalDateTime lastModifiedDate) {
        try {
            var currentHeaderLastModified = TimeUtility.getLocalDateTime(recordLastModified);
            return currentHeaderLastModified.isAfter(lastModifiedDate);
        } catch (DateNotParsedException e) {
            log.warn("Could not parse lastModifiedDate. Filtering out from list: {}", e.toString());
            return false;
        }
    }

    /**
     * Queries the remote repository for records.
     *
     * @param repo             the repository to query.
     * @return a map of records retrieved from the remote repository.
     */
    @SuppressWarnings("UnstableApiUsage")
    public Map<String, List<CMMStudyOfLanguage>> getRecords(Repo repo) {
        log.debug("[{}] Parsing record headers.", repo.getCode());

        /*
         * Repositories are indexed from their path. Because previous versions of the indexer supported
         * harvesting using URLs, we still need to check that a path is defined.
         */

        if (repo.getPath() == null) {
            throw new IllegalArgumentException("Repo " + repo.getCode() + " has no path defined");
        }

        try (var stream = Files.find(repo.getPath(), 1, (path, attributes) ->
            // Find XML files in the source directory.
            attributes.isRegularFile() && getFileExtension(path.toString()).equals("xml")
        )) {
            // Find XML files in the source directory.
            var studies = new AtomicInteger();

            var studiesByLanguage = stream.flatMap(path -> getRecord(repo, path).stream())
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
        } catch (IOException e) {
            log.error(LIST_RECORD_HEADERS_FAILED_WITH_MESSAGE,
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value(LoggingConstants.EXCEPTION_NAME, e.getClass().getName()),
                value(LoggingConstants.REASON, e.getMessage())
            );
        }
        return Collections.emptyMap();
    }

    /**
     * Retrieve a record from a path.
     * @param repo the repository that the record originated from
     * @param path the path to the record.
     * @return an {@link Optional} containing a {@link CMMStudy} or an empty optional if an error occurred.
     */
    List<CMMStudy> getRecord(Repo repo, Path path) {
        try {
            return recordXMLParser.getRecord(repo, path);
        } catch (XMLParseException e) {
            log.warn(FAILED_TO_GET_STUDY_ID_WITH_MESSAGE,
                value(LoggingConstants.REPO_NAME, repo.getCode()),
                value(LoggingConstants.STUDY_ID, path),
                value(LoggingConstants.EXCEPTION_NAME, e.getClass().getName()),
                value(LoggingConstants.REASON, e.getMessage())
            );
        }
        return Collections.emptyList();
    }
}
