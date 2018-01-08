package eu.cessda.pasc.osmhhandler.oaipmh.configuration;

import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Configurations loader tests
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest()
public class PaSCHandlerOaiPmhConfigTest {

  @Autowired
  PaSCHandlerOaiPmhConfig paSCHandlerOaiPmhConfig;

  @Test
  public void shouldReturnConfigurationsForOSMHHandler() {

    OaiPmh oaiPmh = paSCHandlerOaiPmhConfig.getOaiPmh();

    then(oaiPmh).isNotNull();
    then(oaiPmh.getSupportedApiVersions()).hasSize(1);
    then(oaiPmh.getSupportedApiVersions()).contains("v0");
    then(oaiPmh.getSupportedRecordTypes()).hasSize(5);

    then(oaiPmh.getRepos()).isNotNull();
    then(oaiPmh.getRepos()).hasSize(2);
    then(oaiPmh.getRepos().get(0).getUrl()).isEqualTo("www.test-repo.com");
    then(oaiPmh.getRepos().get(0).getPreferredMetadataVersion()).isEqualTo("2.5");
  }
}
