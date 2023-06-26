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

import com.neovisionaries.i18n.CountryCode;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.Country;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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
            return validLanguages.stream().collect(Collectors.toUnmodifiableMap(
                langCode -> langCode,
                langCode -> getCmmStudyOfLanguage(cmmStudy, langCode, validLanguages, repository)
            ));
        } else {
            log.debug("[{}] No valid languages for study [{}]", repository.getCode(), cmmStudy.studyNumber());
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
        // the CMM record must meet the minimum CMM Fields requirements for given Lang Iso Code
        // It must have a title, an abstract field, a study number and a publisher
        return (cmmStudy.titleStudy() != null) && (cmmStudy.titleStudy().get(languageIsoCode) != null) &&
            (cmmStudy.abstractField() != null) && (cmmStudy.abstractField().get(languageIsoCode) != null) &&
            (cmmStudy.studyNumber() != null) && !cmmStudy.studyNumber().isEmpty() &&
            (cmmStudy.publisher() != null) && (cmmStudy.publisher().get(languageIsoCode) != null);
    }

    private CMMStudyOfLanguage getCmmStudyOfLanguage(CMMStudy cmmStudy, String lang, Collection<String> availableLanguages, Repo repository) {

        log.trace("[{}] Extracting CMMStudyOfLanguage for study [{}], language [{}]", repository.getCode(), cmmStudy.studyNumber(), lang);

        CMMStudyOfLanguage.CMMStudyOfLanguageBuilder builder = CMMStudyOfLanguage.builder();

        // Identifier generation -
        var id = cmmStudy.repositoryUrl() + "-" + cmmStudy.studyNumber();
        var hashedId = DigestUtils.sha256Hex(id.getBytes(StandardCharsets.UTF_8));

        // Language neutral specific field extraction
        // UK Data Service = UK-Data-Service__
        builder.id(hashedId)
            .code(repository.getCode())
            .studyNumber(cmmStudy.studyNumber())
            .lastModified(cmmStudy.lastModified())
            .publicationYear(cmmStudy.publicationYear())
            .fileLanguages(cmmStudy.fileLanguages())
            .dataCollectionPeriodStartdate(cmmStudy.dataCollectionPeriodStartdate())
            .dataCollectionPeriodEnddate(cmmStudy.dataCollectionPeriodEnddate())
            .dataCollectionYear(cmmStudy.dataCollectionYear())
            .langAvailableIn(Set.copyOf(availableLanguages));
        Optional.ofNullable(cmmStudy.studyXmlSourceUrl()).ifPresent(url -> builder.studyXmlSourceUrl(url.toString()));


        // #430: Set the publisher filter based on the source repository.
        builder.publisherFilter(new Publisher(repository.getCode(), repository.getName()));

        // Language specific field extraction
        Optional.ofNullable(cmmStudy.titleStudy()).map(map -> map.get(lang)).ifPresent(builder::titleStudy);
        Optional.ofNullable(cmmStudy.abstractField()).map(map -> map.get(lang)).ifPresent(builder::abstractField);
        Optional.ofNullable(cmmStudy.keywords()).map(map -> map.get(lang)).ifPresent(builder::keywords);
        Optional.ofNullable(cmmStudy.classifications()).map(map -> map.get(lang)).ifPresent(builder::classifications);
        Optional.ofNullable(cmmStudy.typeOfTimeMethods()).map(map -> map.get(lang)).ifPresent(builder::typeOfTimeMethods);
        var countries = Optional.ofNullable(cmmStudy.studyAreaCountries())
            .map(map -> map.get(lang)).stream().flatMap(Collection::stream)
            // If the ISO code is not valid, then the optional will be empty
            .map(country -> Optional.ofNullable(CountryCode.getByCode(country.isoCode()))
                .map(CountryCode::getName)
                .map(countryName -> new Country(country.isoCode(), country.elementText(), countryName))
                .orElse(country)
            ).toList();
        builder.studyAreaCountries(countries);
        Optional.ofNullable(cmmStudy.unitTypes()).map(map -> map.get(lang)).ifPresent(builder::unitTypes);
        Optional.ofNullable(cmmStudy.pidStudies()).map(map -> map.get(lang)).ifPresent(builder::pidStudies);
        Optional.ofNullable(cmmStudy.creators()).map(map -> map.get(lang)).ifPresent(builder::creators);
        Optional.ofNullable(cmmStudy.typeOfSamplingProcedures()).map(map -> map.get(lang)).ifPresent(builder::typeOfSamplingProcedures);
        Optional.ofNullable(cmmStudy.samplingProcedureFreeTexts()).map(map -> map.get(lang)).ifPresent(builder::samplingProcedureFreeTexts);
        Optional.ofNullable(cmmStudy.typeOfModeOfCollections()).map(map -> map.get(lang)).ifPresent(builder::typeOfModeOfCollections);
        Optional.ofNullable(cmmStudy.titleStudy()).map(map -> map.get(lang)).ifPresent(builder::titleStudy);
        Optional.ofNullable(cmmStudy.dataCollectionFreeTexts()).map(map -> map.get(lang)).ifPresent(builder::dataCollectionFreeTexts);
        Optional.ofNullable(cmmStudy.dataAccessFreeTexts()).map(map -> map.get(lang)).ifPresent(builder::dataAccessFreeTexts);
        Optional.ofNullable(cmmStudy.publisher()).map(map -> map.get(lang)).ifPresent(builder::publisher);
        Optional.ofNullable(cmmStudy.universe()).map(map -> map.get(lang)).ifPresent(builder::universe);

        // #502 - Use any language to set related publications, override with language specific field if presenet
        Optional.ofNullable(cmmStudy.relatedPublications()).flatMap(map -> map.values().stream().filter(Objects::nonNull).findAny()).ifPresent(builder::relatedPublications);
        Optional.ofNullable(cmmStudy.relatedPublications()).map(map -> map.get(lang)).ifPresent(builder::relatedPublications);

        // #142 - Use any language to set the study url field
        Optional.ofNullable(cmmStudy.studyUrl()).flatMap(map -> map.values().stream().filter(Objects::nonNull).findAny()).ifPresent(builder::studyUrl);

        // Override with the language specific variant
        Optional.ofNullable(cmmStudy.studyUrl()).map(map -> map.get(lang)).ifPresent(builder::studyUrl);

        return builder.build();
    }

}
