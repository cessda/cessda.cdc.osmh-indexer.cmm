package eu.cessda.pasc.oci.models.configurations;

import lombok.Getter;
import lombok.Setter;

/**
 * Repo configuration model
 *
 * @author moses@doraventures.com
 */
@Getter
@Setter
public class Repo {

  private String url;
  private String name;
  private String handler;
}
