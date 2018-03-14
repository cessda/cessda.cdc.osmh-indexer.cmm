package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service interface contract for data ingestion
 *
 * @author moses@doraventures.com
 */
public interface IngestService {

  /**
   * Bulk indices records into the search Engine.
   *
   * @param languageCMMStudiesMap of records
   * @param languageIsoCode index post-end token
   * @return true If bulkIndexing was successful with no known error.
   */
  boolean bulkIndex(List<CMMStudyOfLanguage> languageCMMStudiesMap, String languageIsoCode);

  /**
   * Gets the most recent lastModified date from the cluster across all indices eg pattern (cmmstudy_*)
   * <p>
   * Ingestion to indices can range between minutes to 6hrs meaning this dateTime stamp returned
   * might be off in minutes/hours for some indices therefore this timeStamp should be adjusted according before use.
   *
   * @return LocalDateTime. The exact  most recent lastModified dateTime from the cluster for the indice pattern.
   */
  LocalDateTime getMostRecentLastModified();
}
