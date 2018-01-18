package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import org.springframework.stereotype.Service;

/**
 *
 * @author moses@doraventures.com
 */
@Service
public interface GetRecordService {
  CMMStudy getRecord(String repository, String studyId);
}
