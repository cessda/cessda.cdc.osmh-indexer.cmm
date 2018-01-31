package eu.cessda.pasc.osmh.indexer.configurations;

import eu.cessda.pasc.osmh.indexer.models.config.Repo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
@SpringBootTest()
@ActiveProfiles("test")
public class PascOsmhIndexerConfigTest {

  @Autowired
  PascOsmhIndexerConfig pascOsmhIndexerConfig;

  @Test
  public void shouldReturnConfigurationsForOSMHHandler() throws Exception {

    then(pascOsmhIndexerConfig.getOsmhHarvesterUrl()).isEqualTo("http://localhost:9091/v0/");
    then(pascOsmhIndexerConfig.getIncrementalPeriod()).isEqualTo("0 0 */8 * * *");

    List<Repo> repos = pascOsmhIndexerConfig.getRepos();
    then(repos).isNotNull();
    then(repos).hasSize(2);

    assertThat(repos).extracting("serviceProviderName").contains(
        "UKDS"
        , "FSD"
    ).doesNotContain(
        "othertest"
    );

    assertThat(repos).extracting("url").contains(
        "https://oai.ukdataservice.ac.uk:8443/oai/provider"
        , "http://services.fsd.uta.fi/v0/oai"
    ).doesNotContain(
        "http://services/othertest"
    );
  }
}