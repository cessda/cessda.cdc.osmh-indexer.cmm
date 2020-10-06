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
package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.models.oai.configuration.MetadataParsingDefaultLang;
import eu.cessda.pasc.oci.models.oai.configuration.OaiPmh;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;
import static eu.cessda.pasc.oci.parser.ParsingStrategies.samplingTermVocabAttributeStrategy;
import static eu.cessda.pasc.oci.parser.ParsingStrategies.termVocabAttributeStrategy;
import static eu.cessda.pasc.oci.parser.TimeUtility.parseYearFromDateString;

/**
 * Responsible for Mapping oai-pmh fields to a CMMStudy
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@Component
public class CMMStudyMapper {

    private final OaiPmh oaiPmh;
    private final DocElementParser docElementParser;
    private final XPathFactory xFactory = XPathFactory.instance();

    public CMMStudyMapper() {
        oaiPmh = new OaiPmh();
        var defaultLangSettings = new MetadataParsingDefaultLang();
        defaultLangSettings.setActive(true);
        defaultLangSettings.setLang("en");
        oaiPmh.setMetadataParsingDefaultLang(defaultLangSettings);
        oaiPmh.setConcatRepeatedElements(true);
        oaiPmh.setConcatSeparator("+<br>");
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
     * Original specified path cant be relied on (/codeBook/stdyDscr/citation/titlStmt/IDNo)
     * <ul>
     * <li>It may have multiple identifiers for different agency.</li>
     * <li>Where as The header will by default specify the unique code identifier
     * for the repo(agency) we are querying
     * </li>
     * <p>
     * </ul>
     * Actual path used: /record/header/identifier
     *
     * @param document the document to parse
     * @return true if record is active
     * @throws OaiPmhException if the document contains an {@code <error>} element
     */
    public HeaderElement parseHeaderElement(Document document) throws OaiPmhException {
        // Validate the document doesn't contain an error element
        docElementParser.validateResponse(document);
        Optional<String> studyNumber = parseStudyNumber(document);
        Optional<String> lastModified = parseLastModified(document);
        boolean recordStatus = parseRecordStatus(document);
        return new HeaderElement(studyNumber.orElse(null), lastModified.orElse(null), recordStatus);
    }

    public String parseDefaultLanguage(Document document, Repo repository) {
        XPathExpression<Attribute> attributeExpression = xFactory
            .compile(RECORD_DEFAULT_LANGUAGE_XPATH, Filters.attribute(), null, OAI_AND_DDI_NS);
        Optional<Attribute> codeBookLang = Optional.ofNullable(attributeExpression.evaluateFirst(document));
        if (codeBookLang.isPresent() && !codeBookLang.get().getValue().trim().isEmpty()) {
            return codeBookLang.get().getValue().trim();
            // #192 - Per repository override of the default language
        } else if (repository.getDefaultLanguage() != null) {
            return repository.getDefaultLanguage();
        }
        return oaiPmh.getMetadataParsingDefaultLang().getLang();
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#PID_STUDY_XPATH }
     */
    public Map<String, List<Pid>> parsePidStudies(Document document, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, PID_STUDY_XPATH, ParsingStrategies::pidStrategy);
    }

    /**
     * Parse records status.
     */
    private boolean parseRecordStatus(Document document) {
        Attribute status = xFactory.compile(RECORD_STATUS_XPATH, Filters.attribute(), null, OAI_NS).evaluateFirst(document);
        return status == null || !status.getValue().equalsIgnoreCase("deleted");
    }

    /**
     * Parse last Modified.
     */
    private Optional<String> parseLastModified(Document document) {
        return docElementParser.getFirstElement(document, LAST_MODIFIED_DATE_XPATH).map(Element::getText);
    }

    /**
     * Parse study number.
     */
    private Optional<String> parseStudyNumber(Document document) {
        return docElementParser.getFirstElement(document, IDENTIFIER_XPATH).map(Element::getText);
    }

    /**
     * Parses Abstract
     * <p>
     * Xpath = {@value OaiPmhConstants#ABSTRACT_XPATH }
     */
    public Map<String, String> parseAbstract(Document document, String langCode) {
        return parseLanguageContentOfElement(document, langCode, ABSTRACT_XPATH, true);
    }

    /**
     * Parses Year of Publication from:
     * <p>
     * Xpath = {@value OaiPmhConstants#YEAR_OF_PUB_XPATH }
     */
    public Optional<String> parseYrOfPublication(Document document) {
        return docElementParser.getFirstAttribute(document, YEAR_OF_PUB_XPATH).map(Attribute::getValue);
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#CREATORS_XPATH }
     */
    public Map<String, List<String>> parseCreator(Document doc, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, CREATORS_XPATH, ParsingStrategies::creatorStrategy);
    }

    /**
     * Parses PID Study(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#CLASSIFICATIONS_XPATH }
     */
    public Map<String, List<TermVocabAttributes>> parseClassifications(Document doc, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, CLASSIFICATIONS_XPATH,
            element -> termVocabAttributeStrategy(element, false));
    }

    /**
     * Parses parseKeyword(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#KEYWORDS_XPATH }
     */
    public Map<String, List<TermVocabAttributes>> parseKeywords(Document doc, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, KEYWORDS_XPATH,
            element -> termVocabAttributeStrategy(element, false));
    }

    /**
     * Parses Type Of Time Method(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#TYPE_OF_TIME_METHOD_XPATH }
     */
    public Map<String, List<TermVocabAttributes>> parseTypeOfTimeMethod(Document doc, String defaultLangIsoCode) {

        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, TYPE_OF_TIME_METHOD_XPATH,
            element -> termVocabAttributeStrategy(element, true));
    }

    /**
     * Parses Type Of Mode Of Collection(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#TYPE_OF_MODE_OF_COLLECTION_XPATH }
     */
    public Map<String, List<TermVocabAttributes>> parseTypeOfModeOfCollection(Document doc, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, TYPE_OF_MODE_OF_COLLECTION_XPATH,
            element -> termVocabAttributeStrategy(element, true));
    }

    /**
     * Parses Unit Type(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#UNIT_TYPE_XPATH }
     */
    public Map<String, List<TermVocabAttributes>> parseUnitTypes(Document document, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, UNIT_TYPE_XPATH,
            element -> termVocabAttributeStrategy(element, true));
    }

    /**
     * Parses Type Of Sampling Procedure(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#TYPE_OF_SAMPLING_XPATH }
     */
    public Map<String, List<VocabAttributes>> parseTypeOfSamplingProcedure(Document doc, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(defaultLangIsoCode, doc, TYPE_OF_SAMPLING_XPATH,
            element -> samplingTermVocabAttributeStrategy(element, true));
    }

    /**
     * Parses area Countries covered by a study:
     * <p>
     * Xpath = {@value OaiPmhConstants#STUDY_AREA_COUNTRIES_XPATH }
     */
    public Map<String, List<Country>> parseStudyAreaCountries(Document document, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, STUDY_AREA_COUNTRIES_XPATH, ParsingStrategies::countryStrategy);
    }

    /**
     * Parse Publisher from:
     * <p>
     * Xpath = {@value OaiPmhConstants#PUBLISHER_XPATH } and
     * Xpath = {@value OaiPmhConstants#DISTRIBUTOR_XPATH }
     */
    public Map<String, Publisher> parsePublisher(Document document, String defaultLang) {
        Map<String, Publisher> producerPathMap = docElementParser.extractMetadataObjectForEachLang(defaultLang, document,
            PUBLISHER_XPATH, ParsingStrategies::publisherStrategy);
        Map<String, Publisher> distrPathMap = docElementParser.extractMetadataObjectForEachLang(defaultLang, document,
            DISTRIBUTOR_XPATH, ParsingStrategies::publisherStrategy);

        Map<String, Publisher> mergedStudyUrls = new HashMap<>(producerPathMap);
        distrPathMap.forEach((k, v) -> mergedStudyUrls.merge(k, v, (docDscrValue, stdyDscrValue) -> docDscrValue));
        return mergedStudyUrls;
    }

    private Map<String, String> parseLanguageContentOfElement(Document document, String langCode, String titleXpath, boolean isConcatenating) {
        List<Element> elements = docElementParser.getElements(document, titleXpath);
        return docElementParser.getLanguageKeyValuePairs(elements, isConcatenating, langCode, Element::getText);
    }

    /**
     * Parses Study Title.
     * <p>
     * Xpath = {@value OaiPmhConstants#TITLE_XPATH } and {@value OaiPmhConstants#PAR_TITLE_XPATH }
     */
    public Map<String, String> parseStudyTitle(Document document, String langCode) {

        Map<String, String> titles = parseLanguageContentOfElement(document, langCode, TITLE_XPATH, false);

        // https://bitbucket.org/cessda/cessda.cdc.version2/issues/135
        if (!titles.isEmpty()) {
            Map<String, String> parTitles = parseLanguageContentOfElement(document, langCode, PAR_TITLE_XPATH, false);
            parTitles.forEach(titles::putIfAbsent);  // parTitl lang must not be same as or override titl lang

            // Remove return characters from the values
            titles.replaceAll((key, value) -> HTMLFilter.cleanCharacterReturns(value));
        }
        return titles;
    }

    /**
     * Parses parse Study Url from two plausible allowed xPaths
     * <p>
     * Xpath = {@value OaiPmhConstants#STUDY_URL_DOC_DSCR_XPATH }
     * Xpath = {@value OaiPmhConstants#STUDY_URL_STDY_DSCR_XPATH }
     */
    public Map<String, String> parseStudyUrl(Document document, String langCode) {

        var docDscrElement = docElementParser.getElements(document, STUDY_URL_DOC_DSCR_XPATH);
        var stdyDscrElements = docElementParser.getElements(document, STUDY_URL_STDY_DSCR_XPATH);
        var urlFromDocDscr = docElementParser.getLanguageKeyValuePairs(docDscrElement, false, langCode, ParsingStrategies::uriStrategy);
        var urlFromStdyDscr = docElementParser.getLanguageKeyValuePairs(stdyDscrElements, false, langCode, ParsingStrategies::uriStrategy);

        var mergedStudyUrls = new HashMap<>(urlFromDocDscr);

        // If absent, use the URL from mergedStudyUrls
        urlFromStdyDscr.forEach(mergedStudyUrls::putIfAbsent);

        return mergedStudyUrls;
    }

    /**
     * Parses Sampling Procedure(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#TYPE_OF_SAMPLING_XPATH }
     */
    public Map<String, List<String>> parseSamplingProcedureFreeTexts(Document doc, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, TYPE_OF_SAMPLING_XPATH, ParsingStrategies::nullableElementValueStrategy);
    }

    /**
     * Parses Sampling Procedure(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#TYPE_OF_SAMPLING_XPATH }
     */
    public Map<String, List<String>> parseDataAccessFreeText(Document doc, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, doc, DATA_RESTRCTN_XPATH, ParsingStrategies::nullableElementValueStrategy);
    }

    /**
     * Parses area Countries covered by a study:
     * <p>
     * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH }
     */
    public Map<String, List<DataCollectionFreeText>> parseDataCollectionFreeTexts(Document document, String defaultLangIsoCode) {
        return docElementParser.extractMetadataObjectListForEachLang(
            defaultLangIsoCode, document, DATA_COLLECTION_PERIODS_PATH, ParsingStrategies::dataCollFreeTextStrategy);
    }

    /**
     * Parses Data Collection Period dates from:
     * <p>
     * Xpath = {@value OaiPmhConstants#DATA_COLLECTION_PERIODS_PATH}
     * <p>
     * For Data Collection start and end date plus the four digit Year value as Data Collection Year
     */
    public DataCollectionPeriod parseDataCollectionDates(Document doc) {
        Map<String, String> dateAttrs = docElementParser.getDateElementAttributesValueMap(doc, DATA_COLLECTION_PERIODS_PATH);

        var dataCollectionPeriodBuilder = DataCollectionPeriod.builder();

        if (dateAttrs.containsKey(SINGLE_ATTR)) {
            final String singleDateValue = dateAttrs.get(SINGLE_ATTR);
            dataCollectionPeriodBuilder.startDate(singleDateValue);
            dataCollectionPeriodBuilder.dataCollectionYear(parseYearFromDateString(singleDateValue).orElse(0));
        } else {
            if (dateAttrs.containsKey(START_ATTR)) {
                final String startDateValue = dateAttrs.get(START_ATTR);
                dataCollectionPeriodBuilder.startDate(startDateValue);
                dataCollectionPeriodBuilder.dataCollectionYear(parseYearFromDateString(startDateValue).orElse(0));
            }
            if (dateAttrs.containsKey(END_ATTR)) {
                dataCollectionPeriodBuilder.endDate(dateAttrs.get(END_ATTR));
            }
        }

        return dataCollectionPeriodBuilder.build();
    }

    /**
     * Parses File Language(s) from:
     * <p>
     * Xpath = {@value OaiPmhConstants#FILE_TXT_LANGUAGES_XPATH }
     *
     * @return a set with all of the file languages
     */
    public Set<String> parseFileLanguages(Document document) {
        List<String> fileTxtAttrs = docElementParser.getAttributeValues(document, FILE_TXT_LANGUAGES_XPATH);
        List<String> fileNameAttrs = docElementParser.getAttributeValues(document, FILENAME_LANGUAGES_XPATH);
        return Stream.concat(fileTxtAttrs.stream(), fileNameAttrs.stream()).collect(Collectors.toSet());
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
}