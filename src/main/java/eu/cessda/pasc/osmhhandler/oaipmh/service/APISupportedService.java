package eu.cessda.pasc.osmhhandler.oaipmh.service;

import java.util.List;

/**
 *  For API specific supported attributes
 *
 * @author moses@doraventures.com
 */
public interface APISupportedService {

  List<String> getSupportedVersion();

  boolean isSupportedVersion(String version);

  List<String> getSupportedRecordTypes();
}
