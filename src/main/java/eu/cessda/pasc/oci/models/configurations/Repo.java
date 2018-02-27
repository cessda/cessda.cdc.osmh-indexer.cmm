package eu.cessda.pasc.oci.models.configurations;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Repo configuration model
 *
 * @author moses@doraventures.com
 */
@Getter
@Setter
@ToString
public class Repo {

  private String url;
  private String name;
  private String handler;
}
