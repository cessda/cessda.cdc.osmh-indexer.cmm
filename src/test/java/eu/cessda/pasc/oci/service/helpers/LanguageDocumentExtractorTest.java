package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static eu.cessda.pasc.oci.data.RecordTestData.getASingleSyntheticCMMStudyAsList;
import static org.assertj.core.api.Java6BDDAssertions.then;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class LanguageDocumentExtractorTest {

  @Autowired
  LanguageDocumentExtractor languageDocumentExtractor;

  @Test
  public void shouldReturnMapOfExtractedDocInThereRespectiveLangDocuments() {

    // Given
    List<CMMStudy> studies = getASingleSyntheticCMMStudyAsList();

    // When
    Map<String, List<CMMStudyOfLanguage>> languageDocMap =
        languageDocumentExtractor.mapLanguageDoc(studies, "UK Data Service");

    then(languageDocMap).isNotNull();
    then(languageDocMap).hasSize(4);
    then(languageDocMap).containsOnlyKeys("en", "fi", "sv", "de");
    then(languageDocMap.get("en")).hasSize(1);
    then(languageDocMap.get("fi")).hasSize(1);
    then(languageDocMap.get("sv")).hasSize(1);
    then(languageDocMap.get("de")).hasSize(1);

    List<CMMStudyOfLanguage> enStudy = languageDocMap.get("en");
    Optional<String> s = CMMStudyOfLanguageConverter.toJsonString(enStudy.get(0));
    s.ifPresent(System.out::println);
  }
}