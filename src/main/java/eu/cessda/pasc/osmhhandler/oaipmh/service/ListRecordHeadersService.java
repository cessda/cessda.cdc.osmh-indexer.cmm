package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.response.RecordHeader;

import java.util.List;

/**
 * Service interface contract to handle Listing Record Headers
 *
 * @author moses@doraventures.com
 */
public interface ListRecordHeadersService {
  List<RecordHeader> getRecordHeaders(String baseRepoUrl) throws CustomHandlerException;
}
