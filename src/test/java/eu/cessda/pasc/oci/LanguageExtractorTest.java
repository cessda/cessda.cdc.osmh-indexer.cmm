/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.mock.data.RecordTestData;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static eu.cessda.pasc.oci.mock.data.RecordTestData.getASingleSyntheticCMMStudyAsList;
import static eu.cessda.pasc.oci.mock.data.RecordTestData.getSyntheticCMMStudyAndADeletedRecordAsList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests related to {@link LanguageExtractor}
 *
 * @author moses AT doraventures DOT com
 */
public class LanguageExtractorTest {

    private final LanguageExtractor languageExtractor;

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
    }

    @Test
    public void shouldValidateRecordsThatAreMarkedAsInactiveByPassingTheMinimumCMMFieldsChecks() throws IOException {

        // Given
        CMMStudy syntheticCmmStudy = RecordTestData.getSyntheticCmmStudy();

        // When
        boolean validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "de");
        then(validCMMStudyForLang).isTrue();

        validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "en");
        then(validCMMStudyForLang).isTrue();

        validCMMStudyForLang = languageExtractor.isValidCMMStudyForLang(syntheticCmmStudy, "fi");
        then(validCMMStudyForLang).isTrue();
    }

    @Test
    public void shouldReturnExtractedDocInTheirRespectiveLangDocuments() {

        // Given
        List<CMMStudy> studies = getASingleSyntheticCMMStudyAsList();

        // When
        var languageDocMap = new HashMap<String, List<CMMStudyOfLanguage>>();
        for (var study : studies) {
            var result = languageExtractor.extractFromStudy(study, ReposTestData.getUKDSRepo());
            for (var entry : result.entrySet()) {
                languageDocMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }

        then(languageDocMap).isNotNull();
        then(languageDocMap).hasSize(3);
        then(languageDocMap).containsOnlyKeys("de", "en", "fi");
        then(languageDocMap.get("de")).hasSize(1);
        then(languageDocMap.get("en")).hasSize(1);
        then(languageDocMap.get("fi")).hasSize(1);

    }

    @Test
    public void shouldReturnExtractedDocInTheirRespectiveLangDocumentsExcudingDeletedDocuments() {

        // Given
        List<CMMStudy> studies = getSyntheticCMMStudyAndADeletedRecordAsList();

        // When
        var languageDocMap = new HashMap<String, List<CMMStudyOfLanguage>>();
        for (var study : studies) {
            var result = languageExtractor.extractFromStudy(study, ReposTestData.getUKDSRepo());
            for (var entry : result.entrySet()) {
                languageDocMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }

        then(languageDocMap).isNotNull();
        then(languageDocMap).hasSize(3);
        then(languageDocMap).containsOnlyKeys("de", "en", "fi");
        then(languageDocMap.get("de")).hasSize(1); // an active record that is valid
        then(languageDocMap.get("en")).hasSize(1); // an active record that is valid
        then(languageDocMap.get("fi")).hasSize(1); // an active record that is valid

    }

    @Test
    public void shouldTagAllLanguagesThatPassMinimumCMMRequirementAsStudyAvailableInGivenLang() throws IOException {

        // Given
        final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();

        // When
        var cmmStudyOfLanguage = languageExtractor.extractFromStudy(cmmStudyWithNoAvailableLangSet, ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage.keySet()).containsExactlyInAnyOrder("en", "fi", "de");
        cmmStudyOfLanguage.values().forEach(cmmStudyOfLanguages ->
            assertThat(cmmStudyOfLanguages.getLangAvailableIn()).containsExactlyInAnyOrder("en", "fi", "de")
        );
    }

    @Test
    public void shouldNotTagAnyLangWhenCMMStudyDoesNotHaveTheRequiredCMMStudyIdentifier() throws IOException {

        // Given
        CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();

        // When
        var cmmStudyOfLanguage = languageExtractor.extractFromStudy(cmmStudyWithNoAvailableLangSet.withStudyNumber(null), ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage).isEmpty();
    }

    @Test
    public void shouldNotTagEnglishWhenCMMStudyDoesNotHaveTheRequiredCMMStudyTitleForEnglish() throws IOException {

        // Given
        final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();
        cmmStudyWithNoAvailableLangSet.getTitleStudy().remove("en");

        // When
        var cmmStudyOfLanguage = languageExtractor.extractFromStudy(cmmStudyWithNoAvailableLangSet, ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage.keySet()).doesNotContain("en");
        assertThat(cmmStudyOfLanguage.keySet()).containsExactlyInAnyOrder("fi", "de");
        cmmStudyOfLanguage.values().forEach(cmmStudyOfLanguages ->
            assertThat(cmmStudyOfLanguages.getLangAvailableIn()).containsExactlyInAnyOrder("fi", "de")
        );
    }

    @Test
    public void shouldNotTagFinishWhenCMMStudyDoesNotHaveTheRequiredCMMStudyAbstractForFinish() throws IOException {

        // Given
        final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();
        cmmStudyWithNoAvailableLangSet.getAbstractField().remove("fi");

        // When
        var cmmStudyOfLanguage = languageExtractor.extractFromStudy(cmmStudyWithNoAvailableLangSet, ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage.keySet()).doesNotContain("fi");
        assertThat(cmmStudyOfLanguage.keySet()).containsExactlyInAnyOrder("en", "de");
        cmmStudyOfLanguage.values().forEach(cmmStudyOfLanguages ->
            assertThat(cmmStudyOfLanguages.getLangAvailableIn()).containsExactlyInAnyOrder("en", "de")
        );
    }

    @Test
    public void shouldNotTagGermanWhenCMMStudyDoesNotHaveTheRequiredCMMStudyPublisherForGerman() throws IOException {

        // Given
        final CMMStudy cmmStudyWithNoAvailableLangSet = RecordTestData.getSyntheticCmmStudy();
        cmmStudyWithNoAvailableLangSet.getPublisher().remove("de");

        // When
        var cmmStudyOfLanguage = languageExtractor.extractFromStudy(cmmStudyWithNoAvailableLangSet, ReposTestData.getUKDSRepo());

        assertThat(cmmStudyOfLanguage.keySet()).doesNotContain("de");
        assertThat(cmmStudyOfLanguage.keySet()).containsExactlyInAnyOrder("en", "fi");
        cmmStudyOfLanguage.values().forEach(cmmStudyOfLanguages ->
            assertThat(cmmStudyOfLanguages.getLangAvailableIn()).containsExactlyInAnyOrder("en", "fi")
        );
    }
}
