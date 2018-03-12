package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

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