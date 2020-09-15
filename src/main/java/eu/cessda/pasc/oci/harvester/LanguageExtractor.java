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
public class LanguageExtractor {

    private final AppConfigurationProperties appConfigurationProperties;

    @Autowired
    public LanguageExtractor(AppConfigurationProperties appConfigurationProperties) {
        this.appConfigurationProperties = appConfigurationProperties;
    }

    /**
     * Extracts a custom document for each language IsoCode found in the config.
     *
     * @param cmmStudies filtered list of present studies which generally holds fields for all languages.
     * @param repository the repository where the study originated.
     * @return map extracted documents for each language iso code.
     * @throws NullPointerException if any of the parameters are {@code null}
     */
    public Map<String, List<CMMStudyOfLanguage>> mapLanguageDoc(@NonNull Collection<CMMStudy> cmmStudies, @NonNull Repo repository) {

        log.debug("[{}] Mapping [{}] CMMStudies to CMMStudyOfLanguage", repository.getCode(), cmmStudies.size());
        var languageDocMap = new HashMap<String, List<CMMStudyOfLanguage>>(appConfigurationProperties.getLanguages().size());

        for (String langCode : appConfigurationProperties.getLanguages()) {
            log.trace("[{}] Extract CMMStudyOfLanguage for [{}] language code - STARTED", repository.getCode(), langCode);
            var collectLanguageCmmStudy = cmmStudies.stream()
                // Filter out if the study is not valid for the current language
                .filter(cmmStudy -> isValidCMMStudyForLang(cmmStudy, langCode))
                // Map the study to the language specific variant
                .map(cmmStudy -> getCmmStudyOfLanguage(cmmStudy, langCode, repository))
                .collect(Collectors.toList());
            log.debug("[{}] langIsoCode [{}] has [{}] records that passed CMM minimum fields validation", repository.getCode(), langCode, collectLanguageCmmStudy.size());
            if (!collectLanguageCmmStudy.isEmpty()) {
                languageDocMap.put(langCode, collectLanguageCmmStudy);
            }
        }

        return languageDocMap;
    }

    /**
     * CMM Model minimum field check.  Restriction here has been reduced from these previous mandatory fields:
     * title, abstract, studyNumber and publisher
     *
     * @param cmmStudy        the {@link CMMStudy} to check
     * @param languageIsoCode the languageIsoCode
     * @return true if Study is available in the specified language
     * @throws NullPointerException if any of the parameters are {@code null}
     */
    boolean isValidCMMStudyForLang(@NonNull CMMStudy cmmStudy, @NonNull String languageIsoCode) {

        // Inactive = deleted record no need to validate against CMM below. Index as is. Filtered in Frontend.
        if (!cmmStudy.isActive()) {
            return true;
        }

        return hasMinimumCmmFields(cmmStudy, languageIsoCode);
    }

    private CMMStudyOfLanguage getCmmStudyOfLanguage(CMMStudy cmmStudy, String lang, Repo repository) {

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
            .langAvailableIn(appConfigurationProperties.getLanguages().stream()
                .filter(langCode -> hasMinimumCmmFields(cmmStudy, langCode))
                .collect(Collectors.toSet()))
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

        // #142 - Use any language to set the study url field
        Optional.ofNullable(cmmStudy.getStudyUrl()).flatMap(map -> map.entrySet().stream().findAny()).map(Map.Entry::getValue).ifPresent(builder::studyUrl);

        // Override with the language specific variant
        Optional.ofNullable(cmmStudy.getStudyUrl()).flatMap(map -> Optional.ofNullable(map.get(lang))).ifPresent(builder::studyUrl);

        return builder.build();
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
