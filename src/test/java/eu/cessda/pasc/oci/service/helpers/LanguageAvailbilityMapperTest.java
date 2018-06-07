package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.data.RecordTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.BDDMockito.then;


/**
 * TODO: add description here
 *
 * @author moses@doraventures.com
 */
public class LanguageAvailbilityMapperTest {


  @Test
  public void shouldTagAllLangsThatPassMinimumCMMRequirementAsStudyAvailableInGivenLang() throws IOException {

    // Given
    // use synthetic_compliant_record.json via RecordTestData.getSyntheticCmmStudy()


    // When
    CMMStudy cmmStudy= LanguageAvailbilityMapper.applyAvaiableLangs(RecordTestData.getSyntheticCmmStudy());

    //TODO: then should hava "en", "fi" and "de"
    then(cmmStudy.isAvailableIn).contains("en", "fi", "de");
  }
}