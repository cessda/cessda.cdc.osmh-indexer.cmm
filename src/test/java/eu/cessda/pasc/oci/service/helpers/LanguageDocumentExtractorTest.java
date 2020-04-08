/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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

import static eu.cessda.pasc.oci.data.RecordTestData.getASingleSyntheticCMMStudyAsList;
import static eu.cessda.pasc.oci.data.RecordTestData.getSyntheticCMMStudyAndADeletedRecordAsList;
import static org.assertj.core.api.Java6BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
public class LanguageDocumentExtractorTest extends AbstractSpringTestProfileContext {

  @Autowired
  private LanguageDocumentExtractor languageDocumentExtractor;

  @Autowired
  private CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter;

  private static final String ID_PREFIX = "test-stub";

  @Test
  public void shouldRejectRecordWhenNotInListOfLanguagesAvailableIn() throws IOException {

    // Given
    CMMStudy syntheticCmmStudy = RecordTestData.getSyntheticCmmStudy();
    syntheticCmmStudy.getLangAvailableIn().remove("en");

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", ID_PREFIX, syntheticCmmStudy);

    then(validCMMStudyForLang).isFalse();
  }

  @Test
  public void shouldValidateRecordsWhenGivenLanguageCodeIsInListOfLanguagesAvailableIn() throws IOException {

    // Given
    CMMStudy cmmStudy = RecordTestData.getSyntheticCmmStudy();

    // When
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("de", ID_PREFIX, cmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", ID_PREFIX, cmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fi", ID_PREFIX, cmmStudy);
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
    boolean validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("de", ID_PREFIX, syntheticCmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("en", ID_PREFIX, syntheticCmmStudy);
    then(validCMMStudyForLang).isTrue();

    validCMMStudyForLang = languageDocumentExtractor.isValidCMMStudyForLang("fi", ID_PREFIX, syntheticCmmStudy);
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
    Map<String, List<CMMStudyOfLanguage>> languageDocMap =
            languageDocumentExtractor.mapLanguageDoc(studies, "UK Data Service");

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
    Map<String, List<CMMStudyOfLanguage>> languageDocMap =
            languageDocumentExtractor.mapLanguageDoc(studies, "UK Data Service");

    then(languageDocMap).isNotNull();
    then(languageDocMap).hasSize(17);
    then(languageDocMap).containsOnlyKeys("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");
    then(languageDocMap.get("de")).hasSize(2); // a deleted record and an active record that is valid
    then(languageDocMap.get("en")).hasSize(2); // a deleted record and an active record that is valid
    then(languageDocMap.get("fi")).hasSize(2); // a deleted record and an active record that is valid
    //then(languageDocMap.get("sv")).hasSize(1); // a deleted record and an active record that is not valid for lang

    List<CMMStudyOfLanguage> enStudy = languageDocMap.get("en");
    System.out.println("Printing Records");
    for (CMMStudyOfLanguage cmmStudyOfLanguage : enStudy) {
      String enCMMStudyJsonStringOpt = cmmStudyOfLanguageConverter.toJsonString(cmmStudyOfLanguage);
      System.out.println(enCMMStudyJsonStringOpt);
    }
  }

  @Test
  public void shouldReturnFalseForInValidCMMStudyForLang() {

    // When
    boolean actual = languageDocumentExtractor.isValidCMMStudyForLang("en", "UK Data Service", null);

    then(actual).isFalse();
  }
}
