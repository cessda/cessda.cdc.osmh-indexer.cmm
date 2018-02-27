package eu.cessda.pasc.oci.models.configurations;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Endpoints configuration model
 *
 * @author moses@doraventures.com
 */
@Getter
@Setter
public class Endpoints {

  private List<String> supportedApiVersions;
  private List<String> supportedRecordTypes;
  private List<Repo> repos;
}
