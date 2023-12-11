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
package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.TimeUtility;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;
import static eu.cessda.pasc.oci.parser.ParsingStrategies.termVocabAttributeStrategy;
import static eu.cessda.pasc.oci.parser.ParsingStrategies.uriStrategy;

/**
 * Responsible for Mapping oai-pmh fields to a CMMStudy
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@Component
public class CMMStudyMapper {

    private final AppConfigurationProperties.OaiPmh oaiPmh;
    private final DocElementParser docElementParser;

    public CMMStudyMapper() {
        this.oaiPmh = new AppConfigurationProperties.OaiPmh(
            new AppConfigurationProperties.MetadataParsingDefaultLang(
                true,
                "en"
            ),
            "<br>"
        );
        this.docElementParser = new DocElementParser(this.oaiPmh);
    }

    @Autowired
    CMMStudyMapper(DocElementParser docElementParser, AppConfigurationProperties appConfigurationProperties) {
        this.docElementParser = docElementParser;
        this.oaiPmh = appConfigurationProperties.oaiPmh();
    }

    /**
     * Attempts to parse the default language from the given document.
     * <p>
     * This method will first attempt to find the language attribute at {@link XPaths#getRecordDefaultLanguage()}.
     * If this attribute doesn't exist, the default language of the repository will be used if set.
     * Otherwise, the global default language will be used.
     *
     * @param document   the OAI-PMH document to parse.
     * @param repository the repository used to set the default language.
     * @return the default language of the document.
     */
    String parseDefaultLanguage(Document document, Repo repository, XPaths xPaths) {
        var codeBookLang = DocElementParser.getFirstAttribute(document, xPaths.getRecordDefaultLanguage(), xPaths.getNamespace());
        if (codeBookLang.isPresent() && !codeBookLang.get().getValue().trim().isEmpty()) {
            return codeBookLang.get().getValue().trim();
            // #192 - Per repository override of the default language
        } else if (repository.defaultLanguage() != null) {
            return repository.defaultLanguage();
        } else {
            return oaiPmh.metadataParsingDefaultLang().lang();
        }
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getPidStudyXPath()}
     */
    Map<String, List<Pid>> parsePidStudies(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getPidStudyXPath(), ParsingStrategies::pidStrategy, xPaths.getNamespace());
    }

    /**
     * Parses Abstract
     * <p>
     * Xpath = {@link XPaths#getAbstractXPath() }
     */
    Map<String, String> parseAbstract(Document document, XPaths xPaths, String langCode) {
        return parseLanguageContentOfElement(document, langCode, xPaths.getAbstractXPath(), true, xPaths.getNamespace());
    }

    /**
     * Parses Year of Publication from:
     * <p>
     * Xpath = {@link XPaths#getYearOfPubXPath()}
     */
    Optional<String> parseYrOfPublication(Document document, XPaths xPaths) {
        return DocElementParser.getFirstAttribute(document, xPaths.getYearOfPubXPath(), xPaths.getNamespace()).map(Attribute::getValue);
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getCreatorsXPath()}
     */
    Map<String, List<String>> parseCreator(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getCreatorsXPath(), ParsingStrategies::creatorStrategy, xPaths.getNamespace()
        );
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getClassificationsXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseClassifications(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getClassificationsXPath(), element -> termVocabAttributeStrategy(element, false), xPaths.getNamespace()
        );
    }

    /**
     * Parses parseKeyword(s) from:
     * <p>
     * Xpath = {@link XPaths#getKeywordsXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseKeywords(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getKeywordsXPath(), element -> termVocabAttributeStrategy(element, false), xPaths.getNamespace()
        );
    }

    /**
     * Parses Type Of Time Method(s) from:
     * <p>
     * Xpath = {@link XPaths#getTypeOfTimeMethodXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseTypeOfTimeMethod(Document doc, XPaths xPaths, String defaultLangIsoCode) {

        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getTypeOfTimeMethodXPath(), element -> termVocabAttributeStrategy(element, true), xPaths.getNamespace()
        );
    }

    /**
     * Parses Type Of Mode Of Collection(s) from:
     * <p>
     * Xpath = {@link XPaths#getTypeOfModeOfCollectionXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseTypeOfModeOfCollection(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getTypeOfModeOfCollectionXPath(), element -> termVocabAttributeStrategy(element, true), xPaths.getNamespace()
        );
    }

    /**
     * Parses Unit Type(s) from:
     * <p>
     * Xpath = {@link XPaths#getUnitTypeXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseUnitTypes(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getUnitTypeXPath(), element -> termVocabAttributeStrategy(element, true), xPaths.getNamespace()
        );
    }

    /**
     * Parses Type Of Sampling Procedure(s) from:
     * <p>
     * Xpath = {@link XPaths#getSamplingXPath()}
     */
    Map<String, List<VocabAttributes>> parseTypeOfSamplingProcedure(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getSamplingXPath(), ParsingStrategies::samplingTermVocabAttributeStrategy, xPaths.getNamespace()
        );
    }

    /**
     * Parses area Countries covered by a study:
     * <p>
     * Xpath = {@link XPaths#getStudyAreaCountriesXPath()}
     */
    Map<String, List<Country>> parseStudyAreaCountries(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getStudyAreaCountriesXPath(), ParsingStrategies::countryStrategy, xPaths.getNamespace()
        );
    }

    /**
     * Parse Publisher from:
     * <p>
     * Xpath = {@link XPaths#getPublisherXPath()} and
     * Xpath = {@link XPaths#getDistributorXPath()}
     */
    Map<String, Publisher> parsePublisher(Document document, XPaths xPaths, String defaultLang) {
        var producerPathMap = docElementParser.extractMetadataObjectForEachLang(
            defaultLang, document, xPaths.getPublisherXPath(), ParsingStrategies::publisherStrategy, xPaths.getNamespace()
        );

        Map<String, Publisher> distrPathMap;
        if (xPaths.getDistributorXPath() != null) {
            distrPathMap = docElementParser.extractMetadataObjectForEachLang(
                defaultLang, document, xPaths.getDistributorXPath(), ParsingStrategies::publisherStrategy, xPaths.getNamespace()
            );
        } else {
            distrPathMap = Collections.emptyMap();
        }

        distrPathMap.forEach((k, v) -> producerPathMap.merge(k, v, (docDscrValue, stdyDscrValue) -> docDscrValue));
        return producerPathMap;
    }

    Map<String, String> parseLanguageContentOfElement(Document document, String langCode, String titleXpath, boolean isConcatenating, Namespace... namespaces) {
        var elements = DocElementParser.getElements(document, titleXpath, namespaces);
        return docElementParser.getLanguageKeyValuePairs(elements, isConcatenating, langCode, Element::getText);
    }

    /**
     * Parses Study Title.
     * <p>
     * Xpath = {@link XPaths#getTitleXPath()} and {@link XPaths#getParTitleXPath()}
     */
    Map<String, String> parseStudyTitle(Document document, XPaths xPaths, String langCode) {

        Map<String, String> titles = parseLanguageContentOfElement(document, langCode, xPaths.getTitleXPath(), false, xPaths.getNamespace());

        // https://github.com/cessda/cessda.cdc.versions/issues/135
        if (xPaths.getParTitleXPath() != null && !titles.isEmpty()) {
            Map<String, String> parTitles = parseLanguageContentOfElement(document, langCode, xPaths.getParTitleXPath(), false, xPaths.getNamespace());
            parTitles.forEach(titles::putIfAbsent);  // parTitl lang must not be same as or override titl lang

            // Remove return characters from the values
            titles.replaceAll((key, value) -> ParsingStrategies.cleanCharacterReturns(value));
        }
        return titles;
    }

    /**
     * Parses the Study Url from two plausible allowed xPaths
     * <p>
     * Xpath = {@link XPaths#getStudyURLDocDscrXPath()}
     * Xpath = {@link XPaths#getStudyURLStudyDscrXPath()}
     */
    ParseResults<HashMap<String, URI>, List<URISyntaxException>> parseStudyUrl(Document document, XPaths xPaths, String langCode) {
        var parsingExceptions = new ArrayList<URISyntaxException>();

        var stdyDscrElements = DocElementParser.getElements(document, xPaths.getStudyURLStudyDscrXPath(), xPaths.getNamespace());
        var urlFromStdyDscr = docElementParser.getLanguageKeyValuePairs(stdyDscrElements, langCode, element -> {
            try {
                return ParsingStrategies.uriStrategy(element);
            } catch (URISyntaxException e) {
                parsingExceptions.add(e);
                return Optional.empty();
            }
        });

        // If studyURLStudyDscrXPath defined, use that XPath as well.
        var studyUrls= xPaths.getStudyURLDocDscrXPath().map(xpath -> {
            var docDscrElement = DocElementParser.getElements(document, xpath, xPaths.getNamespace());
            var urlFromDocDscr = docElementParser.getLanguageKeyValuePairs(docDscrElement, langCode, element -> {
                try {
                    return ParsingStrategies.uriStrategy(element);
                } catch (URISyntaxException e) {
                    parsingExceptions.add(e);
                    return Optional.empty();
                }
            });

            // If absent, use the URL from studyDscr
            urlFromStdyDscr.forEach(urlFromDocDscr::putIfAbsent);
            return urlFromDocDscr;
        }).orElse(urlFromStdyDscr);

        return new ParseResults<>(studyUrls, parsingExceptions);
    }

    /**
     * Parses Sampling Procedure(s) from:
     * <p>
     * Xpath = {@link XPaths#getSamplingXPath()}
     */
    Map<String, List<String>> parseSamplingProcedureFreeTexts(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getSamplingXPath(), ParsingStrategies::nullableElementValueStrategy, xPaths.getNamespace()
        );
    }

    /**
     * Parses Sampling Procedure(s) from:
     * <p>
     * Xpath = {@link XPaths#getDataRestrctnXPath()}
     */
    Map<String, List<String>> parseDataAccessFreeText(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getDataRestrctnXPath(), ParsingStrategies::nullableElementValueStrategy, xPaths.getNamespace()
        );
    }

    /**
     * Parses area Countries covered by a study:
     * <p>
     * Xpath = {@link XPaths#getDataCollectionPeriodsXPath()}
     */
    Map<String, List<DataCollectionFreeText>> parseDataCollectionFreeTexts(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getDataCollectionPeriodsXPath(), ParsingStrategies::dataCollFreeTextStrategy, xPaths.getNamespace()
        );
    }

    /**
     * Parses Data Collection Period dates from:
     * <p>
     * Xpath = {@link XPaths#getDataCollectionPeriodsXPath()}
     * <p>
     * For Data Collection start and end date plus the four digit Year value as Data Collection Year
     */
    ParseResults<DataCollectionPeriod, List<DateNotParsedException>> parseDataCollectionDates(Document doc, XPaths xPaths) {
        var dateAttrs = DocElementParser.getDateElementAttributesValueMap(doc, xPaths.getDataCollectionPeriodsXPath(), xPaths.getNamespace());

        var dataCollectionPeriodBuilder = DataCollectionPeriod.builder();

        var parseExceptions = new ArrayList<DateNotParsedException>(2);

        if (dateAttrs.containsKey(SINGLE_ATTR)) {
            final String singleDateValue = dateAttrs.get(SINGLE_ATTR);
            dataCollectionPeriodBuilder.startDate(singleDateValue);
            try {
                var localDateTime = TimeUtility.getLocalDateTime(singleDateValue);
                dataCollectionPeriodBuilder.dataCollectionYear(localDateTime.getYear());
            } catch (DateNotParsedException e) {
                parseExceptions.add(e);
            }
        } else {
            if (dateAttrs.containsKey(START_ATTR)) {
                final String startDateValue = dateAttrs.get(START_ATTR);
                dataCollectionPeriodBuilder.startDate(startDateValue);
                try {
                    var localDateTime = TimeUtility.getLocalDateTime(startDateValue);
                    dataCollectionPeriodBuilder.dataCollectionYear(localDateTime.getYear());
                } catch (DateNotParsedException e) {
                    parseExceptions.add(e);
                }
            }
            if (dateAttrs.containsKey(END_ATTR)) {
                dataCollectionPeriodBuilder.endDate(dateAttrs.get(END_ATTR));
            }
        }

        return new ParseResults<>(
            dataCollectionPeriodBuilder.build(),
            parseExceptions
        );
    }

    /**
     * Parses File Language(s) from:
     * <p>
     * Xpath = {@link XPaths#getFileTxtLanguagesXPath() }
     *
     * @return a set with all the file languages
     */
    Set<String> parseFileLanguages(Document document, XPaths xPaths) {

        var fileTxtAttrsStream = xPaths.getFileTxtLanguagesXPath().stream()
            .flatMap(xpath -> DocElementParser.getAttributeValues(document, xpath, xPaths.getNamespace()).stream());

        var fileNameAttrsStream = xPaths.getFilenameLanguagesXPath().stream()
            .flatMap(xpath -> DocElementParser.getAttributeValues(document, xpath, xPaths.getNamespace()).stream());

        return Stream.concat(fileTxtAttrsStream, fileNameAttrsStream).collect(Collectors.toSet());
    }

    /**
     * Parses universes from:
     * <p>
     * Xpath = {@link XPaths#getUniverseXPath()}
     * <p>
     *
     * @return a map with the key set to the language, and the value a list of universes found.
     */
    @SuppressWarnings({"java:S1301", "java:S131"}) // Suppress false positives
    Map<String, Universe> parseUniverses(Document document, XPaths xPaths, String defaultLangIsoCode) {
        var universeXPath = xPaths.getUniverseXPath();
        if (universeXPath.isPresent()) {
            var extractedUniverses = docElementParser.extractMetadataObjectListForEachLang(
                defaultLangIsoCode,
                document,
                universeXPath.orElseThrow(),
                ParsingStrategies::universeStrategy, xPaths.getNamespace()
            );

            var universes = new HashMap<String, Universe>();
            for (var entry : extractedUniverses.entrySet()) {
                universes.compute(entry.getKey(), (k, universe) -> {
                    if (universe == null) {
                        // Empty universe to be copied in the switch expression
                        universe = new Universe(null, null);
                    }

                    // Loop over all universe entries for each language
                    for (var extractedUniverse : entry.getValue()) {
                        var content = extractedUniverse.getValue();

                        // Switch based on whether the universe is included or excluded,
                        // copying in any previous inclusions or exclusions
                        universe = switch (extractedUniverse.getKey()) {
                            case I -> new Universe(content, universe.exclusion());
                            case E -> new Universe(universe.inclusion(), content);
                        };
                    }

                    return universe;
                });
            }

            return Collections.unmodifiableMap(universes);
        } else {
            return Collections.emptyMap();
        }
    }

    Map<String, List<RelatedPublication>> parseRelatedPublications(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getRelatedPublicationsXPath(), ParsingStrategies::relatedPublicationsStrategy, xPaths.getNamespace()
        );
    }

    ParseResults<Map<String, URI>, List<URISyntaxException>> parseDataAccessURI(Document document, XPaths xPaths, String defaultLangIsoCode) {
        var dataAccessUrlXPath = xPaths.getDataAccessUrlXPath();
        if (dataAccessUrlXPath.isPresent()) {
            var parsingExceptions = new ArrayList<URISyntaxException>();
            var elements = DocElementParser.getElements(document, dataAccessUrlXPath.get(), xPaths.getNamespace());

            var parsingUri = docElementParser.getLanguageKeyValuePairs(
                elements, defaultLangIsoCode,
                element -> {
                    try {
                        return uriStrategy(element);
                    } catch (URISyntaxException e) {
                        parsingExceptions.add(e);
                        return Optional.empty();
                    }
                }
            );

            return new ParseResults<>(parsingUri, parsingExceptions);
        } else {
            return new ParseResults<>(Collections.emptyMap(), Collections.emptyList());
        }
    }

    @Value
    public static class HeaderElement {
        String studyNumber;
        String lastModified;
        boolean recordActive;

        public Optional<String> getStudyNumber() {
            return Optional.ofNullable(studyNumber);
        }

        public Optional<String> getLastModified() {
            return Optional.ofNullable(lastModified);
        }
    }

    @Builder
    @Value
    public static class DataCollectionPeriod {
        String startDate;
        int dataCollectionYear;
        String endDate;

        public Optional<String> getStartDate() {
            return Optional.ofNullable(startDate);
        }

        public Optional<String> getEndDate() {
            return Optional.ofNullable(endDate);
        }
    }

    record ParseResults<T, E>(T results, E exceptions) {
    }
}
