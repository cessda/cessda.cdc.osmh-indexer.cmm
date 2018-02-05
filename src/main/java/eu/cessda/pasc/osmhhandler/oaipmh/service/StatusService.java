package eu.cessda.pasc.osmhhandler.oaipmh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.PaSCHandlerOaiPmhConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

/**
 * Diagnostics Managed Resource Service class.
 *
 * @author moses@doraventures.com
 */
@Service
@ManagedResource
@Slf4j
public class StatusService {

  @Autowired
  PaSCHandlerOaiPmhConfig paSCHandlerOaiPmhConfig;

  @Autowired
  ObjectMapper objectMapper;

  @ManagedOperation(description = "prints out the PaSC Handler OaiPmh ddi 2.5 Configurations.")
  public String printPaSCHandlerOaiPmhConfig() throws JsonProcessingException {
    ObjectWriter prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();
    log.info("Config for Rest: {}", prettyPrinter.writeValueAsString(paSCHandlerOaiPmhConfig.getRestTemplateProps()));
    log.info("Config for DDI 2.5: {}", prettyPrinter.writeValueAsString(paSCHandlerOaiPmhConfig.getOaiPmh()));
    return "See logs.";
  }
}
