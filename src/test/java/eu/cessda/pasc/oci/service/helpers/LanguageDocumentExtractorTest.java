/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.service.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.mock.data.RecordTestData;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.getASingleSyntheticCMMStudyAsList;
import static eu.cessda.pasc.oci.mock.data.RecordTestData.getSyntheticCMMStudyAndADeletedRecordAsList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@Slf4j
public class LanguageDocumentExtractorTest extends AbstractSpringTestProfileContext {

  @Autowired
  private LanguageDocumentExtractor languageDocumentExtractor;

  @Autowired
  private CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter;

  private static final String ID_PREFIX = "test-stub";

  @Test
  public void shouldAcceptRecordWhenNotInListOfLanguagesIfCriteriaIsFulfilled() throws IOException {

    // Given
    CMMStudy syntheticCmmStudy = RecordTestData.getSyntheticCmmStudy();
    syntheticCmmStudy.getLangAvailableIn().remove("en");

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "en");

    then(validCMMStudyForLang).isTrue();
  }

  @Test
  public void shouldValidateRecordsWhenGivenLanguageCodeIsInListOfLanguagesAvailableIn() throws IOException {

    // Given
    CMMStudy cmmStudy = RecordTestData.getSyntheticCmmStudy();

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang(cmmStudy, "de");
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang(cmmStudy, "en");
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang(cmmStudy, "fi");
    then(validCMMStudyForLang).isTrue();

     //Synthetic doc does not exist, so language is skipped
    //validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fr", idPrefix, cmmStudy);
    //then(validCMMStudyForLang).isTrue();

   //validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("nl", idPrefix, cmmStudy);
     //then(validCMMStudyForLang).isTrue();

    //validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("se", idPrefix, cmmStudy);
    //then(validCMMStudyForLang).isFalse();

    //validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("sk", idPrefix, cmmStudy);
    //then(validCMMStudyForLang).isFalse();
  }

  @Test
  public void shouldValidateRecordsThatAreMarkedAsInactiveByPassingTheMinimumCMMFieldsChecks() throws IOException {

    // Given
    CMMStudy syntheticCmmStudy = RecordTestData.getSyntheticCmmStudy();
    syntheticCmmStudy.setActive(false);

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "de");
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "en");
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "fi");
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
  public void shouldReturnExtractedDocInThereRespectiveLangDocuments() throws JsonProcessingException {

    // Given
    List<CMMStudy> studies = getASingleSyntheticCMMStudyAsList();

    // When
    Map<String, List<CMMStudyOfLanguage>> languageDocMap = languageDocumentExtractor.mapLanguageDoc(studies, ReposTestData.getUKDSRepo());

    then(languageDocMap).isNotNull();
    then(languageDocMap).hasSize(17);
    then(languageDocMap).containsOnlyKeys("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");
    then(languageDocMap.get("de")).hasSize(1);
    then(languageDocMap.get("en")).hasSize(1);
    then(languageDocMap.get("fi")).hasSize(1);
    //Synthetic doc does not exist, or does have a studyTitle or abstract, so language is skipped
    //then(languageDocMap.get("fr")).hasSize(0);
    //then(languageDocMap.get("nl")).hasSize(0);
    //then(languageDocMap.get("se")).hasSize(0);
    //then(languageDocMap.get("sk")).hasSize(0);

    List<CMMStudyOfLanguage> enStudy = languageDocMap.get("en");
    String enCMMStudyJsonStringOpt = cmmStudyOfLanguageConverter.toJsonString(enStudy.get(0));
    System.out.println(enCMMStudyJsonStringOpt);
  }

  @Test
  public void shouldReturnExtractedDocInTheirRespectiveLangDocumentsIncludingDeletedRecordsMarkedAsInActive() throws JsonProcessingException {

    // Given
    List<CMMStudy> studies = getSyntheticCMMStudyAndADeletedRecordAsList();

    // When
    Map<String, List<CMMStudyOfLanguage>> languageDocMap = languageDocumentExtractor.mapLanguageDoc(studies, ReposTestData.getUKDSRepo());

    then(languageDocMap).isNotNull();
    then(languageDocMap).hasSize(17);
    then(languageDocMap).containsOnlyKeys("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");
    then(languageDocMap.get("de")).hasSize(2); // a deleted record and an active record that is valid
    then(languageDocMap.get("en")).hasSize(2); // a deleted record and an active record that is valid
    then(languageDocMap.get("fi")).hasSize(2); // a deleted record and an active record that is valid
    //then(languageDocMap.get("sv")).hasSize(1); // a deleted record and an active record that is not valid for lang

    List<CMMStudyOfLanguage> enStudy = languageDocMap.get("en");
    log.info("Printing Records");
    for (CMMStudyOfLanguage cmmStudyOfLanguage : enStudy) {
      String enCMMStudyJsonStringOpt = cmmStudyOfLanguageConverter.toJsonString(cmmStudyOfLanguage);
      log.info(enCMMStudyJsonStringOpt);
    }
  }

  @Test
  public void shouldTagAllLanguagesThatPassMinimumCMMRequirementAsStudyAvailableInGivenLang() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();

    // When
    languageDocumentExtractor.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).containsExactlyInAnyOrder("en", "fi", "de");
  }

  @Test
  public void shouldNotTagAnyLangWhenCMMStudyDoesNotHaveTheRequiredCMMStudyIdentifier() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();
    cmmStudyWithNoAvailableLangSet.setStudyNumber(null);

    // When
    languageDocumentExtractor.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).isEmpty();
  }

  @Test
  public void shouldNotTagEnglishWhenCMMStudyDoesNotHaveTheRequiredCMMStudyTitleForEnglish() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();
    cmmStudyWithNoAvailableLangSet.getTitleStudy().remove("en");

    // When
    languageDocumentExtractor.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).doesNotContain("en");
    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).containsExactlyInAnyOrder("fi", "de");
  }

  @Test
  public void shouldNotTagFinishWhenCMMStudyDoesNotHaveTheRequiredCMMStudyAbstractForFinish() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();
    cmmStudyWithNoAvailableLangSet.getAbstractField().remove("fi");

    // When
    languageDocumentExtractor.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).doesNotContain("fi");
    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).containsExactlyInAnyOrder("en", "de");

  }

  @Test
  public void shouldNotTagGermanWhenCMMStudyDoesNotHaveTheRequiredCMMStudyPublisherForGerman() throws IOException {

    // Given
    final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudyWithNoAvailableLangsSet();
    cmmStudyWithNoAvailableLangSet.getPublisher().remove("de");

    // When
    languageDocumentExtractor.setAvailableLanguages(cmmStudyWithNoAvailableLangSet);

    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).doesNotContain("de");
    assertThat(cmmStudyWithNoAvailableLangSet.getLangAvailableIn()).containsExactlyInAnyOrder("en", "fi");
  }
}
