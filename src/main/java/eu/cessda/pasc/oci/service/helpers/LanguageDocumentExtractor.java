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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Language Document Extractor.  Helper to Extracts a custom document for each language IsoCode found in the config.
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class LanguageDocumentExtractor {

  private final AppConfigurationProperties appConfigurationProperties;

  @Autowired
  public LanguageDocumentExtractor(AppConfigurationProperties appConfigurationProperties) {
    this.appConfigurationProperties = appConfigurationProperties;
  }

  /**
   * Extracts a custom document for each language IsoCode found in the config.
   *
   * @param cmmStudies filtered list of present studies which generally holds fields for all languages.
   * @return map extracted documents for each language iso code.
   */
  public Map<String, List<CMMStudyOfLanguage>> mapLanguageDoc(List<CMMStudy> cmmStudies, String spName) {

    log.info("Mapping CMMStudy to CMMStudyOfLanguage for SP[{}] with [{}] records", spName, cmmStudies.size());
    Map<String, List<CMMStudyOfLanguage>> languageDocMap = new HashMap<>();
    String idPrefix = spName.trim().replace(" ", "-") + "__"; // UK Data Service = UK-Data-Service__

    appConfigurationProperties.getLanguages().forEach(langCode -> {
          Instant start = Instant.now();
          log.trace("Extract CMMStudyOfLanguage for [{}] language code - STARTED", langCode);
          List<CMMStudyOfLanguage> collectLanguageCmmStudy = getCmmStudiesOfLangCode(cmmStudies, idPrefix, langCode);
          languageDocMap.put(langCode, collectLanguageCmmStudy);
          logTimeTook(langCode, start);
        }
    );

    if (log.isDebugEnabled()) {
      languageDocMap.forEach((langIsoCode, cmmStudyOfLanguages) -> {
        String formatTemplate = "langIsoCode [{}] has [{}] records that has passed CMM minimum fields validation";
        log.debug(formatTemplate, langIsoCode, cmmStudyOfLanguages.size());
      });
    }
    return languageDocMap;
  }

  private List<CMMStudyOfLanguage> getCmmStudiesOfLangCode(List<CMMStudy> cmmStudies, String idPrefix,
                                                           String languageIsoCode) {
    return cmmStudies.stream()
        .filter(cmmStudy -> isValidCMMStudyForLang(languageIsoCode, idPrefix, cmmStudy))
        .map(cmmStudy -> getCmmStudyOfLanguage(idPrefix, languageIsoCode, cmmStudy))
        .collect(Collectors.toList());
  }

  /**
   * CMM Model minimum field check.  Restriction here has been reduced from these previous mandatory fields:
   * title, abstract, studyNumber and publisher
   *
   * @param languageIsoCode the languageIsoCode
   * @param idPrefix        the idPrefix
   * @param cmmStudy        the CmmStudy Object
   * @return true if Study is available in other languages
   */
  boolean isValidCMMStudyForLang(String languageIsoCode, String idPrefix, CMMStudy cmmStudy) {

    if (null == cmmStudy) {
      logInvalidCMMStudy("Study is null for language[{}] with studyNumber [{}]", languageIsoCode, idPrefix, null);
      return false;
    }

    // Inactive = deleted record no need to validate against CMM below. Index as is. Filtered in Frontend.
    if (!cmmStudy.isActive()) {
      logInvalidCMMStudy("Study is not Active for language [{}] with studyNumber [{}]", languageIsoCode, idPrefix, cmmStudy);
      return true;
    }

    return cmmStudy.getLangAvailableIn().contains(languageIsoCode);
  }

  private void logInvalidCMMStudy(String msgTemplate, String languageIsoCode, String idPrefix, CMMStudy cmmStudy) {
    if (log.isWarnEnabled()) {
      final String studyNumber = idPrefix + "-" + (null != cmmStudy ? cmmStudy.getStudyNumber() : "Empty");
      log.warn(msgTemplate, languageIsoCode, studyNumber);
    }
  }

  private CMMStudyOfLanguage getCmmStudyOfLanguage(String idPrefix, String lang, CMMStudy cmmStudy) {

    String formatMsg = "Extracting CMMStudyOfLang from CMMStudyNumber [{}] for lang [{}]";
    log.trace(formatMsg, cmmStudy.getStudyNumber(), lang);

    CMMStudyOfLanguage.CMMStudyOfLanguageBuilder builder = CMMStudyOfLanguage.builder();

    // Language neutral specific field extraction
    builder.id(idPrefix + cmmStudy.getStudyNumber())
        .studyNumber(cmmStudy.getStudyNumber())
        .active(cmmStudy.isActive())
        .lastModified(cmmStudy.getLastModified())
        .publicationYear(cmmStudy.getPublicationYear())
        .fileLanguages(cmmStudy.getFileLanguages())
        .dataCollectionPeriodStartdate(cmmStudy.getDataCollectionPeriodStartdate())
        .dataCollectionPeriodEnddate(cmmStudy.getDataCollectionPeriodEnddate())
        .dataCollectionYear(cmmStudy.getDataCollectionYear())
        .langAvailableIn(cmmStudy.getLangAvailableIn())
        .studyXmlSourceUrl(cmmStudy.getStudyXmlSourceUrl());

    // Language specific field extraction
    ofNullable(cmmStudy.getTitleStudy()).ifPresent(map -> builder.titleStudy(map.get(lang)));
    ofNullable(cmmStudy.getAbstractField()).ifPresent(map -> builder.abstractField(map.get(lang)));
    ofNullable(cmmStudy.getKeywords()).ifPresent(map -> builder.keywords(map.get(lang)));
    ofNullable(cmmStudy.getClassifications()).ifPresent(map -> builder.classifications(map.get(lang)));
    ofNullable(cmmStudy.getTypeOfTimeMethods()).ifPresent(map -> builder.typeOfTimeMethods(map.get(lang)));
    ofNullable(cmmStudy.getStudyAreaCountries()).ifPresent(map -> builder.studyAreaCountries(map.get(lang)));
    ofNullable(cmmStudy.getUnitTypes()).ifPresent(map -> builder.unitTypes(map.get(lang)));
    ofNullable(cmmStudy.getPublisher()).ifPresent(map -> builder.publisher(map.get(lang)));
    ofNullable(cmmStudy.getPidStudies()).ifPresent(map -> builder.pidStudies(map.get(lang)));
    ofNullable(cmmStudy.getCreators()).ifPresent(map -> builder.creators(map.get(lang)));
    ofNullable(cmmStudy.getTypeOfSamplingProcedures()).ifPresent(map -> builder.typeOfSamplingProcedures(map.get(lang)));
    ofNullable(cmmStudy.getSamplingProcedureFreeTexts()).ifPresent(map -> builder.samplingProcedureFreeTexts(map.get(lang)));
    ofNullable(cmmStudy.getTypeOfModeOfCollections()).ifPresent(map -> builder.typeOfModeOfCollections(map.get(lang)));
    ofNullable(cmmStudy.getTitleStudy()).ifPresent(map -> builder.titleStudy(map.get(lang)));
    ofNullable(cmmStudy.getDataCollectionFreeTexts()).ifPresent(map -> builder.dataCollectionFreeTexts(map.get(lang)));
    ofNullable(cmmStudy.getDataAccessFreeTexts()).ifPresent(map -> builder.dataAccessFreeTexts(map.get(lang)));
    ofNullable(cmmStudy.getStudyUrl()).ifPresent(map -> builder.studyUrl(map.get(lang)));

    return builder.build();
  }

  private static void logTimeTook(String langCode, Instant startTime) {
    String formatMsg = "Extract CMMStudyOfLanguage for [{}] language code - COMPLETED. Duration [{}]";
    Instant end = Instant.now();
    log.trace(formatMsg, langCode, Duration.between(startTime, end));
  }
}
