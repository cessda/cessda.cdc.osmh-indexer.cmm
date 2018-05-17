package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.data.RecordTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static eu.cessda.pasc.oci.data.RecordTestData.getASingleSyntheticCMMStudyAsList;
import static eu.cessda.pasc.oci.data.RecordTestData.getSyntheticCMMStudyAndADeletedRecordAsList;
import static org.assertj.core.api.Java6BDDAssertions.then;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
public class LanguageDocumentExtractorTest extends AbstractSpringTestProfileContext {

  @Autowired
  LanguageDocumentExtractor languageDocumentExtractor;
  private String idPrefix = "test-stub";

  @Test
  public void shouldRejectRecordsWhenMissingTitle() throws IOException {

    // Given
    CMMStudy study = RecordTestData.getSyntheticCmmStudy().map(cmmStudyValue -> {
      cmmStudyValue.getTitleStudy().remove("en");
      return cmmStudyValue;
    }).orElse(null);

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, study);

    then(validCMMStudyForLang).isFalse();
  }

  @Test
  public void shouldRejectRecordsWhenMissingAbstract() throws IOException {
    // Given
    CMMStudy study = RecordTestData.getSyntheticCmmStudy().map(cmmStudyValue -> {
      cmmStudyValue.getAbstractField().remove("en");
      return cmmStudyValue;
    }).orElse(null);

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, study);
    then(validCMMStudyForLang).isFalse();
  }

  @Test
  public void shouldRejectRecordsWhenMissingStudyNumber() throws IOException {
    // When Study Number is empty------------------------------------------------------------------------------/
    CMMStudy study = RecordTestData.getSyntheticCmmStudy().map(cmmStudyValue -> {
      cmmStudyValue.setStudyNumber("");
      return cmmStudyValue;
    }).orElse(null);

    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, study);
    then(validCMMStudyForLang).isFalse();

    // When Study Number is null ------------------------------------------------------------------------------/
    study = RecordTestData.getSyntheticCmmStudy().map(cmmStudyValue -> {
      cmmStudyValue.setStudyNumber(null);
      return cmmStudyValue;
    }).orElse(null);

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, study);
    then(validCMMStudyForLang).isFalse();
  }

  @Test
  public void shouldRejectRecordsWhenMissingPublisher() throws IOException {

    // When Study Url is Miss ------------------------------------------------------------------------------/
    CMMStudy study = RecordTestData.getSyntheticCmmStudy().map(cmmStudyValue -> {
      cmmStudyValue.getPublisher().remove("en");
      return cmmStudyValue;
    }).orElse(null);

    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, study);
    then(validCMMStudyForLang).isFalse();
  }

  @Test
  public void shouldValidateRecordsThatHaveTheMinimumCMMFields() throws IOException {

    // Given
    CMMStudy cmmStudy = RecordTestData.getSyntheticCmmStudy().orElse(null);

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("de", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fi", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("sv", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isFalse(); // we do not have the required abstract translation in "sv"

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fr", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isFalse(); // we have nothing for "fr"
  }

  @Test
  public void shouldValidateRecordsThatAreMarkedAsInactiveByPassingTheMinimumCMMFieldsChecks() throws IOException {

    // Given
    Optional<CMMStudy> cmmStudy = RecordTestData.getSyntheticCmmStudy();
    CMMStudy inActiveCmmStudy = cmmStudy.map(cmmStudy1 -> {
          cmmStudy1.setActive(false);
          return cmmStudy1;
        }
    ).orElse(null);

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, inActiveCmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("de", idPrefix, inActiveCmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fi", idPrefix, inActiveCmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("sv", idPrefix, inActiveCmmStudy);
    then(validCMMStudyForLang).isTrue(); // Though, we do not have the required abstract translation in "sv".

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fr", idPrefix, inActiveCmmStudy);
    then(validCMMStudyForLang).isTrue(); // We have nothing for "fr", when a record is deleted we lose this knowledge.
  }

  @Test
  public void shouldReturnExtractedDocInThereRespectiveLangDocuments() {

    // Given
    List<Optional<CMMStudy>> studies = getASingleSyntheticCMMStudyAsList();

    // When
    Map<String, List<CMMStudyOfLanguage>> languageDocMap =
        languageDocumentExtractor.mapLanguageDoc(studies, "UK Data Service");

    then(languageDocMap).isNotNull();
    then(languageDocMap).hasSize(4);
    then(languageDocMap).containsOnlyKeys("en", "fi", "sv", "de");
    then(languageDocMap.get("en")).hasSize(1);
    then(languageDocMap.get("fi")).hasSize(1);
    then(languageDocMap.get("sv")).hasSize(0); //Synthetic doc does not have a studyTitle or abstract, so sv is skipped
    then(languageDocMap.get("de")).hasSize(1);

    List<CMMStudyOfLanguage> enStudy = languageDocMap.get("en");
    Optional<String> enCMMStudyJsonStringOpt = CMMStudyOfLanguageConverter.toJsonString(enStudy.get(0));
    enCMMStudyJsonStringOpt.ifPresent(System.out::println);
  }

  @Test
  public void shouldReturnExtractedDocInTheirRespectiveLangDocumentsIncludingDeletedRecordsMarkedAsInActive() {

    // Given
    List<Optional<CMMStudy>> studies = getSyntheticCMMStudyAndADeletedRecordAsList();

    // When
    Map<String, List<CMMStudyOfLanguage>> languageDocMap =
        languageDocumentExtractor.mapLanguageDoc(studies, "UK Data Service");

    then(languageDocMap).isNotNull();
    then(languageDocMap).hasSize(4);
    then(languageDocMap).containsOnlyKeys("en", "fi", "sv", "de");
    then(languageDocMap.get("en")).hasSize(2); // a deleted record and an active record that is valid
    then(languageDocMap.get("fi")).hasSize(2); // a deleted record and an active record that is valid
    then(languageDocMap.get("sv")).hasSize(1); // a deleted record and an active record that is not valid for lang sv
    then(languageDocMap.get("de")).hasSize(2); // a deleted record and an active record that is valid

    List<CMMStudyOfLanguage> enStudy = languageDocMap.get("en");
    System.out.println("Printing Records");
    enStudy.forEach(cmmStudyOfLanguage -> {
          Optional<String> enCMMStudyJsonStringOpt = CMMStudyOfLanguageConverter.toJsonString(cmmStudyOfLanguage);
          enCMMStudyJsonStringOpt.ifPresent(System.out::println);
        }
    );
  }
}
