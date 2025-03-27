/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.elasticsearch;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Service interface contract for data ingestion
 *
 * @author moses AT doraventures DOT com
 */
public interface IngestService {

    /**
     * Bulk indices records into the search Engine.
     *
     * @param languageCMMStudiesMap of records
     * @param languageIsoCode       index post-end token
     * @throws IndexingException if an error occurs when indexing the studies to Elasticsearch.
     */
    void bulkIndex(Collection<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode) throws IndexingException;

    /**
     * Delete the specified studies from all indices.
     * <p>
     * No language option is needed as deletions occur at the study level, not per language.
     *
     * @param cmmStudiesToDelete the collection of studies to delete
     * @param languageIsoCode    the language of the index to delete the studies from
     * @throws IndexingException if an error occurs connecting to Elasticsearch.
     */
    void bulkDelete(Collection<CMMStudyOfLanguage> cmmStudiesToDelete, String languageIsoCode) throws IndexingException;

    /**
     * Gets the total number of hits for the specified language. The language is in the same form as languages configured
     * in application.yml.
     *
     * @param language the language to get the total hits from. Use * to get the hits for all languages.
     * @throws ElasticsearchException if a corresponding index is not found.
     * @throws IOException            if an error occurs connecting to Elasticsearch.
     */
    long getTotalHitCount(String language) throws IOException;

    /**
     * Gets a set of all studies stored in Elasticsearch.
     *
     * @param language the language to get studies from. Use * to get all studies.
     * @return a set containing all studies
     */
    Set<CMMStudyOfLanguage> getAllStudies(String language);

    /**
     * Gets a set of all studies from a specific repository for a given language.
     *
     * @param repository the code of the repository.
     * @param language   the language of the index to search in. Use * to search all indexes.
     */
    Set<CMMStudyOfLanguage> getStudiesByRepository(String repository, String language);

    /**
     * Gets a study with a specific ID.
     *
     * @param id       the id of the study to get.
     * @param language the language of the index to search in.
     * @return an optional containing the study, or an empty optional if the study can't be found
     * or if an error occurs retrieving the study.
     */
    Optional<CMMStudyOfLanguage> getStudy(String id, String language);

    /**
     * Gets the most recent lastModified date from the cluster across all {@code cmmstudy} indices.
     *
     * @return LocalDateTime. The exact most recent lastModified dateTime from the cluster for the index pattern.
     */
    Optional<LocalDate> getMostRecentLastModified();

    /**
     * Perform reindexing for all themes and their respective reindex queries.
     * Loops through all theme directories and runs reindex queries for each one.
     */
    void reindexAllThemes() throws IndexingException;

    /**
     * Perform reindexing from a source index to a destination index using a query.
     *
     * @param sourceIndex      the source index name
     * @param destinationIndex the destination index name
     * @param queryJsonFilePath the file path to the reindex query
     * @throws IndexingException if an error occurs during the reindexing process
     */
    void reindex(String sourceIndex, String destinationIndex, String queryJsonFilePath) throws IndexingException;
}
