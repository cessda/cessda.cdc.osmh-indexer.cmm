package eu.cessda.pasc.oci.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * This is a temp Solution whilst changes are being made to the Harvester (Node JS app)
 * <p>
 * Acts like as a light weight OSMH Harvester:
 * <ul>
 * <li>Takes takes in a string url representing a sp repo and returns the actual handler url for that repo</li>
 * <li> Does not do anything fancy</li>
 * <li> </li>
 * </ul>
 * <p>
 * FIXME: Remove this fake and call the Harvester directly with the repo url
 *
 * @author moses@doraventures.com
 */
@Component
public class FakeHarvester {

  private AppConfigurationProperties appConfigurationProperties;

  @Autowired
  public FakeHarvester(AppConfigurationProperties appConfigurationProperties) {
    this.appConfigurationProperties = appConfigurationProperties;
  }

  public Optional<Repo> getRepoConfigurationProperties(String repositoryUrl) {
    return appConfigurationProperties.getEndpoints().getRepos()
        .stream()
        .filter(repo -> repo.getUrl().equalsIgnoreCase(repositoryUrl))
        .findFirst();
  }
}
