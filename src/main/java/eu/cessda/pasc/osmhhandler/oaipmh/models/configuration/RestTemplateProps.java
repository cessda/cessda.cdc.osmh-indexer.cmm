package eu.cessda.pasc.osmhhandler.oaipmh.models.configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * RestTemplate Properties configuration model
 *
 * @author moses@doraventures.com
 */
@Getter
@Setter
public class RestTemplateProps {

  private int connTimeout;
  private int connRequestTimeout;
  private int readTimeout;
}
