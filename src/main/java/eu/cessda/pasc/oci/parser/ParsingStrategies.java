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

import eu.cessda.pasc.oci.TimeUtility;
import eu.cessda.pasc.oci.exception.InvalidUniverseException;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import lombok.NonNull;
import org.jdom2.Element;
import org.jdom2.Namespace;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;
import static eu.cessda.pasc.oci.parser.XMLMapper.*;
import static java.util.Optional.ofNullable;

/**
 * Placeholder for various strategies to use to extract metadata for each field type.
 *
 * @author moses AT doraventures DOT com
 */
class ParsingStrategies{

    private ParsingStrategies() {
    }

    // Metadata handling
    private static final String EMPTY_EL = "empty";
    private static final String PUBLISHER_NOT_AVAIL = "Publisher not specified";
    private static final String STRING = "String";


    /**
     * Constructs a {@link Country} using the given element.
     * <p>
     * The ISO code is derived from the{@value OaiPmhConstants#ABBR_ATTR} attribute.
     *
     * @param element the {@link Element} to parse.
     * @return an Optional {@link Country}.
     */
    static Optional<Country> countryStrategy(Element element) {
        var country = new Country(
            getAttributeValue(element, ABBR_ATTR).orElse(null),
            cleanCharacterReturns(element.getText())
        );
        return Optional.of(country);
    }

    /**
     * Constructs a {@link Pid} using the given element.
     * <p>
     * The pid is derived from the element's text, and the abbreviation is derived from the
     * {@value OaiPmhConstants#AGENCY_ATTR} attribute.
     *
     * @param element the {@link Element} to parse.
     * @return an Optional {@link Pid}.
     */
    static Optional<Pid> pidStrategy(Element element) {
        var pid = new Pid(
            getAttributeValue(element, AGENCY_ATTR).orElse(null),
            element.getText()
        );
        return Optional.of(pid);
    }

    /**
     * Constructs a {@link Pid} using the given element.
     * @param element the {@link Element} to parse.
     * @return an Optional {@link Pid}.
     */
    @SuppressWarnings("java:S131")
    static Optional<Pid> pidLifecycleStrategy(Element element) {
        String agency = null;
        String identifier = null;

        for (var child : element.getChildren()) {
            // Only search in the same namespace context as the parent element
            if (!child.getNamespace().equals(element.getNamespace())) {
                continue;
            }

            switch (child.getName()) {
                case "ManagingAgency" -> agency = child.getText();
                case "IdentifierContent" -> identifier = child.getText();
            }
        }

        // Only return a PID if an identifier is found
        if (identifier != null) {
            return Optional.of(new Pid(agency, identifier));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns an {@link Optional} representing the text of the given element if the element text is not blank,
     * otherwise returns an empty optional.
     *
     * @param element the {@link Element} to parse.
     */
    static Optional<String> nullableElementValueStrategy(Element element) {
        String value = element.getTextTrim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    /**
     * Returns an {@link Optional} representing the text of the element, as well as the affiliation if present.
     * <p>
     * If the {@value OaiPmhConstants#CREATOR_AFFILIATION_ATTR} attribute is present, then the string is constructed
     * as "element text (attribute value)". Otherwise only the element text is returned.
     *
     * @param element the {@link Element} to parse.
     */
    static Optional<Creator> creatorStrategy(Element element) {

        var creator = element.getTextTrim();
        var affiliation = element.getAttributeValue(CREATOR_AFFILIATION_ATTR);

        Creator.Identifier identifier = null;

        // Is there an ExtLink?
        var extLinkElement = element.getChild("ExtLink", null);
        if (extLinkElement != null) {
            var type = extLinkElement.getAttributeValue("title");
            var identifierString = extLinkElement.getTextTrim();
            var extLink = getAttributeValue(extLinkElement, URI_ATTR).map(URI::create).orElse(null);

            identifier = new Creator.Identifier(identifierString, type, extLink);
        }

        if (!creator.isEmpty()) {
            var creatorRecord = new Creator(creator, affiliation, identifier);
            return Optional.of(creatorRecord);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Constructs a {@link Publisher} using the given element.
     * <p>
     * The name is derived from the element's text, and the abbreviation is derived from the
     * {@value OaiPmhConstants#ABBR_ATTR} attribute. If the attribute is missing or otherwise
     * unset the abbreviation will be set to {@value PUBLISHER_NOT_AVAIL}.
     *
     * @param element the {@link Element} to parse.
     * @return a {@link Publisher}.
     */
    static Publisher publisherStrategy(Element element) {
        return new Publisher(
            getAttributeValue(element, ABBR_ATTR).orElse(PUBLISHER_NOT_AVAIL),
            cleanCharacterReturns(element.getText())
        );
    }

    /**
     * Parses a vocabulary term from the given element.
     *
     * @param element the element to parse.
     * @param hasControlledValue whether to use a child {@value OaiPmhConstants#CONCEPT_EL} element to extract the vocabulary attributes.
     * @return the term.
     */
    static Optional<TermVocabAttributes> termVocabAttributeStrategy(Element element, boolean hasControlledValue) {
        // The term always comes from the original element
        var term = cleanCharacterReturns(element.getText());

        // If hasControlledValue is true extract the identifier from the element text,
        // otherwise lookup the id from the ID_ATTR attribute
        Element vocabElement;
        String id;
        if (hasControlledValue) {
            // Try to find a concept element
            vocabElement = ofNullable(element.getChild(CONCEPT_EL, element.getNamespace())).orElse(new Element(EMPTY_EL));
            id = vocabElement.getText();
        } else {
            // Use the original element as the source of the vocabulary
            vocabElement = element;
            id = getAttributeValue(element, ID_ATTR).orElse("");
        }

        var termVocab = new TermVocabAttributes(
            getAttributeValue(vocabElement, VOCAB_ATTR).orElse(""),
            getAttributeValue(vocabElement, VOCAB_URI_ATTR).orElse(""),
            id,
            term
        );
        return Optional.of(termVocab);
    }

    /**
     * Parses a vocabulary term from the given element.
     *
     * @param element the element to parse.
     * @param attrNames the names of the attributes representing the vocab name and URI.
     * @return the term.
     */
    @NonNull
    @SuppressWarnings("java:S131")
    static Optional<TermVocabAttributes> termVocabAttributeLifecycleStrategy(Element element, TermVocabAttributeNames attrNames) {
        String vocab = "";
        String vocabUri = "";

        var term = element.getText();
        for (var attr : element.getAttributes()) {
            String attrName = attr.getName();
            if (attrName.equals(attrNames.vocab())) {
                vocab = attr.getValue();
            } else if (attrName.equals(attrNames.vocabUri())) {
                vocabUri = attr.getValue();
            }
        }

        return Optional.of(new TermVocabAttributes(vocab, vocabUri, "", term));
    }

    /**
     * Parses the value of the {@value OaiPmhConstants#URI_ATTR} attribute.
     *
     * @param element the {@link Element} to parse.
     * @return the value of the attribute, or {@link Optional#empty()} if the attribute was not present.
     */
    static Optional<String> uriStrategy(Element element) {
        var uriAttr = element.getAttribute(URI_ATTR);
        if (uriAttr != null) {
            return Optional.of(uriAttr.getValue());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Parses a sampling vocabulary term from the given element. Vocabulary information is extracted from a
     * descendant {@value OaiPmhConstants#CONCEPT_EL} element.
     * @param element the element to parse.
     * @return a term, or an empty optional if there was no descendant {@value OaiPmhConstants#CONCEPT_EL} element
     * and the element text content was blank.
     */
    static Optional<TermVocabAttributes> samplingTermVocabAttributeStrategy(Element element) {
        var term = element.getTextTrim();

        //PUG req. only process if element has a <concept>
        var conceptVal = element.getChild(CONCEPT_EL, element.getNamespace());
        if (conceptVal != null) {
            var vocabAttributes = new TermVocabAttributes(
                getAttributeValue(conceptVal, VOCAB_ATTR).orElse(""),
                getAttributeValue(conceptVal, VOCAB_URI_ATTR).orElse(""),
                conceptVal.getText(),
                term
            );
            return Optional.of(vocabAttributes);
        } else if (!term.isEmpty()) {
            return Optional.of(new TermVocabAttributes("", "", "", term));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Constructs a {@link DataCollectionFreeText} using the given element.
     * <p>
     * The free text field is derived from the element text, and the event is derived from the
     * {@value OaiPmhConstants#EVENT_ATTR} attribute.
     *
     * @param element the {@link Element} to parse.
     * @return a {@link DataCollectionFreeText}.
     */
    static Optional<DataCollectionFreeText> dataCollFreeTextStrategy(Element element) {
        // #243: Extract in all cases - previously if the @date attribute was present extraction was skipped
        if (!element.getText().isEmpty()) {
            var dataCollectionFreeText = new DataCollectionFreeText(
                element.getText(),
                getAttributeValue(element, EVENT_ATTR).orElse(null)
            );
            return Optional.of(dataCollectionFreeText);
        }

        return Optional.empty();
    }

    /**
     * Constructs a {@link RelatedPublication} using the given element.
     * <p>
     * The strategy tries to extract the title from {@code //citation/titlStmt/titl}, the holdings from
     * {@code //citation/holdings} using the URI attribute and the publication date from
     * {@code //citation/distStmt/distDate/@date}. If the {@code titl} element cannot be found, or if the
     * {@code titl} element is blank then the method attempts to extract directly from the element.
     * If a title cannot be extracted, an empty {@link Optional} is returned.
     * @param element the {@link Element} to parse.
     * @return a {@link RelatedPublication}, or an empty {@link Optional} if a title cannot be extracted.
     */
    static Optional<RelatedPublication> relatedPublicationsStrategy(Element element) {
        var namespace = element.getNamespace();

        // Result variables
        String title = null;
        var holdings = new ArrayList<URI>();
        String publicationDate = "";

        // Determine whether the element has a citation in it
        var citation = ofNullable(element.getChild("citation", namespace));

        // Determine if ExtLink has a URI
        ofNullable(element.getChild("ExtLink", namespace))
            .map(e -> e.getAttributeValue(URI_ATTR))
            .map(uriString -> {
                try {
                    return new URI(uriString.trim());
                } catch (URISyntaxException e) {
                    // filter out invalid URIs
                    return null;
                }
            }).ifPresent(holdings::add);

        // Parse the holdings and publication date in the citation if present
        if (citation.isPresent()) {
            var uris = parseHoldingsURI(citation.orElseThrow());
            holdings.addAll(uris);

            // Parse the publication date from citation/distStmt/distDate/@date
            publicationDate = citation
            .map(c -> c.getChild("distStmt", namespace))
            .map(d -> d.getChild("distDate", namespace))
            .map(e -> e.getAttributeValue("date"))
            .map(String::trim)
            .flatMap(dateStr -> {
                try {
                    TimeUtility.getTimeFormat(dateStr, Function.identity());
                    return Optional.of(dateStr);
                } catch (DateTimeParseException e) {
                    // Invalid date, ignore
                    return Optional.empty();
                }
            }).orElse("");
        }

        // Try to extract text from the relPubl element
        var elementText = element.getTextTrim();
        if (!elementText.isBlank()) {
            title = elementText;
        } else {
            final Optional<Element> titleElement;

            // Try to locate biblCit
            var bibCit = citation.map(c -> c.getChild("biblCit", namespace));
            if (bibCit.isPresent()) {
                // Use biblCit for the title
                titleElement = bibCit;
            } else {
                // No text has been found, try to parse the citation if present
                titleElement = citation.map(c -> c.getChild("titlStmt", namespace))
                    .map(titlStmt -> titlStmt.getChild("titl", namespace));
            }

            // Extract the text from the element
            var optionalTitle = titleElement.map(Element::getTextTrim).filter(s -> !s.isBlank());
            if (optionalTitle.isPresent()) {
                title = optionalTitle.orElseThrow();
            }
        }

        // Only return if the title is set
        if (title != null) {
            var relatedPublication = new RelatedPublication(title, holdings, publicationDate);
            return Optional.of(relatedPublication);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Parse the URIs of the holdings of a citation.
     * @return a list of URIs of the holdings of a citation.
     */
    private static List<URI> parseHoldingsURI(Element citation) {
        var holdingsElements = citation.getChildren("holdings", citation.getNamespace());
        var holdingsURIList = new ArrayList<URI>(holdingsElements.size());

        for (var holdings : holdingsElements) {
            var holdingsURIAttrValue = holdings.getAttributeValue(URI_ATTR);
            if (holdingsURIAttrValue != null) {
                // Attempt to parse the holdings URI, drop if invalid
                try {
                    var holdingsURI = new URI(holdingsURIAttrValue.trim());
                    holdingsURIList.add(holdingsURI);
                } catch (URISyntaxException e) {
                    // filter out invalid URIs
                }
            }
        }

        return holdingsURIList;
    }

    /**
     * Remove return characters (i.e. {@code \n}) from the string.
     */
    static String cleanCharacterReturns(String candidate) {
        return candidate.replace("\n", "").trim();
    }

    /**
     * Parse the universe element. The clusion defaults to I if not present in the source element.
     * @param element the element to parse.
     * @return a {@link Map.Entry} with the key set to the clusion status and the value set to the content of the element.
     */
    static Optional<UniverseElement> universeStrategy(Element element) {

        // Set the text content
        var content = element.getTextTrim();

        // Set the inclusion/exclusion status if defined, defaults to inclusion if not defined
        var clusion = element.getAttributeValue("clusion");
        if (clusion != null) {
            try {
                var clusionValue = Universe.Clusion.valueOf(clusion);
                return Optional.of(new UniverseElement(clusionValue, content));
            } catch (IllegalArgumentException e) {
                throw new InvalidUniverseException(clusion, e);
            }
        } else {
            // Clusion not defined in the source element, default to an inclusion
            // See https://ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/field_level_documentation_files/schemas/codebook_xsd/elements/universe.html
            return Optional.of(new UniverseElement(Universe.Clusion.I, content));
        }
    }

    /**
     * Parse the universe element. The clusion defaults to I if the source element doesn't specify its inclusivity.
     *
     * @param elements the elements to parse.
     * @return a {@link Map} the language as the key and a list of universes as the value.
     */
    @NonNull
    static Map<String, List<UniverseElement>> universeLifecycleStrategy(List<Element> elements) {
        var map = new HashMap<String, List<UniverseElement>>();

        for (var universeElement : elements) {
            var inclusionStatus = parseInclusionStatus(universeElement);

            // Language specific content is stored in sub-elements which needs to be flattened
            for (var otherMaterial : universeElement.getChildren("Description", null)) {
                for (var contentElement : otherMaterial.getChildren("Content", null)) {
                    map.computeIfAbsent(
                        XMLMapper.getLangOfElement(contentElement), k -> new ArrayList<>()
                    ).add(
                        new UniverseElement(inclusionStatus, contentElement.getText())
                    );
                }
            }
        }

        return map;
    }

    /**
     * Parse the inclusion status of the given universe element using the {@code isInclusive} attribute.
     *
     * @param universeElement the element to parse
     * @return the universe's inclusion status.
     */
    @NonNull
    private static Universe.Clusion parseInclusionStatus(Element universeElement) {
        // Search for the isInclusive attribute
        var attr = universeElement.getAttribute("isInclusive", null);

        // Only "false" should cause the universe to be marked as an exclusion
        if (attr != null && "false".equalsIgnoreCase(attr.getValue())) {
            return Universe.Clusion.E;
        } else {
            return Universe.Clusion.I;
        }
    }

    /**
     * Extract the value of the {@code date} attribute of the given element.
     *
     * @param element the element to parse.
     * @return the value of the date attribute, or {@code null} if the attribute is not present.
     */
    @Nullable
    static String dateStrategy(Element element) {
        var attribute = element.getAttribute("date", null);
        if (attribute != null) {
            return attribute.getValue();
        } else {
            return null;
        }
    }

    @NonNull
    static Map<String, Creator> creatorsLifecycleStrategy(Element element) {
        var creatorsMap = new HashMap<String, Creator>();

        // Parse affiliation attribute
        String affiliation = element.getAttributeValue("affiliation");

        for (var child : element.getChildren(STRING, null)) {
            // Extract the creator name and language
            var creator = child.getTextTrim();
            var lang = getLangOfElement(child);

            if (creator != null) {
                creatorsMap.put(lang, new Creator(creator, affiliation, null));
            }
        }

        return creatorsMap;
    }

    @NonNull
    static CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, List<DateTimeParseException>> dataCollectionPeriodsStrategy(List<Element> elementList) {
        var dateAttrs = getDateElementAttributesValueMap(elementList);

        var dataCollectionPeriodBuilder = CMMStudyMapper.DataCollectionPeriod.builder();

        var parseExceptions = new ArrayList<DateTimeParseException>(2);

        if (dateAttrs.containsKey(SINGLE_ATTR)) {
            final String singleDateValue = dateAttrs.get(SINGLE_ATTR);
            dataCollectionPeriodBuilder.startDate(singleDateValue);
            try {
                var year = TimeUtility.getTimeFormat(singleDateValue, Year::from);
                dataCollectionPeriodBuilder.dataCollectionYear(year.getValue());
            } catch (DateTimeParseException e) {
                parseExceptions.add(e);
            }
        } else {
            if (dateAttrs.containsKey(START_ATTR)) {
                final String startDateValue = dateAttrs.get(START_ATTR);
                dataCollectionPeriodBuilder.startDate(startDateValue);
                try {
                    var year = TimeUtility.getTimeFormat(startDateValue, Year::from);
                    dataCollectionPeriodBuilder.dataCollectionYear(year.getValue());
                } catch (DateTimeParseException e) {
                    parseExceptions.add(e);
                }
            }
            if (dateAttrs.containsKey(END_ATTR)) {
                dataCollectionPeriodBuilder.endDate(dateAttrs.get(END_ATTR));
            }
        }

        // Parse free texts
        var freeTexts = extractMetadataObjectListForEachLang(ParsingStrategies::dataCollFreeTextStrategy).apply(elementList);
        dataCollectionPeriodBuilder.freeTexts(freeTexts);

        return new CMMStudyMapper.ParseResults<>(
            dataCollectionPeriodBuilder.build(),
            parseExceptions
        );
    }

    @NonNull
    @SuppressWarnings("java:S131")
    static CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, List<DateTimeParseException>> dataCollectionPeriodsLifecycleStrategy(Element dataCollectionDate) {
        String startDate = null;
        String endDate = null;
        String singleDate = null;

        for (var child : dataCollectionDate.getChildren()) {
            switch (child.getName()) {
                case "StartDate" -> startDate = child.getTextTrim();
                case "EndDate" -> endDate = child.getTextTrim();
                case "SimpleDate" -> singleDate = child.getTextTrim();
            }
        }

        var parseExceptions = new ArrayList<DateTimeParseException>();

        // Derive the data collection year
        Integer year = null;
        if (singleDate != null) {
            try {
                var parsedYear = TimeUtility.getTimeFormat(singleDate, Year::from);
                year = parsedYear.getValue();
            } catch (DateTimeParseException e) {
                parseExceptions.add(e);
            }
        }

        if (year == null && startDate != null) {
            try {
                var parsedYear = TimeUtility.getTimeFormat(startDate, Year::from);
                year = parsedYear.getValue();
            } catch (DateTimeParseException e) {
                parseExceptions.add(e);
            }
        }

        var dataCollectionPeriod = new CMMStudyMapper.DataCollectionPeriod(startDate, year, endDate, Collections.emptyMap());
        return new CMMStudyMapper.ParseResults<>(dataCollectionPeriod, parseExceptions);
    }

    @NonNull
    static Map<String, List<TermVocabAttributes>> conceptStrategy(List<Element> elementList, Function<Element, Optional<TermVocabAttributes>> mappingFunction) {
        var map = new HashMap<String, List<TermVocabAttributes>>();

        for (var element : elementList) {
            mappingFunction.apply(element).ifPresent(mappedElement ->
                map.computeIfAbsent(parseConceptLanguageCode(element), k -> new ArrayList<>()).add(mappedElement)
            );
        }

        return map;
    }

    /**
     * Extract the creator/publisher from a DDI Lifecycle {@code Organization} element.
     *
     * @param element the element to parse.
     * @return a map containing language specific version of the publisher.
     */
    @NonNull
    @SuppressWarnings("java:S131")
    static Map<String, Publisher> organizationStrategy(Element element) {
        var identification = element.getChild("OrganizationIdentification", null);
        if (identification == null) {
            return Collections.emptyMap();
        }

        var organizationName = identification.getChild("OrganizationName", null);
        if (organizationName == null) {
            return Collections.emptyMap();
        }


        var nameMap = new HashMap<String, String>();
        var abbrMap = new HashMap<String, String>();

        for (var child : organizationName.getChildren()) {

            switch (child.getName()) {
                case STRING -> {
                    var lang = getLangOfElement(child);
                    var t = child.getTextTrim();
                    nameMap.put(lang, t);
                }
                case "Abbreviation" -> {
                    var abbrStrElems = child.getChildren(STRING, null);
                    for (var a : abbrStrElems) {
                        var lang = getLangOfElement(a);
                        var t = a.getTextTrim();
                        abbrMap.put(lang, t);
                    }
                }
            }
        }

        // Merge name and abbreviation maps
        var publisherMap = new HashMap<String, Publisher>();
        Stream.concat(nameMap.keySet().stream(), abbrMap.keySet().stream()).distinct().forEach(key ->
            publisherMap.put(key, new Publisher(abbrMap.getOrDefault(key, ""), nameMap.getOrDefault(key, "")))
        );

        return Collections.unmodifiableMap(publisherMap);
    }

    /**
     * Extract country information from a {@code GeographicReference} element
     *
     * @param geographicReference the element to parse.
     * @return a map containing language specific versions of the country.
     */
    @SuppressWarnings({"java:S131", "java:S3776"})
    static Map<String, Country> geographicReferenceStrategy(Element geographicReference) {
        var countryMap = new HashMap<String, Country>();

        var locationValue = geographicReference.getChild("LocationValue", null);
        if (locationValue != null) {

            var nameMap = new HashMap<String, String>();

            String code = null;

            for (var child : locationValue.getChildren()) {
                switch (child.getName()) {
                    case "GeographicLocationIdentifier" -> {
                        // Extract the location code
                        var locIdentifier = locationValue.getChild("GeographicLocationIdentifier", null);
                        if (locIdentifier != null) {
                            var geographicCode = locIdentifier.getChild("GeographicCode", null);
                            if (geographicCode != null) {
                                code = geographicCode.getTextTrim();
                            }
                        }
                    }
                    case "LocationValueName" -> {
                        for (var string : child.getChildren(STRING, null)) {
                            var countryString = string.getTextTrim();
                            var lang = getLangOfElement(string);
                            nameMap.put(lang, countryString);
                        }
                    }
                }
            }

            for (var entry : nameMap.entrySet()) {
                var lang = entry.getKey();
                var country = new Country(code, entry.getValue());
                countryMap.put(lang, country);
            }
        }

        return countryMap;
    }

    /**
     * Extract the creator/publisher from a DDI Lifecycle {@code Individual} element.
     *
     * @param element the element to parse.
     * @return a map containing language specific version of the publisher.
     */
    static Map<String, Creator> individualStrategy(Element element) {
        var identification = element.getChild("IndividualIdentification", null);
        if (identification == null) {
            return Collections.emptyMap();
        }

        var map = new HashMap<String, Creator>();

        Creator.Identifier identifier = null;

        var researcherID = identification.getChild("ResearcherID", null);
        if (researcherID != null) {
            String rtype = XMLMapper.getTextContent(researcherID.getChild("TypeOfID", null));
            String rid = XMLMapper.getTextContent(researcherID.getChild("ResearcherIdentification", null));

            // avoid NullPointerException by checking the string for null
            URI ruri = null;
            var uriString = getTextContent(researcherID.getChild("URI", null));
            if (uriString != null) {
               ruri = URI.create(uriString);
            }

            identifier = new Creator.Identifier(rid, rtype, ruri);
        }

        var names = identification.getChildren("IndividualName", null);
        for (var name : names) {

            var isPreferred = name.getAttributeValue("isPreferred", (Namespace) null);

            // Use the full name if present
            var fullName = name.getChild("FullName", null);
            if (fullName != null) {
                var stringElements = fullName.getChildren(STRING, null);
                for (var string : stringElements) {
                    var lang = getLangOfElement(string);
                    var publisher = new Creator(string.getTextTrim(), null, identifier);

                    if (Boolean.parseBoolean(isPreferred)) {
                        map.put(lang, publisher);
                    } else {
                        map.putIfAbsent(lang, publisher);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(map);
    }

    @NonNull
    static Map<String, List<TermVocabAttributes>> analysisUnitStrategy(List<Element> elementList, TermVocabAttributeNames attrNames) {
        var termList = new ArrayList<TermVocabAttributes>(elementList.size());
        for (var element : elementList) {
            termVocabAttributeLifecycleStrategy(element, attrNames).ifPresent(termList::add);
        }
        return Map.of(EMPTY_LANGUAGE, termList);
    }

    /**
     * Parse DDI Lifecycle {@code Creator} elements.
     *
     * @param creatorElements a list of {@code Creator} elements.
     * @return a map of creators per language.
     */
    @NonNull
    static Map<String, List<Creator>> creatorsStrategy(List<Element> creatorElements) {
        var creatorsMap = new HashMap<String, List<Creator>>();

        for (var element : creatorElements) {

            var creatorNameElement = element.getChild("CreatorName", null);
            var creatorReferenceElement = element.getChild("CreatorReference", null);

            if (creatorNameElement != null) {
                creatorsLifecycleStrategy(creatorNameElement).forEach((lang, creator) ->
                    creatorsMap.computeIfAbsent(lang, l -> new ArrayList<>()).add(creator)
                );
            } else if (creatorReferenceElement != null) {
                var referencedElement = resolveReference(creatorReferenceElement);
                var publisherMapOpt = referencedElement.flatMap(r -> switch (r.type()) {
                    case "Individual" -> Optional.of(individualStrategy(r.element()));
                    case "Organization" -> Optional.of(organizationStrategy(r.element()));
                    default -> Optional.empty();
                }).orElse(
                    // Placeholder empty map
                    Collections.<String, Record>emptyMap()
                );

                publisherMapOpt.forEach((key, v) -> {
                    Creator creator;

                    if (v instanceof Publisher publisher) {
                        creator = extractCreatorFromPublisher(publisher);
                    } else if (v instanceof Creator) {
                        creator = (Creator) v;
                    } else {
                        return;
                    }

                    creatorsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(creator);
                });
            }
        }

        return creatorsMap;
    }

    /**
     * Create a new {@link Creator} using the given {@link Publisher}. Only the publisher's name
     * is extracted.
     *
     * @param publisher the {@link Publisher} to convert
     * @return a creator.
     */
    private static @NonNull Creator extractCreatorFromPublisher(Publisher publisher) {
        Creator creator;
        String name;
        if (publisher.abbreviation().isEmpty()) {
            name = publisher.name();
        } else {
            name = publisher.name() + " (" + publisher.abbreviation() + ")";
        }

        creator = new Creator(name, null, null);
        return creator;
    }

    @NonNull
    static Map<String, List<TermVocabAttributes>> samplingProceduresLifecycleStrategy(List<Element> elementList, TermVocabAttributeNames attrNames) {
        return controlledVocabularyStrategy(elementList, "TypeOfSamplingProcedure", attrNames);
    }

    @NonNull
    static Map<String, List<TermVocabAttributes>> typeOfModeOfCollectionLifecycleStrategy(List<Element> elementList, TermVocabAttributeNames attrNames) {
        return controlledVocabularyStrategy(elementList, "TypeOfModeOfCollection", attrNames);
    }

    @NonNull
    static Map<String, List<TermVocabAttributes>> typeOfTimeMethodLifecycleStrategy(List<Element> elementList, TermVocabAttributeNames attrNames) {
        return controlledVocabularyStrategy(elementList, "TypeOfTimeMethod", attrNames);
    }

    @NonNull
    @SuppressWarnings({"java:S3776", "ExtractMethodRecommender"})
    private static Map<String, List<TermVocabAttributes>> controlledVocabularyStrategy(List<Element> elementList, String controlledVocabularyElement, TermVocabAttributeNames attrNames) {
        var mergedMap = new HashMap<String, List<TermVocabAttributes>>();

        for (var element : elementList) {
            Optional<TermVocabAttributes> termVocabAttributes = Optional.empty();
            Map<String, List<String>> langMap = Collections.emptyMap();

            for (var child : element.getChildren()) {
                String childName = child.getName();
                if (childName.equals("Description")) {
                    // Extract text
                    var contentElements = child.getChildren();
                    langMap = extractMetadataObjectListForEachLang(ParsingStrategies::nullableElementValueStrategy).apply(contentElements);
                } else {
                    if (childName.equals(controlledVocabularyElement)) {
                        // Extract controlled vocabulary information
                        termVocabAttributes = termVocabAttributeLifecycleStrategy(child, attrNames);
                    }
                }
            }

            // Merge CV information with descriptions
            for (var entry : langMap.entrySet()) {
                var vocabAttributesList = new ArrayList<TermVocabAttributes>();

                for (var term : entry.getValue()) {
                    if (termVocabAttributes.isPresent()) {
                        var vocabAttributes = termVocabAttributes.get();
                        vocabAttributesList.add(new TermVocabAttributes(vocabAttributes.vocab(), vocabAttributes.vocabUri(), vocabAttributes.id(), term));
                    } else {
                        vocabAttributesList.add(new TermVocabAttributes("", "", "", term));
                    }
                }

                mergedMap.merge(entry.getKey(), vocabAttributesList, (a, b) -> { a.addAll(b); return a; });
            }

            // If no descriptions are present, derive the text from the TypeOfSamplingProcedure element
            if (langMap.isEmpty() && termVocabAttributes.isPresent()) {
                mergedMap.computeIfAbsent(EMPTY_LANGUAGE, k -> new ArrayList<>()).add(termVocabAttributes.get());
            }
        }

        return mergedMap;
    }

    /**
     * Extract language specific lists of related publications.
     *
     * @param elementList the list of {@code OtherMaterial} elements to parse.
     * @return a map of language specific related publication lists.
     */
    @NonNull
    @SuppressWarnings("java:S3776")
    static Map<String, List<RelatedPublication>> relatedPublicationLifecycleStrategy(List<Element> elementList) {
        var relPubLMap = new HashMap<String, List<RelatedPublication>>();

        for (var element : elementList) {

            // Filter by TypeOfMaterial = "Related Publication"
            var typeOfMaterial = element.getChildTextTrim("TypeOfMaterial", null);
            if (typeOfMaterial == null || !typeOfMaterial.equalsIgnoreCase("Related Publication")) {
                continue;
            }

            var uriList = new ArrayList<URI>();
            var titleMap = new HashMap<String, String>();
            String publicationDate = "";

            // First try ExternalURLReference as primary URI
            var externalUrl = element.getChildTextTrim("ExternalURLReference", null);
            if (externalUrl != null && !externalUrl.isBlank()) {
                try {
                    uriList.add(URI.create(externalUrl));
                } catch (IllegalArgumentException e) {
                    // Invalid URI, ignore
                }
            }

            // Extract the citation
            var citation = element.getChild("Citation", null);
            if (citation != null) {
                for (var child : citation.getChildren()) {
                    if (child.getName().equals("InternationalIdentifier")) {
                        // Extract the URL of the identifier
                        var identifierContext = child.getChild("IdentifierContent", null);
                        if (identifierContext != null) {
                            var uriStr = identifierContext.getTextTrim();
                            try {
                                uriList.add(URI.create(uriStr));
                            } catch (IllegalArgumentException e) {
                                // Invalid URI, ignore
                            }
                        }

                    } else if (child.getName().equals("Title")) {
                        // Extract the language-dependent title
                        for (var string : child.getChildren(STRING, null)) {
                            titleMap.put(getLangOfElement(string), string.getTextTrim());
                        }

                    } else if (child.getName().equals("PublicationDate")) {
                        // Try to extract and validate SimpleDate
                        var simpleDate = child.getChild("SimpleDate", null);
                        if (simpleDate != null) {
                            var dateStr = simpleDate.getTextTrim();
                            if (!dateStr.isBlank()) {
                                try {
                                    TimeUtility.getTimeFormat(dateStr, Function.identity());
                                    publicationDate = dateStr;
                                } catch (DateTimeParseException e) {
                                    // Invalid date, keep as ""
                                }
                            }
                        }
                    }
                }
            }

            final String finalPublicationDate = publicationDate;
            titleMap.forEach((lang, title) -> {
                // Merge the language dependent title with the list of URIs and publication date
                var relPub = new RelatedPublication(title, uriList, finalPublicationDate);
                relPubLMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(relPub);
            });
        }

        return relPubLMap;
    }

    @NonNull
    static Map<String, List<Country>> geographicLocationStrategy(List<Element> elementList) {
        var countryListMap = new HashMap<String, List<Country>>();
        for (var element : elementList) {
          resolveReference(element).ifPresent(referencedElement -> {
              var geographicReference = referencedElement.element();
              var countryMap = geographicReferenceStrategy(geographicReference);
              countryMap.forEach((key, value) -> countryListMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value));
          });
        }
        return countryListMap;
    }

    @NonNull
    static Map<String, Publisher> publisherReferenceStrategy(List<Element> elementList) {
        for (var element : elementList) {
            var referencedElement = resolveReference(element);
            var publisherMapOpt = referencedElement.flatMap(r -> switch (r.type()) {
                case "Individual" -> {
                    var creatorMap = individualStrategy(r.element());

                    // Create a Publisher to represent this creator
                    var publisherMap = new HashMap<String, Publisher>(creatorMap.size());
                    creatorMap.forEach((lang, creator) -> {
                        var publisher = new Publisher(null, creator.name());
                        publisherMap.put(lang, publisher);
                    });
                    yield Optional.of(publisherMap);
                }
                case "Organization" -> Optional.of(organizationStrategy(r.element()));
                default -> Optional.empty();
            }).orElse(Collections.emptyMap());

            // Return the first publisher entry found
            if (!publisherMapOpt.isEmpty()) {
                return publisherMapOpt;
            }
        }

        // No publishers found
        return Collections.emptyMap();
    }

    @NonNull
    static Optional<Funding> fundingStrategy(Element element) {
        var grantNumber = nullableElementValueStrategy(element);
        var agency = getAttributeValue(element, AGENCY_ATTR).orElse(null);
        return Optional.of(new Funding(grantNumber.orElse(null), agency));
    }

    /**
     * Constructs a {@link DataKindFreeText} using the given element.
     * <p>
     * The free text field is derived from the element text, and the type is derived from the
     * {@value OaiPmhConstants#TYPE_ATTR} attribute.
     *
     * @param element the {@link Element} to parse.
     * @return a {@link DataKindFreeText}.
     */
    @NonNull
    static Optional<DataKindFreeText> dataKindFreeTextStrategy(Element element) {
        var dataKind = nullableElementValueStrategy(element);
        var type = getAttributeValue(element, TYPE_ATTR).orElse(null);
        return Optional.of(new DataKindFreeText(dataKind.orElse(null), type));
    }

    /**
     * Returns the text content of the given attribute.
     * If the attribute does not exist, an empty {@link Optional} will be returned.
     *
     * @param element the element to parse.
     * @param idAttr  the attribute to return the text content of.
     */
    private static Optional<String> getAttributeValue(Element element, String idAttr) {
        return ofNullable(element.getAttributeValue(idAttr));
    }

    /**
     * Gets the date attribute from elements that have an {@value OaiPmhConstants#EVENT_ATTR} attribute.
     * <p>
     * If the same {@value OaiPmhConstants#EVENT_ATTR} type is defined for multiple languages the filter will only keep the first encountered.
     * <ul>
     *     <li>{@code <collDate xml:lang="en" date="2009-03-19" event="start"/> }</li>
     *     <li><strike>{@code <collDate xml:lang="fi" date="2009-03-19" event="start"/> }</strike></li>
     * </ul>
     * There is currently no requirement to extract dates of event per language.
     *
     * @param elements the elements to extract attributes from.
     * @return a {@link Map} with the keys set to the {@value OaiPmhConstants#EVENT_ATTR} and the values set to the date values.
     */
    @NonNull
    private static Map<String, String> getDateElementAttributesValueMap(List<Element> elements) {
        //PUG requirement: we only care about those with @date CV
        Map<String, String> map = new HashMap<>();
        for (var element : elements) {
            var eventAttr = getAttributeValue(element, EVENT_ATTR);
            var dateAttr = getAttributeValue(element, DATE_ATTR);
            if (eventAttr.isPresent() && dateAttr.isPresent()) {
                map.putIfAbsent(eventAttr.get(), dateAttr.get());
            }
        }
        return map;
    }

    /**
     * Parse funding information from DDI Lifecycle {@code FundingInformation} element.
     * <p>
     * This method extracts the grant number and the organisation name from the {@code FundingInformation} element.
     * @param elementList a list of {@code FundingInformation} elements.
     * @return a map of language specific funding information.
     */
    @NonNull
    static Map<String, List<Funding>> fundingLifecycleStrategy(List<Element> elementList) {
        var fundingMap = new HashMap<String, List<Funding>>();

        for (var element : elementList) {
            String grantNumber = null;
            Map<String, Publisher> orgLangMap = Collections.emptyMap();

            for (var childElement : element.getChildren()) {
                switch (childElement.getName()) {
                    case "AgencyOrganizationReference":
                        var resolvedReference = resolveReference(childElement);
                        if (resolvedReference.isPresent()) {
                            orgLangMap = organizationStrategy(resolvedReference.get().element());
                        }
                        break;
                    case "GrantNumber":
                        grantNumber = childElement.getTextTrim();
                        break;
                }
            }

            for (var organization : orgLangMap.entrySet()) {
                var funding = new Funding(grantNumber, organization.getValue().name());
                fundingMap.computeIfAbsent(organization.getKey(), k -> new ArrayList<>()).add(funding);
            }
        }

        return fundingMap;
    }

    /**
     * Processes a list of {@link Element}s, returning a {@link String} representing the access category (Open/Restricted).
     * Returns the first non-empty valid result, otherwise returns null.
     *
     * @param elements the list of {@link Element}s to parse.
     * @return a {@link String} with "Open", "Restricted", or null if no valid value is found.
     */
    static String dataAccessStrategy(List<Element> elements) {
        for (Element element : elements) {
            String value = element.getTextTrim();

            // Check if the value is "openAccess", return "Open"
            if (!value.isEmpty()) {
                if ("openAccess".equalsIgnoreCase(value)
                    || "info:eu-repo/semantics/openAccess".equalsIgnoreCase(value)) {
                    return "Open";
                }

                // Check if the value is one of the restricted types and return "Restricted"
                if ("closedAccess".equalsIgnoreCase(value)
                    || "embargoedAccess".equalsIgnoreCase(value)
                    || "restrictedAccess".equalsIgnoreCase(value)
                    || "info:eu-repo/semantics/restrictedAccess".equalsIgnoreCase(value)) {
                    return "Restricted";
                }
            }
        }

        // Return null if no valid value was found
        return null;
    }

    /**
     * Constructs a {@link Series} using the given element.
     * <p>
     * Names come from `serName` child elements, descriptions from `serInfo` child elements,
     * and uri from the {@value OaiPmhConstants#URI_ATTR} attribute.
     *
     * @param element the {@link Element} to parse.
     * @return a {@link Series} if at least one name, description, or uri is found.
     */
    static Optional<Series> seriesStrategy(Element element) {
        var namespace = element.getNamespace();

        var names = element.getChildren("serName", namespace).stream()
                .map(Element::getTextTrim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toList());

        var descriptions = element.getChildren("serInfo", namespace).stream()
                .map(Element::getTextTrim)
                .filter(info -> !info.isEmpty())
                .collect(Collectors.toList());

        var uriString = element.getAttributeValue(URI_ATTR);
        var uris = new ArrayList<URI>();
        if (uriString != null && !uriString.isEmpty()) {
            try {
                uris.add(new URI(uriString.trim()));
            } catch (URISyntaxException e) {
                // filter out invalid URIs
            }
        }

        if (names.isEmpty() && descriptions.isEmpty() && uris.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Series(names, descriptions, uris));
    }

    /**
     * Parse names, descriptions and uris for series from DDI Lifecycle {@code SeriesStatement} element.
     *
     * @param elements the elements to parse.
     * @return a {@link Map} with the language as the key and a list of series as the value.
     */
    @NonNull
    static Map<String, List<Series>> seriesLifecycleStrategy(List<Element> elements) {
        var seriesMap = new HashMap<String, List<Series>>();

        for (var seriesElement : elements) {

            var uriMap = new HashMap<String, List<URI>>();
            var nameMap = new HashMap<String, List<String>>();
            var descriptionMap = new HashMap<String, List<String>>();

            for (var repoLocation : seriesElement.getChildren("SeriesRepositoryLocation", null)) {
                var lang = XMLMapper.getLangOfElement(repoLocation);
                uriMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(URI.create(repoLocation.getTextTrim()));
            }

            for (var nameElement : seriesElement.getChildren("SeriesName", null)) {
                for (var nameContent : nameElement.getChildren("String", null)) {
                    var lang = XMLMapper.getLangOfElement(nameContent);
                    nameMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(nameContent.getTextTrim());
                }
            }

            for (var descElement : seriesElement.getChildren("SeriesDescription", null)) {
                for (var descContent : descElement.getChildren("Content", null)) {
                    var lang = XMLMapper.getLangOfElement(descContent);
                    descriptionMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(descContent.getTextTrim());
                }
            }

            var allLangs = new HashSet<>(uriMap.keySet());
            allLangs.addAll(nameMap.keySet());
            allLangs.addAll(descriptionMap.keySet());

            for (String lang : allLangs) {
                var series = new Series(
                    nameMap.getOrDefault(lang, new ArrayList<>()),
                    descriptionMap.getOrDefault(lang, new ArrayList<>()),
                    uriMap.getOrDefault(lang, new ArrayList<>())
                );
                seriesMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(series);
            }
        }

        return seriesMap;
    }
}
