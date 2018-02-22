package eu.cessda.pasc.oci.dao;

import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;

/**
 * The Open Source Metadata Harvester service.   For handling remote calls to the remote harvester
 *
 * @author moses@doraventures.com
 */
public interface HarvesterDao {
  String listRecordHeaders(String repository) throws ExternalSystemException;
}
