package eu.cessda.pasc.oci.repository;

import org.junit.Test;

import static eu.cessda.pasc.oci.repository.StudyIdentifierEncoder.encodeStudyIdentifier;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses@doraventures.com
 */
public class StudyIdentifierEncoderTest {

  @Test
  public void shouldReplaceAllSpecialCharacters() {

    // Given
    String expectedEncodedId = "oai_cl_dbk_dt_gesis_dt_org_cl_DBK_sl_ZA0001";
    String identifier = "oai:dbk.gesis.org:DBK/ZA0001";

    // When
    String encodedId = encodeStudyIdentifier().apply(identifier);

    then(encodedId).isEqualTo(expectedEncodedId);
  }

  @Test
  public void printEncoding() {

    // Given
    String identifier = "http://nesstar.ucd.ie:80/obj/fStudy/PVTYV1-Anon";

    // When
    String encodedId = encodeStudyIdentifier().apply(identifier);
    System.out.println(encodedId);
  }
}
