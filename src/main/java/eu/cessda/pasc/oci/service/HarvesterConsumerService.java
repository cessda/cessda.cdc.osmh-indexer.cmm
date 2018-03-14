package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for consuming harvested and Metadata via OSMH Harvester
 *
 * @author moses@doraventures.com
 */
public interface HarvesterConsumerService {

  /**
   * Queries the remote repo for RecordHeaders.  RecordHeaders are filtered if lastModifiedDate is provided.
   *
   * @param repo repository details.
   * @param lastModifiedDate to filter headers on.
   * @return List RecordHeaders.
   */
  List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate);

  /**
   * Queries the remote repo for a Record.
   *
   * @param repo repository details.
   * @param studyNumber the remote Study Identifier.
   * @return Record instance.
   */
  Optional<CMMStudy> getRecord(Repo repo, String studyNumber);
}