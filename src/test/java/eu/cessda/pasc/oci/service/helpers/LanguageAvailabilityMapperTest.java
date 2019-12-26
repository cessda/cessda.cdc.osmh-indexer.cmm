/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
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
 * @author moses AT doraventures DOT com
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