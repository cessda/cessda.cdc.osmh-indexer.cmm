package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;

/**
 * The Open Source Metadata Harvester service.   For handling remote calls to the remote harvester
 *
 * @author moses@doraventures.com
 */
public interface HarvesterDao {
  String listRecordHeaders(String spRepository) throws ExternalSystemException;
  String getRecord(String spRepository, String studyNumber) throws ExternalSystemException;
}
