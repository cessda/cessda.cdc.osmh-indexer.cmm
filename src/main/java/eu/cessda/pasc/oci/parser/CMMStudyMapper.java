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
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;
import static eu.cessda.pasc.oci.parser.ParsingStrategies.*;

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
        oaiPmh = new AppConfigurationProperties.OaiPmh();
        var defaultLangSettings = new AppConfigurationProperties.OaiPmh.MetadataParsingDefaultLang();
        defaultLangSettings.setActive(true);
        defaultLangSettings.setLang("en");
        oaiPmh.setMetadataParsingDefaultLang(defaultLangSettings);
        oaiPmh.setConcatRepeatedElements(true);
        oaiPmh.setConcatSeparator("<br>");
        docElementParser = new DocElementParser(oaiPmh);
    }

    @Autowired
    public CMMStudyMapper(DocElementParser docElementParser, AppConfigurationProperties appConfigurationProperties) {
        this.docElementParser = docElementParser;
        this.oaiPmh = appConfigurationProperties.getOaiPmh();
    }

    /**
     * Extracts the Study Number from the header element
     * <p>
     * Original specified path can't be relied on (/codeBook/stdyDscr/citation/titlStmt/IDNo)
     * <ul>
     * <li>It may have multiple identifiers for different agency.</li>
     * <li>Where as The header will by default specify the unique code identifier for the repo(agency) we are querying</li>
     * </ul>
     * <p>
     * Actual path used: /record/header/identifier
     *
     * @param document the document to parse
     * @return the parsed {@link HeaderElement}
     * @throws OaiPmhException if the document contains an {@code <error>} element
     */
    HeaderElement parseHeaderElement(Document document) throws OaiPmhException {
        // Validate the document doesn't contain an error element
        DocElementParser.validateResponse(document);
        Optional<String> studyNumber = parseStudyNumber(document);
        Optional<String> lastModified = parseLastModified(document);
        boolean recordStatus = parseRecordStatus(document);
        return new HeaderElement(studyNumber.orElse(null), lastModified.orElse(null), recordStatus);
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
        var codeBookLang = DocElementParser.getFirstAttribute(document, xPaths.getRecordDefaultLanguage(), xPaths.getDdiNS());
        if (codeBookLang.isPresent() && !codeBookLang.get().getValue().trim().isEmpty()) {
            return codeBookLang.get().getValue().trim();
            // #192 - Per repository override of the default language
        } else if (repository.getDefaultLanguage() != null) {
            return repository.getDefaultLanguage();
        } else {
            return oaiPmh.getMetadataParsingDefaultLang().getLang();
        }
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getPidStudyXPath()}
     */
    Map<String, List<Pid>> parsePidStudies(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getPidStudyXPath(), xPaths.getDdiNS(), ParsingStrategies::pidStrategy);
    }

    /**
     * Parse records status.
     */
    private boolean parseRecordStatus(Document document) {
        Attribute status = XPathFactory.instance().compile(RECORD_STATUS_XPATH, Filters.attribute(), null, OAI_NS).evaluateFirst(document);
        return status == null || !status.getValue().equalsIgnoreCase("deleted");
    }

    /**
     * Parse last Modified.
     */
    private Optional<String> parseLastModified(Document document) {
        return DocElementParser.getFirstElement(document, LAST_MODIFIED_DATE_XPATH, OAI_NS).map(Element::getText);
    }

    /**
     * Parse study number.
     */
    private Optional<String> parseStudyNumber(Document document) {
        return DocElementParser.getFirstElement(document, IDENTIFIER_XPATH, OAI_NS).map(Element::getText);
    }

    /**
     * Parses Abstract
     * <p>
     * Xpath = {@link XPaths#getAbstractXPath() }
     */
    Map<String, String> parseAbstract(Document document, XPaths xPaths, String langCode) {
        return parseLanguageContentOfElement(document, langCode, xPaths.getAbstractXPath(), true, xPaths.getOaiAndDdiNs());
    }

    /**
     * Parses Year of Publication from:
     * <p>
     * Xpath = {@link XPaths#getYearOfPubXPath()}
     */
    Optional<String> parseYrOfPublication(Document document, XPaths xPaths) {
        return DocElementParser.getFirstAttribute(document, xPaths.getYearOfPubXPath(), xPaths.getDdiNS()).map(Attribute::getValue);
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getCreatorsXPath()}  }
     */
    Map<String, List<String>> parseCreator(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getCreatorsXPath(), xPaths.getDdiNS(), ParsingStrategies::creatorStrategy
        );
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@link XPaths#getClassificationsXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseClassifications(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getClassificationsXPath(), xPaths.getDdiNS(),
            element -> termVocabAttributeStrategy(element, xPaths.getDdiNS(), false)
        );
    }

    /**
     * Parses parseKeyword(s) from:
     * <p>
     * Xpath = {@link XPaths#getKeywordsXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseKeywords(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getKeywordsXPath(), xPaths.getDdiNS(),
            element -> termVocabAttributeStrategy(element, xPaths.getDdiNS(), false)
        );
    }

    /**
     * Parses Type Of Time Method(s) from:
     * <p>
     * Xpath = {@link XPaths#getTypeOfTimeMethodXPath()}  }
     */
    Map<String, List<TermVocabAttributes>> parseTypeOfTimeMethod(Document doc, XPaths xPaths, String defaultLangIsoCode) {

        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getTypeOfTimeMethodXPath(), xPaths.getDdiNS(),
            element -> termVocabAttributeStrategy(element, xPaths.getDdiNS(),true)
        );
    }

    /**
     * Parses Type Of Mode Of Collection(s) from:
     * <p>
     * Xpath = {@link XPaths#getTypeOfModeOfCollectionXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseTypeOfModeOfCollection(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getTypeOfModeOfCollectionXPath(), xPaths.getDdiNS(),
            element -> termVocabAttributeStrategy(element, xPaths.getDdiNS(), true)
        );
    }

    /**
     * Parses Unit Type(s) from:
     * <p>
     * Xpath = {@link XPaths#getUnitTypeXPath()}
     */
    Map<String, List<TermVocabAttributes>> parseUnitTypes(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getUnitTypeXPath(), xPaths.getDdiNS(),
            element -> termVocabAttributeStrategy(element, xPaths.getDdiNS(), true)
        );
    }

    /**
     * Parses Type Of Sampling Procedure(s) from:
     * <p>
     * Xpath = {@link XPaths#getSamplingXPath()}
     */
    Map<String, List<VocabAttributes>> parseTypeOfSamplingProcedure(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getSamplingXPath(), xPaths.getDdiNS(),
            element -> samplingTermVocabAttributeStrategy(element, xPaths.getDdiNS(), true)
        );
    }

    /**
     * Parses area Countries covered by a study:
     * <p>
     * Xpath = {@link XPaths#getStudyAreaCountriesXPath()}
     */
    Map<String, List<Country>> parseStudyAreaCountries(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getStudyAreaCountriesXPath(), xPaths.getDdiNS(),
            ParsingStrategies::countryStrategy
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
            defaultLang, document, xPaths.getPublisherXPath(), xPaths.getDdiNS(),
            ParsingStrategies::publisherStrategy
        );
        var distrPathMap = docElementParser.extractMetadataObjectForEachLang(
            defaultLang, document, xPaths.getDistributorXPath(), xPaths.getDdiNS(),
            ParsingStrategies::publisherStrategy
        );

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

        Map<String, String> titles = parseLanguageContentOfElement(document, langCode, xPaths.getTitleXPath(), false, xPaths.getOaiAndDdiNs());

        // https://github.com/cessda/cessda.cdc.versions/issues/135
        if (!titles.isEmpty()) {
            Map<String, String> parTitles = parseLanguageContentOfElement(document, langCode, xPaths.getParTitleXPath(), false, xPaths.getOaiAndDdiNs());
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

        var stdyDscrElements = DocElementParser.getElements(document, xPaths.getStudyURLStudyDscrXPath(), xPaths.getOaiAndDdiNs());
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
            var docDscrElement = DocElementParser.getElements(document, xpath, xPaths.getOaiAndDdiNs());
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
            defaultLangIsoCode, doc, xPaths.getSamplingXPath(), xPaths.getDdiNS(),
            ParsingStrategies::nullableElementValueStrategy
        );
    }

    /**
     * Parses Sampling Procedure(s) from:
     * <p>
     * Xpath = {@link XPaths#getDataRestrctnXPath()}
     */
    Map<String, List<String>> parseDataAccessFreeText(Document doc, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, xPaths.getDataRestrctnXPath(), xPaths.getDdiNS(),
            ParsingStrategies::nullableElementValueStrategy
        );
    }

    /**
     * Parses area Countries covered by a study:
     * <p>
     * Xpath = {@link XPaths#getDataCollectionPeriodsXPath()}
     */
    Map<String, List<DataCollectionFreeText>> parseDataCollectionFreeTexts(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getDataCollectionPeriodsXPath(), xPaths.getDdiNS(),
            ParsingStrategies::dataCollFreeTextStrategy
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
        var dateAttrs = DocElementParser.getDateElementAttributesValueMap(doc, xPaths.getDataCollectionPeriodsXPath(), xPaths.getDdiNS());

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
            .flatMap(xpath -> DocElementParser.getAttributeValues(document, xpath, xPaths.getDdiNS()).stream());

        var fileNameAttrsStream = xPaths.getFilenameLanguagesXPath().stream()
            .flatMap(xpath -> DocElementParser.getAttributeValues(document, xpath, xPaths.getDdiNS()).stream());

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
                xPaths.getDdiNS(),
                ParsingStrategies::universeStrategy
            );

            var universes = new HashMap<String, Universe>();
            for (var entry : extractedUniverses.entrySet()) {
                var universe = universes.computeIfAbsent(entry.getKey(), k -> new Universe());

                // Loop over all universe entries for each language
                for(var extractedUniverse : entry.getValue()) {
                    var universeContent = extractedUniverse.getValue();

                    // Switch based on the type of clusion
                    switch (extractedUniverse.getKey()) {
                        case I -> universe.setInclusion(universeContent);
                        case E -> universe.setExclusion(universeContent);
                    }
                }
            }

            return universes;
        } else {
            return Collections.emptyMap();
        }
    }

    Map<String, List<RelatedPublication>> parseRelatedPublications(Document document, XPaths xPaths, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, xPaths.getRelatedPublicationsXPath(), xPaths.getDdiNS(),
            element -> relatedPublicationsStrategy(element, xPaths.getDdiNS())
        );
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

    @Value
    static class ParseResults<T, E> {
        T results;
        E exceptions;
    }
}