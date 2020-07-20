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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Component responsible for extracting and mapping languages in which a given CMMStudy is available.
 * <p>
 * Note the CMM record must meet the minimum CMM Fields requirements.
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
  public Map<String, List<CMMStudyOfLanguage>> mapLanguageDoc(@NonNull Collection<CMMStudy> cmmStudies, @NonNull Repo repository) {

    log.debug("[{}] Mapping [{}] CMMStudies to CMMStudyOfLanguage", repository.getCode(), cmmStudies.size());
    Map<String, List<CMMStudyOfLanguage>> languageDocMap = new HashMap<>(appConfigurationProperties.getLanguages().size());

    for (String langCode : appConfigurationProperties.getLanguages()) {
      log.trace("[{}] Extract CMMStudyOfLanguage for [{}] language code - STARTED", repository.getCode(), langCode);
      List<CMMStudyOfLanguage> collectLanguageCmmStudy = getCmmStudiesOfLangCode(cmmStudies, repository, langCode);
      languageDocMap.put(langCode, collectLanguageCmmStudy);
    }

    if (log.isDebugEnabled()) {
      for (Map.Entry<String, List<CMMStudyOfLanguage>> entry : languageDocMap.entrySet()) {
        log.debug("[{}] langIsoCode [{}] has [{}] records that passed CMM minimum fields validation", repository.getCode(), entry.getKey(), entry.getValue().size());
      }
    }
    return languageDocMap;
  }

  private List<CMMStudyOfLanguage> getCmmStudiesOfLangCode(Collection<CMMStudy> cmmStudies, Repo repository, String languageIsoCode) {
    return cmmStudies.stream().filter(cmmStudy -> isValidCMMStudyForLang(cmmStudy, languageIsoCode))
            .map(cmmStudy -> getCmmStudyOfLanguage(repository, languageIsoCode, cmmStudy))
            .collect(Collectors.toList());
  }

  /**
   * CMM Model minimum field check.  Restriction here has been reduced from these previous mandatory fields:
   * title, abstract, studyNumber and publisher
   *
   * @param cmmStudy        the CmmStudy Object
   * @param languageIsoCode the languageIsoCode
   * @return true if Study is available in other languages
   * @throws NullPointerException if any of the parameters are {@code null}
   */
  boolean isValidCMMStudyForLang(@NonNull CMMStudy cmmStudy, @NonNull String languageIsoCode) {

    // Inactive = deleted record no need to validate against CMM below. Index as is. Filtered in Frontend.
    if (!cmmStudy.isActive()) {
      return true;
    }

    return cmmStudy.getLangAvailableIn().contains(languageIsoCode);
  }

  private CMMStudyOfLanguage getCmmStudyOfLanguage(Repo repository, String lang, CMMStudy cmmStudy) {

    String formatMsg = "Extracting CMMStudyOfLang from CMMStudyNumber [{}] for lang [{}]";
    log.trace(formatMsg, cmmStudy.getStudyNumber(), lang);

    CMMStudyOfLanguage.CMMStudyOfLanguageBuilder builder = CMMStudyOfLanguage.builder();

    // Language neutral specific field extraction
    String idPrefix = repository.getCode().trim().replace(" ", "-") + "__"; // UK Data Service = UK-Data-Service__
    builder.id(idPrefix + cmmStudy.getStudyNumber())
            .code(repository.getCode())
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

    // #183: The CDC user group would like the publisher to be set based on the indexer's configuration
    builder.publisher(Publisher.builder().name(repository.getName()).abbreviation(repository.getCode()).build());

    // Language specific field extraction
    Optional.ofNullable(cmmStudy.getTitleStudy()).ifPresent(map -> builder.titleStudy(map.get(lang)));
    Optional.ofNullable(cmmStudy.getAbstractField()).ifPresent(map -> builder.abstractField(map.get(lang)));
    Optional.ofNullable(cmmStudy.getKeywords()).ifPresent(map -> builder.keywords(map.get(lang)));
    Optional.ofNullable(cmmStudy.getClassifications()).ifPresent(map -> builder.classifications(map.get(lang)));
    Optional.ofNullable(cmmStudy.getTypeOfTimeMethods()).ifPresent(map -> builder.typeOfTimeMethods(map.get(lang)));
    Optional.ofNullable(cmmStudy.getStudyAreaCountries()).ifPresent(map -> builder.studyAreaCountries(map.get(lang)));
    Optional.ofNullable(cmmStudy.getUnitTypes()).ifPresent(map -> builder.unitTypes(map.get(lang)));
    Optional.ofNullable(cmmStudy.getPidStudies()).ifPresent(map -> builder.pidStudies(map.get(lang)));
    Optional.ofNullable(cmmStudy.getCreators()).ifPresent(map -> builder.creators(map.get(lang)));
    Optional.ofNullable(cmmStudy.getTypeOfSamplingProcedures()).ifPresent(map -> builder.typeOfSamplingProcedures(map.get(lang)));
    Optional.ofNullable(cmmStudy.getSamplingProcedureFreeTexts()).ifPresent(map -> builder.samplingProcedureFreeTexts(map.get(lang)));
    Optional.ofNullable(cmmStudy.getTypeOfModeOfCollections()).ifPresent(map -> builder.typeOfModeOfCollections(map.get(lang)));
    Optional.ofNullable(cmmStudy.getTitleStudy()).ifPresent(map -> builder.titleStudy(map.get(lang)));
    Optional.ofNullable(cmmStudy.getDataCollectionFreeTexts()).ifPresent(map -> builder.dataCollectionFreeTexts(map.get(lang)));
    Optional.ofNullable(cmmStudy.getDataAccessFreeTexts()).ifPresent(map -> builder.dataAccessFreeTexts(map.get(lang)));
    Optional.ofNullable(cmmStudy.getStudyUrl()).ifPresent(map -> builder.studyUrl(map.get(lang)));

    return builder.build();
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
