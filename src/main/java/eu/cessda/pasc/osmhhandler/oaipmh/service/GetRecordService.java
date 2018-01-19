package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;

/**
 *
 * @author moses@doraventures.com
 */
public interface GetRecordService {
  CMMStudy getRecord(String repository, String studyId) throws InternalSystemException;
}
