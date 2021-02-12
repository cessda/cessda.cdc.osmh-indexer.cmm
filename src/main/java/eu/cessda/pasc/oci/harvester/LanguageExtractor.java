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
     * Extracts the language specific variants of a given CMMStudy
     * @param cmmStudy the study to extract
     * @param repository the repository the study was harvested from
     * @return an unmodifiable {@link Map} with extracted documents for each language ISO code
     */
    public Map<String, CMMStudyOfLanguage> extractFromStudy(CMMStudy cmmStudy, Repo repository) {
        var validLanguages = appConfigurationProperties.getLanguages().stream()
            .filter(langCode -> isValidCMMStudyForLang(cmmStudy, langCode))
            .collect(Collectors.toList());

        if (!validLanguages.isEmpty()) {
            var studyOfLanguages = new TreeMap<String, CMMStudyOfLanguage>();
            for (var langCode : validLanguages) {
                studyOfLanguages.put(langCode, getCmmStudyOfLanguage(cmmStudy, langCode, validLanguages, repository));
            }
            return Collections.unmodifiableMap(studyOfLanguages);
        } else {
            log.debug("[{}] No valid languages for study [{}]", repository.getCode(), cmmStudy.getStudyNumber());
            return Collections.emptyMap();
        }
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
        Optional.ofNullable(cmmStudy.getTitleStudy()).map(map -> map.get(lang)).ifPresent(builder::titleStudy);
        Optional.ofNullable(cmmStudy.getAbstractField()).map(map -> map.get(lang)).ifPresent(builder::abstractField);
        Optional.ofNullable(cmmStudy.getKeywords()).map(map -> map.get(lang)).ifPresent(builder::keywords);
        Optional.ofNullable(cmmStudy.getClassifications()).map(map -> map.get(lang)).ifPresent(builder::classifications);
        Optional.ofNullable(cmmStudy.getTypeOfTimeMethods()).map(map -> map.get(lang)).ifPresent(builder::typeOfTimeMethods);
        var countries = Optional.ofNullable(cmmStudy.getStudyAreaCountries())
            .map(map -> map.get(lang)).stream().flatMap(Collection::stream)
            // If the ISO code is not valid, then the optional will be empty
            .map(country -> Optional.ofNullable(CountryCode.getByCode(country.getIsoCode()))
                .map(CountryCode::getName)
                .map(country::withSearchField)
                .orElse(country)
            ).collect(Collectors.toList());
        builder.studyAreaCountries(countries);
        Optional.ofNullable(cmmStudy.getUnitTypes()).map(map -> map.get(lang)).ifPresent(builder::unitTypes);
        Optional.ofNullable(cmmStudy.getPidStudies()).map(map -> map.get(lang)).ifPresent(builder::pidStudies);
        Optional.ofNullable(cmmStudy.getCreators()).map(map -> map.get(lang)).ifPresent(builder::creators);
        Optional.ofNullable(cmmStudy.getTypeOfSamplingProcedures()).map(map -> map.get(lang)).ifPresent(builder::typeOfSamplingProcedures);
        Optional.ofNullable(cmmStudy.getSamplingProcedureFreeTexts()).map(map -> map.get(lang)).ifPresent(builder::samplingProcedureFreeTexts);
        Optional.ofNullable(cmmStudy.getTypeOfModeOfCollections()).map(map -> map.get(lang)).ifPresent(builder::typeOfModeOfCollections);
        Optional.ofNullable(cmmStudy.getTitleStudy()).map(map -> map.get(lang)).ifPresent(builder::titleStudy);
        Optional.ofNullable(cmmStudy.getDataCollectionFreeTexts()).map(map -> map.get(lang)).ifPresent(builder::dataCollectionFreeTexts);
        Optional.ofNullable(cmmStudy.getDataAccessFreeTexts()).map(map -> map.get(lang)).ifPresent(builder::dataAccessFreeTexts);

        // #142 - Use any language to set the study url field
        Optional.ofNullable(cmmStudy.getStudyUrl()).flatMap(map -> map.values().stream().findAny()).ifPresent(builder::studyUrl);

        // Override with the language specific variant
        Optional.ofNullable(cmmStudy.getStudyUrl()).map(map -> map.get(lang)).ifPresent(builder::studyUrl);

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
