package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;

/**
 * Data access object Contract for querying remote repository for RecordHeaders
 *
 * @author moses@doraventures.com
 */
public interface ListRecordHeadersDao {

  String listRecordHeaders(String baseRepoUrl) throws ExternalSystemException;

  String listRecordHeadersResumption(String baseRepoUrl) throws ExternalSystemException;
}
