package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendGetRecordParams;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.decodeStudyNumber;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Test class for {@link OaiPmhHelpers}
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class OaiPmhHelpersTest {


  @Autowired
  private HandlerConfigurationProperties handlerConfigurationProperties;

  @Test
  public void ShouldAppendMetaDataPrefixForGivenFSD() throws CustomHandlerException {

    // Given
    String fsdEndpoint = "http://services.fsd.uta.fi/v0/oai";
    String expectedReqUrl = "http://services.fsd.uta.fi/v0/oai?verb=GetRecord&identifier=15454&metadataPrefix=oai_ddi25";

    // When
    String builtUrl = appendGetRecordParams(fsdEndpoint, "15454", handlerConfigurationProperties.getOaiPmh());

    then(builtUrl).isEqualTo(expectedReqUrl);
  }

  @Test
  public void ShouldAppendMetaDataPrefixForGivenUKDS() throws CustomHandlerException {

    // Given
    String fsdEndpoint = "https://oai.ukdataservice.ac.uk:8443/oai/provider";
    String expectedReqUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=15454&metadataPrefix=ddi";

    // When
    String builtUrl = appendGetRecordParams(fsdEndpoint, "15454", handlerConfigurationProperties.getOaiPmh());

    then(builtUrl).isEqualTo(expectedReqUrl);
  }

  @Test(expected = CustomHandlerException.class)
  public void ShouldThrowExceptionForANonConfiguredRepo() throws CustomHandlerException {
    // When
    appendGetRecordParams("http://services.inthe.future/v0/oai", "15454", handlerConfigurationProperties.getOaiPmh());
  }

  @Test
  public void shouldDecodeStudyNumberSpecialCharactersBackToOriginalForm() {

    // Given
    String studyNumberWithRestCharactersEncoded = "oai_cl_dbk_dt_gesis_dt_org_cl_DBK_sl_ZA0001";

    // When
    String result = decodeStudyNumber(studyNumberWithRestCharactersEncoded);

    then(result).isEqualTo("oai:dbk.gesis.org:DBK/ZA0001");
  }
}