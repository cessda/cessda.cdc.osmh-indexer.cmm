/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.models.DataAccessMapping;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cessda.pasc.oci.parser.XMLMapper.extractMetadataObjectListForEachLang;

/**
 * Responsible for Mapping oai-pmh fields to a CMMStudy
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@Component
public class CMMStudyMapper {

    private final AppConfigurationProperties.OaiPmh oaiPmh;
    private final Map<String, Map<String, List<DataAccessMapping>>> dataAccessMappings;

    public CMMStudyMapper() throws IOException {
        this.oaiPmh = new AppConfigurationProperties.OaiPmh(
            new AppConfigurationProperties.MetadataParsingDefaultLang(
                true,
                "en"
            ),
            "<br>"
        );
        this.dataAccessMappings = loadDataMappings(new ObjectMapper());
    }

    @Autowired
    CMMStudyMapper(AppConfigurationProperties appConfigurationProperties, ObjectMapper objectMapper) throws IOException {
        this.oaiPmh = appConfigurationProperties.oaiPmh();

        // Load the Data Access mapping JSON file
        this.dataAccessMappings = loadDataMappings(objectMapper);
    }

    private static Map<String, Map<String, List<DataAccessMapping>>> loadDataMappings(ObjectMapper objectMapper) throws IOException {
        try (InputStream inputStream = ResourceHandler.getResourceAsStream("data_access_mappings.json")) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
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
        XPathExpression<Attribute> expression = XPathFactory.instance().compile(xPaths.getRecordDefaultLanguage(), Filters.attribute(), null, xPaths.getNamespace());
        var attributes = expression.evaluate(document);
        var codeBookLang = attributes.stream().findFirst();
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
     * Merge the given lists into a single list. The source lists are not modified.
     */
    @NonNull
    private static <T> List<T> mergeLists(List<T> a, List<T> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }

    /**
     * Map {@code} null keys (i.e. elements missing {@code xml:lang}) to the default language,
     * or remove them if default language mapping is disabled.
     * <p>
     * If both the default language and {@code null} entries exist, the default language entries
     * will override the {@code null} entries.
     *
     * @param langMap            the map.
     * @param defaultLangIsoCode the language to map to.
     */
    private <T> Map<String, T> mapNullLanguage(Map<String, T> langMap, String defaultLangIsoCode) {
        return mapNullLanguage(langMap, defaultLangIsoCode, (a, b) -> a);
    }

    /**
     * Map {@code} null keys (i.e. elements missing {@code xml:lang}) to the default language,
     * or remove them if default language mapping is disabled.
     *
     * @param langMap            the map.
     * @param defaultLangIsoCode the language to map to.
     * @param mergeOperator      the operation to run on merge conflicts
     */
    private <T> Map<String, T> mapNullLanguage(Map<String, T> langMap, String defaultLangIsoCode, BinaryOperator<T> mergeOperator) {
        var hasEmptyLangContent = langMap.containsKey(XMLMapper.EMPTY_LANGUAGE);
        if (hasEmptyLangContent && oaiPmh.metadataParsingDefaultLang().active()) {
            // Create a new map to store the result in
            var newMap = new HashMap<>(langMap);

            // Extract the empty value and merge with the default language value
            var emptyLangContentValue = newMap.remove(XMLMapper.EMPTY_LANGUAGE);
            newMap.merge(defaultLangIsoCode, emptyLangContentValue, mergeOperator);

            return newMap;
        } else {
            return langMap;
        }
    }

    /**
     * Parses Abstract
     * <p>
     * Xpath = {@link XPaths#getAbstractXPath() }
     */
    Map<String, String> parseAbstract(Document document, XPaths xPaths, String langCode) {
        var unmappedAbstracts = xPaths.getAbstractXPath().resolve(document, xPaths.getNamespace());
        return mapNullLanguage(unmappedAbstracts, langCode);
    }

    /**
     * Parses Year of Publication from:
     * <p>
     * Xpath = {@link XPaths#getYearOfPubXPath()}
     */
    Optional<String> parseYrOfPublication(Document document, XPaths xPaths) {
        return xPaths.getYearOfPubXPath().resolve(document, xPaths.getNamespace());
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getPidStudyXPath()}
     */
    Map<String, List<Pid>> parsePidStudies(Document document, XPaths xPaths) {
        return xPaths.getPidStudyXPath().resolve(document, xPaths.getNamespace());
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getCreatorsXPath()}
     */
    Map<String, List<Creator>> parseCreator(Document document, XPaths xPaths) {
        return xPaths.getCreatorsXPath().resolve(document, xPaths.getNamespace());
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getClassificationsXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseClassifications(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getClassificationsXPath().resolve(doc, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parses parseKeyword(s) from:
     * <p>
     * Xpath = {@link XPaths#getKeywordsXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseKeywords(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getKeywordsXPath().resolve(doc, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parses Type Of Time Method(s) from:
     * <p>
     * Xpath = {@link XPaths#getTypeOfTimeMethodXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseTypeOfTimeMethod(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getTypeOfTimeMethodXPath().resolve(doc, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parses Type Of Mode Of Collection(s) from:
     * <p>
     * Xpath = {@link XPaths#getTypeOfModeOfCollectionXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseTypeOfModeOfCollection(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getTypeOfModeOfCollectionXPath().resolve(doc, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parses Unit Type(s) from:
     * <p>
     * Xpath = {@link XPaths#getUnitTypeXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseUnitTypes(Document document, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getUnitTypeXPath().resolve(document, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parses Type Of Sampling Procedure(s) from:
     * <p>
     * Xpath = {@link XPaths#getSamplingXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseTypeOfSamplingProcedure(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getSamplingXPath().resolve(doc, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parses area Countries covered by a study:
     * <p>
     * Xpath = {@link XPaths#getStudyAreaCountriesXPath()}
     */
    Map<String, List<Country>> parseStudyAreaCountries(Document document, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getStudyAreaCountriesXPath().resolve(document, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parse Publisher from:
     * <p>
     * Xpath = {@link XPaths#getPublisherXPath()} and
     * Xpath = {@link XPaths#getDistributorXPath()}
     */
    Map<String, Publisher> parsePublisher(Document document, XPaths xPaths, String defaultLang) {
        var producerPathMap = mapNullLanguage(xPaths.getPublisherXPath().resolve(document, xPaths.getNamespace()), defaultLang);

        if (xPaths.getDistributorXPath() != null) {
            var distrPathMap = mapNullLanguage(xPaths.getDistributorXPath().resolve(document, xPaths.getNamespace()), defaultLang);
            distrPathMap.forEach(producerPathMap::putIfAbsent);
        }

        return producerPathMap;
    }

    /**
     * Parses Study Title.
     * <p>
     * Xpath = {@link XPaths#getTitleXPath()} and {@link XPaths#getParTitleXPath()}
     */
    Map<String, String> parseStudyTitle(Document document, XPaths xPaths, String langCode) {
        Map<String, String> titles = mapNullLanguage(xPaths.getTitleXPath().resolve(document, xPaths.getNamespace()), langCode);

        // https://github.com/cessda/cessda.cdc.versions/issues/135
        var xpathOptional = xPaths.getParTitleXPath();
        if (xpathOptional.isPresent() && !titles.isEmpty()) {
            var parTitles = mapNullLanguage(xpathOptional.get().resolve(document, xPaths.getNamespace()), langCode);
            parTitles.forEach(titles::putIfAbsent);  // parTitl lang must not be same as or override titl lang

            // Remove return characters from the values
            titles.replaceAll((key, value) -> ParsingStrategies.cleanCharacterReturns(value));
        }

        return mapNullLanguage(titles, langCode);
    }

    /**
     * Parses the Study Url from two plausible allowed xPaths
     * <p>
     * Xpath = {@link XPaths#getStudyURLXPath()}
     */
    @SuppressWarnings("java:S3776") // Extracting parts of the method will increase complexity
    ParseResults<Map<String, URI>, List<URISyntaxException>> parseStudyUrl(Document document, XPaths xPaths) {

        var studyURLs = new HashMap<String, URI>();
        var parsingExceptions = new ArrayList<URISyntaxException>();

        // If studyURLStudyDscrXPath defined, use that XPath as well.
        xPaths.getStudyURLXPath().ifPresent(xpath -> {
            var docUriStrings = xpath.resolve(document, xPaths.getNamespace());

            for (var uriStrings : docUriStrings.entrySet()) {
                for (var uriString : uriStrings.getValue()) {
                    try {
                        var uri = new URI(uriString);
                        studyURLs.put(uriStrings.getKey(), uri);
                        break;
                    } catch(URISyntaxException e){
                        parsingExceptions.add(e);
                    }
                }
            }
        });

        return new ParseResults<>(studyURLs, parsingExceptions);
    }

    /**
     * Parses Data Access to be Open / Restricted if possible, otherwise returns Uncategorized.
     * <p>
     * Xpath = {@link XPaths#getDataAccessXPath()}, {@link XPaths#getDataAccessAltXPath()} and {@link XPaths#getDataRestrctnXPath()}
     * <p>
     */
    String parseDataAccess(Document doc, XPaths xPaths, String defaultLangIsoCode, String repository) {
        var dataAccess = xPaths.getDataAccessXPath().resolve(doc, xPaths.getNamespace());

        if (dataAccess == null) {
            // Try deriving from free text - check if repository can be found in mappings file
            var repositoryNode = dataAccessMappings.get(repository);
            if (repositoryNode != null) {
                for (var entry : repositoryNode.entrySet()) {
                    // Get the key (short form of XPath) and value (values to map to Open / Restricted)
                    String xpathKey = entry.getKey();

                    // Copy data access to a map for more optimised comparisons
                    var dataAccessMap = new HashMap<String, DataAccessMapping.AccessCategory>();
                    for (var dataAccessMapping : entry.getValue()) {
                        dataAccessMap.put(dataAccessMapping.content(), dataAccessMapping.accessCategory());
                    }

                    // Resolve the corresponding XPath for the repository
                    Map<String, List<String>> resolvedMap = Collections.emptyMap();
                    if ("dataRestrctnXPath".equals(xpathKey)) {
                        resolvedMap = parseDataAccessFreeText(doc, xPaths, defaultLangIsoCode);
                    } else if ("dataAccessAltXPath".equals(xpathKey)) {
                        var dataAccessAltXPath = new SimpleXMLMapper<>("//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:specPerm", extractMetadataObjectListForEachLang(ParsingStrategies::nullableElementValueStrategy));
                        resolvedMap = dataAccessAltXPath.resolve(doc, xPaths.getNamespace());
                    }

                    // Check if the map has entries, and if so, iterate through the list and compare each value separately
                    for (Map.Entry<String, List<String>> resolvedEntry : resolvedMap.entrySet()) {
                        for (String resolvedValue : resolvedEntry.getValue()) {
                            var match = dataAccessMap.get(resolvedValue);
                            if (match != null) {
                                return match.name();
                            }
                        }
                    }
                }
            }
            // If data access is null and no mapping is found, return Uncategorized
            return "Uncategorized";
        }

        return dataAccess;
    }

    /**
     * Parses Data Access(es) from:
     * <p>
     * Xpath = {@link XPaths#getDataRestrctnXPath()}
     */
    Map<String, List<String>> parseDataAccessFreeText(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedDataAccessTexts = xPaths.getDataRestrctnXPath().resolve(doc, xPaths.getNamespace());
        return mapNullLanguage(unmappedDataAccessTexts, defaultLangIsoCode);
    }

    /**
     * Parses Data Collection Period dates from:
     * <p>
     * Xpath = {@link XPaths#getDataCollectionPeriodsXPath()}
     * <p>
     * For Data Collection start and end date plus the four digit Year value as Data Collection Year
     */
    ParseResults<DataCollectionPeriod, List<DateTimeParseException>> parseDataCollectionDates(Document doc, XPaths xPaths) {
        var parseResults = xPaths.getDataCollectionPeriodsXPath().resolve(doc, xPaths.getNamespace());
        return new ParseResults<>(parseResults.results, parseResults.exceptions);
    }

    /**
     * Parses File Language(s) from:
     * <p>
     * Xpath = {@link XPaths#getFileTxtLanguagesXPath() }
     *
     * @return a set with all the file languages
     */
    Set<String> parseFileLanguages(Document document, XPaths xPaths) {

        var fileTxtAttrsStream = xPaths.getFileTxtLanguagesXPath().stream();
        var fileNameAttrsStream = xPaths.getFilenameLanguagesXPath().stream();

        return Stream.concat(fileTxtAttrsStream, fileNameAttrsStream)
            .flatMap(xpath -> xpath.resolve(document, xPaths.getNamespace()).stream())
            .filter(lang -> !lang.equals(XMLMapper.EMPTY_LANGUAGE)) // Filter out empty language results
            .collect(Collectors.toSet());
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
            var extractedUniverses = mapNullLanguage(universeXPath.get().resolve(document, xPaths.getNamespace()), defaultLangIsoCode, (a, b) -> { a.addAll(b); return a; });

            var universes = new HashMap<String, Universe>();
            for (var entry : extractedUniverses.entrySet()) {
                universes.compute(entry.getKey(), (k, universe) -> {
                    if (universe == null) {
                        // Empty universe to be copied in the switch expression
                        universe = new Universe(null, null);
                    }

                    // Loop over all universe entries for each language
                    for (var extractedUniverse : entry.getValue()) {
                        var content = extractedUniverse.content();

                        // Switch based on whether the universe is included or excluded,
                        // copying in any previous inclusions or exclusions
                        universe = switch (extractedUniverse.clusion()) {
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
        var unmappedXPaths = xPaths.getRelatedPublicationsXPath().resolve(document, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode);
    }

    ParseResults<Map<String, URI>, List<URISyntaxException>> parseDataAccessURI(Document document, XPaths xPaths, String defaultLangIsoCode) {
        var dataAccessUrlXPath = xPaths.getDataAccessUrlXPath();
        if (dataAccessUrlXPath.isPresent()) {
            var parsingExceptions = new ArrayList<URISyntaxException>();
            var urlStrings = mapNullLanguage(dataAccessUrlXPath.get().resolve(document, xPaths.getNamespace()), defaultLangIsoCode);
            var parsingUri = new HashMap<String, URI>(urlStrings.size());

            for (var s : urlStrings.entrySet()) {
                for (var u : s.getValue()) {
                    try {
                        parsingUri.put(s.getKey(), new URI(u));
                        break;
                    } catch (URISyntaxException e) {
                        parsingExceptions.add(e);
                    }
                }
            }

            return new ParseResults<>(parsingUri, parsingExceptions);
        } else {
            return new ParseResults<>(Collections.emptyMap(), Collections.emptyList());
        }
    }

    Map<String, List<Funding>> parseFunding(Document document, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getFundingXPath().resolve(document, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parses Data Kind(s) from:
     * <p>
     * Xpath = {@link XPaths#getDataKindXPath()}
     */
    Map<String, List<DataKindFreeText>> parseDataKindFreeText(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getDataKindXPath().resolve(doc, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
    }

    /**
     * Parses General Data Format(s) from:
     * <p>
     * Xpath = {@link XPaths#getGeneralDataFormatXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseGeneralDataFormats(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        var generalDataFormatXPath = xPaths.getGeneralDataFormatXPath();
        if (generalDataFormatXPath.isPresent()) {
            var unmappedXPaths = generalDataFormatXPath.get().resolve(doc, xPaths.getNamespace());
            return mapNullLanguage(unmappedXPaths, defaultLangIsoCode, CMMStudyMapper::mergeLists);
        } else {
            return Collections.emptyMap();
        }
    }

    @Builder
    @Value
    public static class DataCollectionPeriod {
        String startDate;
        Integer dataCollectionYear;
        String endDate;
        Map<String, List<DataCollectionFreeText>> freeTexts;

        public Optional<Integer> getDataCollectionYear() {
            return Optional.ofNullable(dataCollectionYear);
        }

        public Optional<String> getStartDate() {
            return Optional.ofNullable(startDate);
        }

        public Optional<String> getEndDate() {
            return Optional.ofNullable(endDate);
        }
    }

    record ParseResults<T, E>(T results, E exceptions) {
    }

    /**
     * Parses Series from:
     * <p>
     * Xpath = {@link XPaths#getSeriesXPath()}
     */
    Map<String, List<Series>> parseSeries(Document document, XPaths xPaths, String defaultLangIsoCode) {
        var unmappedXPaths = xPaths.getSeriesXPath().resolve(document, xPaths.getNamespace());
        return mapNullLanguage(unmappedXPaths, defaultLangIsoCode);
    }
}
