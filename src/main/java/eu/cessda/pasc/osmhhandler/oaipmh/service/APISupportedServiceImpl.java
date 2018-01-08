package eu.cessda.pasc.osmhhandler.oaipmh.service;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author moses@doraventures.com
 */
@Service
public class APISupportedServiceImpl implements APISupportedService {

  @Autowired
  @Qualifier("PaSCHandlerOaiPmhConfig")
  PaSCHandlerOaiPmhConfig pmhConfig;

  public List<String> getSupportedVersion() {
    return pmhConfig.getOaiPmh().getSupportedApiVersions();
  }

  @Override
  public boolean isSupportedVersion(String version) {
    return getSupportedVersion().contains(version);
  }

  @Override
  public List<String> getSupportedRecordTypes() {
    return pmhConfig.getOaiPmh().getSupportedRecordTypes();
  }

}
