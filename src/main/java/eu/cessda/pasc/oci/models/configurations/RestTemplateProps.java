package eu.cessda.pasc.oci.models.configurations;

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
  private boolean verifySSL;
}
