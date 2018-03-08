package eu.cessda.pasc.oci.configurations;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Configurations loader tests
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
public class AppConfigurationPropertiesTest extends AbstractSpringTestProfileContext{

  @Autowired
  AppConfigurationProperties appConfigurationProperties;

  @Test
  public void shouldReturnConfigurationsForOSMHHandler() {

    then(appConfigurationProperties.getHarvester().getUrl()).isEqualTo("http://localhost:9091");
    then(appConfigurationProperties.getHarvester().getVersion()).isEqualTo("v0");

    List<Repo> repos = appConfigurationProperties.getEndpoints().getRepos();
    then(repos).isNotNull();
    then(repos).hasSize(4);

    assertThat(repos).extracting("name")
        .contains("UK Data Service", "Finish Data Services", "GESIS", "GESIS"
        ).doesNotContain("othertest");

    assertThat(repos).extracting("url")
        .contains(
            "https://oai.ukdataservice.ac.uk:8443/oai/provider",
            "http://services.fsd.uta.fi/v0/oai",
            "https://dbk.gesis.org/dbkoai",
            "https://dbk.gesis.org/dbkoai/"
        ).doesNotContain(
        "http://services/othertest"
    );
  }
}