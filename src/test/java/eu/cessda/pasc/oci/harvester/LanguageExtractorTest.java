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
package eu.cessda.pasc.oci.harvester;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.mock.data.RecordTestData;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.getASingleSyntheticCMMStudyAsList;
import static eu.cessda.pasc.oci.mock.data.RecordTestData.getSyntheticCMMStudyAndADeletedRecordAsList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
@Slf4j
public class LanguageExtractorTest {

    private final LanguageExtractor languageExtractor;
    private final CMMStudyOfLanguageConverter cmmStudyOfLanguageConverter = new CMMStudyOfLanguageConverter();

    public LanguageExtractorTest() {
        var appConfigurationProperties = Mockito.mock(AppConfigurationProperties.class);
        Mockito.when(appConfigurationProperties.getLanguages()).thenReturn(ReposTestData.getListOfLanguages());
        languageExtractor = new LanguageExtractor(appConfigurationProperties);
    }

    @Test
    public void shouldValidateRecordsWhenGivenLanguageCodeIsInListOfLanguagesAvailableIn() throws IOException {

        // Given
        CMMStudy cmmStudy = RecordTestData.getSyntheticCmmStudy();

        // When
        boolean validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(cmmStudy, "de");
        then(validCMMStudyForLang).isTrue();

        validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(cmmStudy, "en");
        then(validCMMStudyForLang).isTrue();

        validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(cmmStudy, "fi");
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
        syntheticCmmStudy.withActive(false);

        // When
        boolean validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "de");
        then(validCMMStudyForLang).isTrue();

        validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "en");
        then(validCMMStudyForLang).isTrue();

        validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "fi");
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
        Map<String, List<CMMStudyOfLanguage>> languageDocMap = languageExtractor.mapLanguageDoc(studies, ReposTestData.getUKDSRepo());

        then(languageDocMap).isNotNull();
        then(languageDocMap).hasSize(3);
        then(languageDocMap).containsOnlyKeys("de", "en", "fi");
        then(languageDocMap.get("de")).hasSize(1);
        then(languageDocMap.get("en")).hasSize(1);
        then(languageDocMap.get("fi")).hasSize(1);
        //Synthetic doc does not exist, or does have a studyTitle or abstract, so language is skipped
        //then(languageDocMap.get("fr")).hasSize(0);
        //then(languageDocMap.get("nl")).hasSize(0);
        //then(languageDocMap.get("se")).hasSize(0);
        //then(languageDocMap.get("sk")).hasSize(0);

    }

    @Test
    public void shouldReturnExtractedDocInTheirRespectiveLangDocumentsIncludingDeletedRecordsMarkedAsInActive() {

        // Given
        List<CMMStudy> studies = getSyntheticCMMStudyAndADeletedRecordAsList();

        // When
        Map<String, List<CMMStudyOfLanguage>> languageDocMap = languageExtractor.mapLanguageDoc(studies, ReposTestData.getUKDSRepo());

        then(languageDocMap).isNotNull();
        then(languageDocMap).hasSize(17);
        then(languageDocMap).containsOnlyKeys("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");
        then(languageDocMap.get("de")).hasSize(2); // a deleted record and an active record that is valid
        then(languageDocMap.get("en")).hasSize(2); // a deleted record and an active record that is valid
        then(languageDocMap.get("fi")).hasSize(2); // a deleted record and an active record that is valid
        //then(languageDocMap.get("sv")).hasSize(1); // a deleted record and an active record that is not valid for lang

    }

    @Test
    public void shouldTagAllLanguagesThatPassMinimumCMMRequirementAsStudyAvailableInGivenLang() throws IOException {

        // Given
        final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();

        // When
        var cmmStudyOfLanguage = languageExtractor.mapLanguageDoc(Collections.singleton(cmmStudyWithNoAvailableLangSet), ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage.keySet()).containsExactlyInAnyOrder("en", "fi", "de");
        cmmStudyOfLanguage.values().forEach(cmmStudyOfLanguages -> assertThat(cmmStudyOfLanguages.get(0).getLangAvailableIn()).containsExactlyInAnyOrder("en", "fi", "de"));
    }

    @Test
    public void shouldNotTagAnyLangWhenCMMStudyDoesNotHaveTheRequiredCMMStudyIdentifier() throws IOException {

        // Given
        CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();

        // When
        var cmmStudyOfLanguage = languageExtractor.mapLanguageDoc(Collections.singleton(cmmStudyWithNoAvailableLangSet.withStudyNumber(null)), ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage).isEmpty();
    }

    @Test
    public void shouldNotTagEnglishWhenCMMStudyDoesNotHaveTheRequiredCMMStudyTitleForEnglish() throws IOException {

        // Given
        final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();
        cmmStudyWithNoAvailableLangSet.getTitleStudy().remove("en");

        // When
        var cmmStudyOfLanguage = languageExtractor.mapLanguageDoc(Collections.singleton(cmmStudyWithNoAvailableLangSet), ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage.keySet()).doesNotContain("en");
        assertThat(cmmStudyOfLanguage.keySet()).containsExactlyInAnyOrder("fi", "de");
        cmmStudyOfLanguage.values().forEach(cmmStudyOfLanguages -> assertThat(cmmStudyOfLanguages.get(0).getLangAvailableIn()).containsExactlyInAnyOrder("fi", "de"));
    }

    @Test
    public void shouldNotTagFinishWhenCMMStudyDoesNotHaveTheRequiredCMMStudyAbstractForFinish() throws IOException {

        // Given
        final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();
        cmmStudyWithNoAvailableLangSet.getAbstractField().remove("fi");

        // When
        var cmmStudyOfLanguage = languageExtractor.mapLanguageDoc(Collections.singleton(cmmStudyWithNoAvailableLangSet), ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage.keySet()).doesNotContain("fi");
        assertThat(cmmStudyOfLanguage.keySet()).containsExactlyInAnyOrder("en", "de");
        cmmStudyOfLanguage.values().forEach(cmmStudyOfLanguages -> assertThat(cmmStudyOfLanguages.get(0).getLangAvailableIn()).containsExactlyInAnyOrder("en", "de"));
    }

    @Test
    public void shouldNotTagGermanWhenCMMStudyDoesNotHaveTheRequiredCMMStudyPublisherForGerman() throws IOException {

        // Given
        final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();
        cmmStudyWithNoAvailableLangSet.getPublisher().remove("de");

        // When
        var cmmStudyOfLanguage = languageExtractor.mapLanguageDoc(Collections.singleton(cmmStudyWithNoAvailableLangSet), ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage.keySet()).doesNotContain("de");
        assertThat(cmmStudyOfLanguage.keySet()).containsExactlyInAnyOrder("en", "fi");
        cmmStudyOfLanguage.values().forEach(cmmStudyOfLanguages -> assertThat(cmmStudyOfLanguages.get(0).getLangAvailableIn()).containsExactlyInAnyOrder("en", "fi"));
    }
}
