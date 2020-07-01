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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Component responsible for extracting and mapping languages in which a given CMMStudy is available.
 * <p>
 * Note the CMM record must meet the minimum CMM Fields requirements.
 *
 * @author moses AT doraventures DOT com
 */
@Component
public class LanguageAvailabilityMapper {

  private final AppConfigurationProperties appConfigurationProperties;

  @Autowired
  public LanguageAvailabilityMapper(AppConfigurationProperties appConfigurationProperties) {
    this.appConfigurationProperties = appConfigurationProperties;
  }

  /**
   * Sets the available language field based on whether the minimum fields for that language are available.
   * <p>
   * The minimum fields required for a language are a title, an abstract, a study number and a publisher.
   *
   * @param cmmStudy the {@link CMMStudy} to check.
   */
  public void setAvailableLanguages(@NonNull CMMStudy cmmStudy) {
    final List<String> propertiesLanguages = appConfigurationProperties.getLanguages();
    for (String lang : propertiesLanguages) {
      if (hasMinimumCmmFields(cmmStudy, lang)) {
        cmmStudy.getLangAvailableIn().add(lang);
      }
    }
  }

  private boolean hasMinimumCmmFields(CMMStudy cmmStudy, String languageIsoCode) {
    // the CMM record must meet the minimum CMM Fields requirements for given Lang Iso Code
    // It must have a title, an abstract field, a study number and a publisher
    return (cmmStudy.getTitleStudy() != null) && (cmmStudy.getTitleStudy().get(languageIsoCode) != null) &&
            (cmmStudy.getAbstractField() != null) && (cmmStudy.getAbstractField().get(languageIsoCode) != null) &&
            (cmmStudy.getStudyNumber() != null) && !cmmStudy.getStudyNumber().isEmpty() &&
            (cmmStudy.getPublisher() != null) && (cmmStudy.getPublisher().get(languageIsoCode) != null);
  }
}
