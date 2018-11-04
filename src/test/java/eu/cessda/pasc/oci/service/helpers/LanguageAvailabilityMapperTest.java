package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.data.RecordTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
public class LanguageAvailabilityMapperTest extends AbstractSpringTestProfileContext {

  @Autowired
  private LanguageAvailabilityMapper languageAvailabilityMapper;

  @Test
  public void shouldTagAllLanguagesThatPassMinimumCMMRequirementAsStudyAvailableInGivenLang() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();

    // When
    languageAvailabilityMapper.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).containsExactlyInAnyOrder("en", "fi", "de");
  }

  @Test
  public void shouldNotTagAnyLangWhenCMMStudyDoesNotHaveTheRequiredCMMStudyIdentifier() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();
    cmmStudyWithNoAvailableLangSet.setStudyNumber(null);

    // When
    languageAvailabilityMapper.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).isEmpty();
  }

  @Test
  public void shouldNotTagEnglishWhenCMMStudyDoesNotHaveTheRequiredCMMStudyTitleForEnglish() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();
    cmmStudyWithNoAvailableLangSet.getTitleStudy().remove("en");

    // When
    languageAvailabilityMapper.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).doesNotContain("en");
    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).containsExactlyInAnyOrder("fi", "de");
  }

  @Test
  public void shouldNotTagFinishWhenCMMStudyDoesNotHaveTheRequiredCMMStudyAbstractForFinish() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();
    cmmStudyWithNoAvailableLangSet.getAbstractField().remove("fi");

    // When
    languageAvailabilityMapper.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).doesNotContain("fi");
    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).containsExactlyInAnyOrder("en", "de");

  }

  @Test
  public void shouldNotTagGermanWhenCMMStudyDoesNotHaveTheRequiredCMMStudyPublisherForGerman() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();
    cmmStudyWithNoAvailableLangSet.getPublisher().remove("de");

    // When
    languageAvailabilityMapper.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).doesNotContain("de");
    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).containsExactlyInAnyOrder("en", "fi");
  }
}