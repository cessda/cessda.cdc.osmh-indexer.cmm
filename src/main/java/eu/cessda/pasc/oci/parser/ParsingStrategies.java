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
import eu.cessda.pasc.oci.exception.InvalidUniverseException;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import lombok.NonNull;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import org.jdom2.Namespace;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

import static eu.cessda.pasc.oci.parser.DocElementParser.getAttributeValue;
import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;
import static eu.cessda.pasc.oci.parser.XMLMapper.getLangOfElement;
import static java.util.Optional.ofNullable;

/**
 * Placeholder for various strategies to use to extract metadata for each field type.
 *
 * @author moses AT doraventures DOT com
 */
class ParsingStrategies{

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
    static Optional<String> creatorStrategy(Element element) {

        var creator = element.getTextTrim();
        var affiliation = getAttributeValue(element, CREATOR_AFFILIATION_ATTR);

        if (affiliation.isPresent()) {
            return Optional.of(creator + " (" + affiliation.get() + ")");
        } else if (!creator.isEmpty()) {
            return Optional.of(creator);
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

    @NonNull
    static Optional<TermVocabAttributes> termVocabAttributeLifecycleStrategy(Element element) {
        String vocab = "";
        String vocabUri = "";

        var term = element.getText();
        for (var attr : element.getAttributes()) {
            switch (attr.getName()) {
                case "codeListName" -> vocab = attr.getValue();
                case "codeListURN" -> vocabUri = attr.getValue();
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

    static Optional<VocabAttributes> samplingTermVocabAttributeStrategy(Element element) {
        //PUG req. only process if element has a <concept>
        var conceptVal = element.getChild(CONCEPT_EL, element.getNamespace());
        if (conceptVal != null) {
            var vocabAttributes = new VocabAttributes(
                getAttributeValue(conceptVal, VOCAB_ATTR).orElse(""),
                getAttributeValue(conceptVal, VOCAB_URI_ATTR).orElse(""),
                conceptVal.getText()
            );
            return Optional.of(vocabAttributes);
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
     * The strategy tries to extract the title from {@code //citation/titlStmt/titl} and the holdings from
     * {@code //citation/holdings} using the URI attribute. If the {@code titl} element cannot be found, or if the
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

        // Parse the holdings in the citation if present
        if (citation.isPresent()) {
            var uris = parseHoldingsURI(citation.orElseThrow());
            holdings.addAll(uris);
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
            var relatedPublication = new RelatedPublication(title, holdings);
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
     * @param elements the elements to parse.
     * @return a {@link Map} the language as the key and a list of universes as the value.
     */
    @NonNull
    static Map<String, List<UniverseElement>> universeLifecycleStrategy(List<Element> elements) {
        var map = new HashMap<String, List<UniverseElement>>();

        for (var universeElement : elements) {
            var inclusionStatus = parseInclusionStatus(universeElement);

            // Language specific content is stored in sub-elements which needs to be flattened
            for (var otherMaterial : universeElement.getChildren()) {
                if (!otherMaterial.getName().equals("Description")) {
                    continue;
                }

                for (var contentElement : otherMaterial.getChildren()) {
                    if (!contentElement.getName().equals("Content")) {
                        continue;
                    }

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

    @NonNull
    private static Universe.Clusion parseInclusionStatus(Element universeElement) {
        for (var attr :  universeElement.getAttributes()) {
            // Search for the isInclusive attribute
            if (!attr.getName().equals("isInclusive")) {
                continue;
            }

            try {
                if (!attr.getBooleanValue()) {
                    return Universe.Clusion.E;
                }
            } catch (DataConversionException ex) {
                // conversion failed, default to inclusion
            }
        }

        return Universe.Clusion.I;
    }

    @Nullable
    static String dateStrategy(Element element) {
        for (var attribute : element.getAttributes()) {
            if (attribute.getName().equals("date")) {
                return attribute.getValue();
            }
        }
        return null;
    }

    @NonNull
    static HashMap<String, List<String>> creatorsLifecycleStrategy(List<Element> elements) {
        var creatorsMap = new HashMap<String, List<String>>();

        for (var element : elements) {

            // Attempt to parse the affiliation attribute
            String affiliation = null;
            for (var attr : element.getAttributes()) {
                if (attr.getName().equals("affiliation")) {
                    affiliation = attr.getValue();
                }
            }

            for (var child : element.getChildren()) {
                if (!child.getName().equals(STRING)) {
                    continue;
                }

                // Extract the creator name and language
                var creator = child.getTextTrim();
                var lang = getLangOfElement(child);

                if (creator != null) {
                    if (affiliation != null) {
                        creator += " (" + affiliation + ")";
                    }

                    creatorsMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(creator);
                }
            }
        }

        return creatorsMap;
    }

    @NonNull
    static CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, List<DateNotParsedException>> dataCollectionPeriodsStrategy(List<Element> elementList) {
        var dateAttrs = DocElementParser.getDateElementAttributesValueMap(elementList);

        var dataCollectionPeriodBuilder = CMMStudyMapper.DataCollectionPeriod.builder();

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

        // Parse free texts
        var freeTexts = XMLMapper.extractMetadataObjectListForEachLang(ParsingStrategies::dataCollFreeTextStrategy).apply(elementList);
        dataCollectionPeriodBuilder.freeTexts(freeTexts);

        return new CMMStudyMapper.ParseResults<>(
            dataCollectionPeriodBuilder.build(),
            parseExceptions
        );
    }

    @NonNull
    static CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, List<DateNotParsedException>> dataCollectionPeriodsLifecycleStrategy(Element dataCollectionDate) {
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

        var parseExceptions = new ArrayList<DateNotParsedException>();

        // Derive the data collection year
        Integer year = null;
        if (singleDate != null) {
            try {
                var localDateTime = TimeUtility.getLocalDateTime(singleDate);
                year = localDateTime.getYear();
            } catch (DateNotParsedException e) {
                parseExceptions.add(e);
            }
        }

        if (year == null && startDate != null) {
            try {
                var localDateTime = TimeUtility.getLocalDateTime(startDate);
                year = localDateTime.getYear();
            } catch (DateNotParsedException e) {
                parseExceptions.add(e);
            }
        }

        // Default to 0 if the year cannot be parsed
        if (year == null) {
            year = 0;
        }

        var dataCollectionPeriod = new CMMStudyMapper.DataCollectionPeriod(startDate, year, endDate, Collections.emptyMap());
        return new CMMStudyMapper.ParseResults<>(dataCollectionPeriod, parseExceptions);
    }

    @NonNull
    static HashMap<String, List<TermVocabAttributes>> conceptStrategy(List<Element> elementList) {
        var map = new HashMap<String, List<TermVocabAttributes>>();

        for (var element : elementList) {
            termVocabAttributeStrategy(element, true).ifPresent(mappedElement ->
                map.computeIfAbsent(XMLMapper.parseConceptLanguageCode(element), k -> new ArrayList<>()).add(mappedElement)
            );
        }

        return map;
    }

    @NonNull
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
            publisherMap.put(key, new Publisher(abbrMap.getOrDefault(key, PUBLISHER_NOT_AVAIL), nameMap.getOrDefault(key, "")))
        );

        return publisherMap;
    }

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

    static Map<String, Publisher> individualStrategy(Element element) {
        var identification = element.getChild("IndividualIdentification", null);
        if (identification == null) {
            return Collections.emptyMap();
        }

        var map = new HashMap<String, Publisher>();

        var names = identification.getChildren("IndividualName", null);
        for (var name : names) {

            var isPreferred = name.getAttributeValue("isPreferred", (Namespace) null);

            // Use the full name if present
            var fullName = name.getChild("FullName", null);
            if (fullName != null) {
                var string = fullName.getChild("String", null);
                if (string != null) {
                    var lang = getLangOfElement(string);
                    var publisher = new Publisher("", string.getTextTrim());

                    if (Boolean.parseBoolean(isPreferred)) {
                        map.put(lang, publisher);
                    } else {
                        map.putIfAbsent(lang, publisher);
                    }
                }
            }
        }

        return map;
    }

    @NonNull
    static Map<String, List<TermVocabAttributes>> analysisUnitStrategy(List<Element> elementList) {
        var termList = new ArrayList<TermVocabAttributes>(elementList.size());
        for (var element : elementList) {
            termVocabAttributeLifecycleStrategy(element).ifPresent(termList::add);
        }
        return Map.of("", termList);
    }
}
