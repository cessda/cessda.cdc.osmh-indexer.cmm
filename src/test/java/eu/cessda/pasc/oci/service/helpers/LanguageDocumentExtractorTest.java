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
  public void shouldRejectRecordWhenNotInListOfLanguagesAvailableIn() throws IOException {

    // Given
    CMMStudy syntheticCmmStudy = RecordTestData.getSyntheticCmmStudy();
    syntheticCmmStudy.getLangAvailableIn().remove("en");

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, syntheticCmmStudy);

    then(validCMMStudyForLang).isFalse();
  }

  @Test
  public void shouldValidateRecordsWhenGivenLanguageCodeIsInListOfLanguagesAvailableIn() throws IOException {

    // Given
    CMMStudy cmmStudy = RecordTestData.getSyntheticCmmStudy();

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("de", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fi", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isTrue();
    
    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fr", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isTrue();
    
    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("nl", idPrefix, cmmStudy);
     then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("se", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isFalse(); 

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("sk", idPrefix, cmmStudy);
    then(validCMMStudyForLang).isFalse(); 
  }

  @Test
  public void shouldValidateRecordsThatAreMarkedAsInactiveByPassingTheMinimumCMMFieldsChecks() throws IOException {

    // Given
    CMMStudy syntheticCmmStudy = RecordTestData.getSyntheticCmmStudy();
    syntheticCmmStudy.setActive(false);

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("de", idPrefix, syntheticCmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", idPrefix, syntheticCmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fi", idPrefix, syntheticCmmStudy);
    then(validCMMStudyForLang).isTrue();

    //Synthetic doc does not exist, so language is skipped
    //validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fr", idPrefix, syntheticCmmStudy);
    //then(validCMMStudyForLang).isTrue();

   //validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("nl", idPrefix, syntheticCmmStudy);
    //then(validCMMStudyForLang).isTrue();

    //validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("se", idPrefix, syntheticCmmStudy);
    //then(validCMMStudyForLang).isTrue();

    //validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("sk", idPrefix, syntheticCmmStudy);
    //then(validCMMStudyForLang).isTrue();
  }

  @Test
  public void shouldReturnExtractedDocInThereRespectiveLangDocuments() {

    // Given
    List<CMMStudy> studies = getASingleSyntheticCMMStudyAsList();

    // When
    Map<String, List<CMMStudyOfLanguage>> languageDocMap =
        languageDocumentExtractor.mapLanguageDoc(studies, "UK Data Service");

    then(languageDocMap).isNotNull();
    then(languageDocMap).hasSize(7);
    then(languageDocMap).containsOnlyKeys("de", "en", "fi", "fr", "nl", "se", "sk");
    then(languageDocMap.get("de")).hasSize(1);
    then(languageDocMap.get("en")).hasSize(1);
    then(languageDocMap.get("fi")).hasSize(1);
    //Synthetic doc does not exist, or does have a studyTitle or abstract, so language is skipped
    //then(languageDocMap.get("fr")).hasSize(0); 
    //then(languageDocMap.get("nl")).hasSize(0); 
    //then(languageDocMap.get("se")).hasSize(0); 
    //then(languageDocMap.get("sk")).hasSize(0); 

    List<CMMStudyOfLanguage> enStudy = languageDocMap.get("en");
    Optional<String> enCMMStudyJsonStringOpt = CMMStudyOfLanguageConverter.toJsonString(enStudy.get(0));
    enCMMStudyJsonStringOpt.ifPresent(System.out::println);
  }

  @Test
  public void shouldReturnExtractedDocInTheirRespectiveLangDocumentsIncludingDeletedRecordsMarkedAsInActive() {

    // Given
    List<CMMStudy> studies = getSyntheticCMMStudyAndADeletedRecordAsList();

    // When
    Map<String, List<CMMStudyOfLanguage>> languageDocMap =
        languageDocumentExtractor.mapLanguageDoc(studies, "UK Data Service");

    then(languageDocMap).isNotNull();
    then(languageDocMap).hasSize(7);
    then(languageDocMap).containsOnlyKeys("de", "en", "fi", "fr", "nl", "se", "sk");
    then(languageDocMap.get("de")).hasSize(2); // a deleted record and an active record that is valid
    then(languageDocMap.get("en")).hasSize(2); // a deleted record and an active record that is valid
    then(languageDocMap.get("fi")).hasSize(2); // a deleted record and an active record that is valid
    //then(languageDocMap.get("sv")).hasSize(1); // a deleted record and an active record that is not valid for lang

    List<CMMStudyOfLanguage> enStudy = languageDocMap.get("en");
    System.out.println("Printing Records");
    enStudy.forEach(cmmStudyOfLanguage -> {
          Optional<String> enCMMStudyJsonStringOpt = CMMStudyOfLanguageConverter.toJsonString(cmmStudyOfLanguage);
          enCMMStudyJsonStringOpt.ifPresent(System.out::println);
        }
    );
  }
}
