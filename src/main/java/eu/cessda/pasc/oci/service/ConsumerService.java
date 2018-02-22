package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;

import java.util.List;

/**
 * Service responsible for consuming harvested and Metadata via OSMH Harvester
 *
 * @author moses@doraventures.com
 */
public interface ConsumerService {
  List<RecordHeader> listRecorderHeadersBody(Repo repo);
}
