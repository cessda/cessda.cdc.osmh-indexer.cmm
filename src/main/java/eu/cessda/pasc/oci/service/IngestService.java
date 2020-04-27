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
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
   * @return true If bulkIndexing was successful with no known error.
   */
  boolean bulkIndex(List<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode);


  /**
   * Gets the total number of hits for the specified language. The language is in the same form as languages configured
   * in application.yml.
   *
   * @param language the language to get the total hits from. Use * to get the hits for all languages.
   * @throws org.elasticsearch.index.IndexNotFoundException if a corresponding index is not found.
   * @throws org.elasticsearch.ElasticsearchException       if an error occurs connecting to Elasticsearch.
   */
  long getTotalHitCount(String language);

  /**
   * Gets a set of all studies stored in Elasticsearch.
   *
   * @param language the language to get studies from. Use * to get all studies.
   * @return a map containing all studies with the key set to the study ID.
   */
  Map<String, CMMStudyOfLanguage> getAllStudies(String language);

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
   * Gets the most recent lastModified date from the cluster across all indices eg pattern (cmmstudy_*)
   * <p>
   * Ingestion to indices can range between minutes to 6hrs meaning this dateTime stamp returned
   * might be off in minutes/hours for some indices therefore this timeStamp should be adjusted according before use.
   *
   * @return LocalDateTime. The exact most recent lastModified dateTime from the cluster for the index pattern.
   */
  Optional<LocalDateTime> getMostRecentLastModified();
}
