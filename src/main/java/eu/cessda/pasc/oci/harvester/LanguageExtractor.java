/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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

import com.neovisionaries.i18n.CountryCode;
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
import java.util.stream.Stream;

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
     */
    public Map<String, List<CMMStudyOfLanguage>> mapLanguageDoc(@NonNull Collection<CMMStudy> cmmStudies, @NonNull Repo repository) {

        log.debug("[{}] Mapping [{}] CMMStudies to CMMStudyOfLanguage", repository.getCode(), cmmStudies.size());


        var collectLanguageCmmStudy = cmmStudies.stream()
            // Map the study to the language specific variant
            .flatMap(cmmStudy -> {
                var validLanguages = appConfigurationProperties.getLanguages().stream()
                    .filter(langCode -> isValidCMMStudyForLang(cmmStudy, langCode))
                    .collect(Collectors.toList());

                if (!validLanguages.isEmpty()) {
                    var studyOfLanguages = new HashMap<String, CMMStudyOfLanguage>(validLanguages.size());
                    for (var langCode : validLanguages) {
                        studyOfLanguages.put(langCode, getCmmStudyOfLanguage(cmmStudy, langCode, validLanguages, repository));
                    }
                    return studyOfLanguages.entrySet().stream();
                } else {
                    log.debug("[{}] No valid languages for study [{}]", repository.getCode(), cmmStudy.getStudyNumber());
                    return Stream.empty();
                }
            }).collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        log.debug("[{}] has [{}] records that passed CMM minimum fields validation", repository.getCode(), collectLanguageCmmStudy.entrySet().size());

        return collectLanguageCmmStudy;
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
    boolean isValidCMMStudyForLang(CMMStudy cmmStudy, String languageIsoCode) {

        // Inactive = deleted record, no need to validate against CMM below. This will be deleted in the index.
        if (!cmmStudy.isActive()) {
            return true;
        }

        return hasMinimumCmmFields(cmmStudy, languageIsoCode);
    }

    private CMMStudyOfLanguage getCmmStudyOfLanguage(CMMStudy cmmStudy, String lang, Collection<String> availableLanguages, Repo repository) {

        log.trace("[{}] Extracting CMMStudyOfLanguage for study [{}], language [{}]", repository.getCode(), cmmStudy.getStudyNumber(), lang);

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
            .langAvailableIn(Set.copyOf(availableLanguages))
            .studyXmlSourceUrl(cmmStudy.getStudyXmlSourceUrl());

        // #183: The CDC user group would like the publisher to be set based on the indexer's configuration
        builder.publisher(Publisher.builder().name(repository.getName()).abbreviation(repository.getCode()).build());

        // Language specific field extraction
        Optional.ofNullable(cmmStudy.getTitleStudy()).ifPresent(map -> builder.titleStudy(map.get(lang)));
        Optional.ofNullable(cmmStudy.getAbstractField()).ifPresent(map -> builder.abstractField(map.get(lang)));
        Optional.ofNullable(cmmStudy.getKeywords()).ifPresent(map -> builder.keywords(map.get(lang)));
        Optional.ofNullable(cmmStudy.getClassifications()).ifPresent(map -> builder.classifications(map.get(lang)));
        Optional.ofNullable(cmmStudy.getTypeOfTimeMethods()).ifPresent(map -> builder.typeOfTimeMethods(map.get(lang)));
        var countries = Optional.ofNullable(cmmStudy.getStudyAreaCountries())
            .map(map -> map.get(lang)).stream().flatMap(Collection::stream)
            // If the ISO code is not valid, then the optional will be empty
            .map(country -> Optional.ofNullable(CountryCode.getByCode(country.getIsoCode()))
                .map(CountryCode::getName)
                .map(country::withSearchField)
                .orElse(country)
            ).collect(Collectors.toList());
        builder.studyAreaCountries(countries);
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
