package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;

/**
 * Data access for fetching Record from remote repositories
 *
 * @author moses@doraventures.com
 */
public interface GetRecordDoa {

  String getRecordXML(String repoUrl, String studyIdentifier) throws ExternalSystemException;
}
