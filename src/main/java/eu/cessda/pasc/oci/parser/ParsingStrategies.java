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
package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.models.cmmstudy.*;
import lombok.experimental.UtilityClass;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.Optional;

import static eu.cessda.pasc.oci.parser.DocElementParser.getAttributeValue;
import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;
import static java.util.Optional.ofNullable;

/**
 * Placeholder for various strategies to use to extract metadata for each field type.
 *
 * @author moses AT doraventures DOT com
 */
@UtilityClass
class ParsingStrategies {

    // Metadata handling
    private static final String EMPTY_EL = "empty";
    private static final String PUBLISHER_NOT_AVAIL = "Publisher not specified";

    /**
     * Constructs a {@link Country} using the given element.
     * <p>
     * The ISO code is derived from the{@value OaiPmhConstants#ABBR_ATTR} attribute.
     *
     * @param element the {@link Element} to parse.
     * @return an Optional {@link Country}.
     */
    static Optional<Country> countryStrategy(Element element) {
        var builder = Country.builder();
        builder.elementText(cleanCharacterReturns(element.getText()));
        getAttributeValue(element, ABBR_ATTR).ifPresent(builder::isoCode);
        return Optional.of(builder.build());
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
        var agencyBuilder = Pid.builder().elementText(element.getText());
        getAttributeValue(element, AGENCY_ATTR).ifPresent(agencyBuilder::agency);
        return Optional.of(agencyBuilder.build());
    }

    /**
     * Returns an {@link Optional} representing the text of the given element if the element text is not empty,
     * otherwise returns an empty optional.
     *
     * @param element the {@link Element} to parse.
     */
    static Optional<String> nullableElementValueStrategy(Element element) {
        String value = element.getText();
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
        return getAttributeValue(element, CREATOR_AFFILIATION_ATTR)
            .map(valueString -> (element.getText() + " (" + valueString + ")"))
            .or(() -> Optional.of(element.getText()));
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
        return Publisher.builder()
            .abbreviation(getAttributeValue(element, ABBR_ATTR).orElse(PUBLISHER_NOT_AVAIL))
            .name(cleanCharacterReturns(element.getText()))
            .build();
    }

    static Optional<TermVocabAttributes> termVocabAttributeStrategy(Element element, Namespace namespace, boolean hasControlledValue) {
        var conceptVal = ofNullable(element.getChild(CONCEPT_EL, namespace)).orElse(new Element(EMPTY_EL));

        var builder = TermVocabAttributes.builder();
        builder.term(cleanCharacterReturns(element.getText()));
        if (hasControlledValue) {
            builder.vocab(getAttributeValue(conceptVal, VOCAB_ATTR).orElse(""))
                .vocabUri(getAttributeValue(conceptVal, VOCAB_URI_ATTR).orElse(""))
                .id(conceptVal.getText());
        } else {
            builder.vocab(getAttributeValue(element, VOCAB_ATTR).orElse(""))
                .vocabUri(getAttributeValue(element, VOCAB_URI_ATTR).orElse(""))
                .id(getAttributeValue(element, ID_ATTR).orElse(""));
        }
        return Optional.of(builder.build());
    }

    /**
     * Gets the value of the {@value OaiPmhConstants#URI_ATTR} attribute.
     *
     * @param element the {@link Element} to parse.
     * @return the value of the attribute, or an empty string if the attribute was not present.
     */
    static String uriStrategy(Element element) {
        return ofNullable(element.getAttributeValue(URI_ATTR)).orElse("");
    }

    static Optional<VocabAttributes> samplingTermVocabAttributeStrategy(Element element, Namespace namespace, boolean hasControlledValue) {
        //PUG req. only process if element has a <concept>
        return ofNullable(element.getChild(CONCEPT_EL, namespace)).map(conceptVal -> {
            var builder = VocabAttributes.builder();
            if (hasControlledValue) {
                builder.vocab(getAttributeValue(conceptVal, VOCAB_ATTR).orElse(""))
                    .vocabUri(getAttributeValue(conceptVal, VOCAB_URI_ATTR).orElse(""))
                    .id(conceptVal.getText());
            } else {
                builder.vocab(getAttributeValue(element, VOCAB_ATTR).orElse(""))
                    .vocabUri(getAttributeValue(element, VOCAB_URI_ATTR).orElse(""))
                    .id(getAttributeValue(element, ID_ATTR).orElse(""));
            }
            return builder.build();
        });
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
            var builder = DataCollectionFreeText.builder()
                .elementText(element.getText());
            getAttributeValue(element, EVENT_ATTR).ifPresent(builder::event);
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }

    /**
     * Remove return characters (i.e. {@code \n}) from the string.
     */
    static String cleanCharacterReturns(String candidate) {
        return candidate.replace("\n", "").trim();
    }
}
