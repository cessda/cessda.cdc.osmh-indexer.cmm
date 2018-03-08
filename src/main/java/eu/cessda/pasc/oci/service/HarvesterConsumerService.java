package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for consuming harvested and Metadata via OSMH Harvester
 *
 * @author moses@doraventures.com
 */
public interface HarvesterConsumerService {
  List<RecordHeader> listRecordHeaders(Repo repo);
  Optional<CMMStudy> getRecord(Repo repo, String StudyNumber);
}