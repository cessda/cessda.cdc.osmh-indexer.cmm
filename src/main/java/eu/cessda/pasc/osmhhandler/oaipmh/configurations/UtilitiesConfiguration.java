package eu.cessda.pasc.osmhhandler.oaipmh.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Extra Util configuration
 *
 * @author moses@doraventures.com
 */
@Configuration
public class UtilitiesConfiguration {

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}