package eu.cessda.pasc.osmhhandler.oaipmh.configuration;

import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Configurations loader tests
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles("test")
public class HandlerConfigurationPropertiesTest {

  @Autowired
  HandlerConfigurationProperties handlerConfigurationProperties;

  @Test
  public void shouldReturnConfigurationsForOSMHHandler() {

    OaiPmh oaiPmh = handlerConfigurationProperties.getOaiPmh();

    then(oaiPmh).isNotNull();

    then(oaiPmh.getSupportedApiVersions()).hasSize(1);
    then(oaiPmh.getSupportedApiVersions()).contains("v0");
    then(oaiPmh.getSupportedRecordTypes()).hasSize(5);

    then(oaiPmh.getRepos()).isNotNull();
    then(oaiPmh.getRepos()).isNotEmpty();
    then(oaiPmh.getRepos().get(0).getUrl()).isEqualTo("https://data2.aussda.at/oai/");
    then(oaiPmh.getRepos().get(0).getPreferredMetadataParam()).isEqualTo("oai_ddi");
    then(oaiPmh.getRepos().get(1).getUrl()).isEqualTo("https://easy.dans.knaw.nl/oai/");
    then(oaiPmh.getRepos().get(1).getPreferredMetadataParam()).isEqualTo("oai_ddi25_nl");
    then(oaiPmh.getRepos().get(2).getUrl()).isEqualTo("https://easy.dans.knaw.nl/oai/");
    then(oaiPmh.getRepos().get(2).getPreferredMetadataParam()).isEqualTo("oai_ddi25_en");
    then(oaiPmh.getRepos().get(3).getUrl()).isEqualTo("http://services.fsd.uta.fi/v0/oai");
    then(oaiPmh.getRepos().get(3).getPreferredMetadataParam()).isEqualTo("oai_ddi25");
    then(oaiPmh.getRepos().get(4).getUrl()).isEqualTo("https://dbk.gesis.org/dbkoai3/");
    then(oaiPmh.getRepos().get(4).getPreferredMetadataParam()).isEqualTo("oai_ddi25");
    then(oaiPmh.getRepos().get(5).getUrl()).isEqualTo("http://147.213.4.44:6003/v0/oai");
    then(oaiPmh.getRepos().get(5).getPreferredMetadataParam()).isEqualTo("oai_ddi25");
    then(oaiPmh.getRepos().get(6).getUrl()).isEqualTo("https://snd.gu.se/en/oai-pmh");
    then(oaiPmh.getRepos().get(6).getPreferredMetadataParam()).isEqualTo("oai_ddi25");
    then(oaiPmh.getRepos().get(7).getUrl()).isEqualTo("https://oai.ukdataservice.ac.uk:8443/oai/provider");
    then(oaiPmh.getRepos().get(7).getPreferredMetadataParam()).isEqualTo("ddi");
  }
}
