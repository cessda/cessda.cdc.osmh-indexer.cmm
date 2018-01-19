package eu.cessda.pasc.osmhhandler.oaipmh.dao;

/**
 * Data access for fetching Record from remote repositories
 *
 * @author moses@doraventures.com
 */
public interface GetRecordDoa {

  String getRecordXML(String repoUrl, String studyIdentifier);
}
