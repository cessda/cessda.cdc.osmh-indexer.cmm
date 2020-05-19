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
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

  public void setAvailableLanguages(CMMStudy cmmStudy) {

    final List<String> propertiesLanguages = appConfigurationProperties.getLanguages();
    for (String lang : propertiesLanguages) {
      if (hasMinimumCmmFields(lang, cmmStudy)) {
        cmmStudy.getLangAvailableIn().add(lang);
      }
    }
  }

  private boolean hasMinimumCmmFields(String languageIsoCode, CMMStudy cmmStudy) {

    if (cmmStudy == null) {
      return false;
    }

    // the CMM record must meet the minimum CMM Fields requirements for given Lang Iso Code
    return hasTitle(languageIsoCode, cmmStudy) &&
        hasAbstract(languageIsoCode, cmmStudy) &&
        hasStudyNumber(cmmStudy) &&
        hasPublisher(languageIsoCode, cmmStudy);
  }

  private boolean hasPublisher(String languageIsoCode, CMMStudy cmmStudy) {
    Map<String, Publisher> publisherMap = cmmStudy.getPublisher();
    return (publisherMap != null) && (publisherMap.get(languageIsoCode) != null);
  }

  private boolean hasStudyNumber(CMMStudy cmmStudy) {
    String studyNumber = cmmStudy.getStudyNumber();
    return (studyNumber != null) && !studyNumber.isEmpty();
  }

  private boolean hasAbstract(String languageIsoCode, CMMStudy cmmStudy) {
    Map<String, String> abstractFieldMap = cmmStudy.getAbstractField();
    return (abstractFieldMap != null) && (abstractFieldMap.get(languageIsoCode) != null);
  }

  private boolean hasTitle(String languageIsoCode, CMMStudy cmmStudy) {
    Map<String, String> titleStudyMap = cmmStudy.getTitleStudy();
    return (titleStudyMap != null) && (titleStudyMap.get(languageIsoCode) != null);
  }
}
