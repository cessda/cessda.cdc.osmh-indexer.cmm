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

import eu.cessda.pasc.oci.models.cmmstudy.*;
import lombok.experimental.UtilityClass;
import org.jdom2.Element;

import java.util.Optional;
import java.util.function.Function;

import static eu.cessda.pasc.oci.parser.DocElementParser.getAttributeValue;
import static eu.cessda.pasc.oci.parser.DocElementParser.parseTermVocabAttrAndValues;
import static eu.cessda.pasc.oci.parser.HTMLFilter.cleanCharacterReturns;
import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;
import static java.util.Optional.ofNullable;

/**
 * Placeholder for various strategies to use to extract metadata for each field type
 *
 * @author moses AT doraventures DOT com
 */
@UtilityClass
class ParsingStrategies {

  // Metadata handling
  private static final String EMPTY_EL = "empty";
  private static final String DATE_NOT_AVAIL = "Date not specified";
  private static final String AGENCY_NOT_AVAIL = "Agency not specified";
  private static final String COUNTRY_NOT_AVAIL = "Country not specified";
  private static final String PUBLISHER_NOT_AVAIL = "Publisher not specified";

  static Function<Element, Optional<Country>> countryStrategyFunction() {
    return element -> {
      Country country = Country.builder()
          .iso2LetterCode(getAttributeValue(element, ABBR_ATTR).orElse(COUNTRY_NOT_AVAIL))
          .countryName(cleanCharacterReturns(element.getText()))
              .build();
      return Optional.of(country);
    };
  }

  static Function<Element, Optional<Pid>> pidStrategyFunction() {
    return element -> {
      Pid agency = Pid.builder()
              .agency(getAttributeValue(element, AGENCY_ATTR).orElse(AGENCY_NOT_AVAIL))
              .pid(element.getText())
              .build();
      return Optional.of(agency);
    };
  }

  static Function<Element, Optional<String>> nullableElementValueStrategyFunction() {
    return element -> {
      String value = element.getText();
      return value.isEmpty() ? Optional.empty() : Optional.of(value);
    };
  }

  static Function<Element, Optional<String>> creatorStrategyFunction() {
    return element -> Optional.of(
            getAttributeValue(element, CREATOR_AFFILIATION_ATTR).map(
                    valueString -> (element.getText() + " (" + valueString + ")")).orElseGet(element::getText)
    );
  }

  static Function<Element, Publisher> publisherStrategyFunction() {
      return element -> Publisher.builder()
          .abbreviation(getAttributeValue(element, ABBR_ATTR).orElse(PUBLISHER_NOT_AVAIL))
          .name(cleanCharacterReturns(element.getText())).build();
  }

  static Function<Element, Optional<TermVocabAttributes>> termVocabAttributeStrategyFunction(boolean hasControlledValue) {
    return element -> {
      Optional<Element> concept = ofNullable(element.getChild(CONCEPT_EL, DDI_NS));
      Element conceptVal = concept.orElse(new Element(EMPTY_EL));
      TermVocabAttributes vocabValueAttrs = parseTermVocabAttrAndValues(element, conceptVal, hasControlledValue);
      return Optional.of(vocabValueAttrs);
    };
  }

  static Function<Element, String> uriStrategyFunction() {
    return element -> ofNullable(element.getAttributeValue(URI_ATTR)).orElse("");
  }

  static Function<Element, Optional<VocabAttributes>> samplingTermVocabAttributeStrategyFunction(boolean hasControlledValue) {

    return element -> {
      Optional<Element> concept = ofNullable(element.getChild(CONCEPT_EL, DDI_NS));

      //PUG req. only process if element has a <concept>
      return concept.map(conceptVal -> {
        VocabAttributes.VocabAttributesBuilder builder = VocabAttributes.builder();
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
    };
  }

  static Function<Element, Optional<DataCollectionFreeText>> dataCollFreeTextStrategyFunction() {

    return element -> {

      Optional<String> dateAttrValue = getAttributeValue(element, DATE_ATTR);

      // PUG requirement:  Only extract if there is no @date in <collDate>
      if (dateAttrValue.isEmpty()) {
        return Optional.of(DataCollectionFreeText.builder()
                .event(getAttributeValue(element, EVENT_ATTR).orElse(DATE_NOT_AVAIL))
                .dataCollectionFreeText(element.getText())
                .build());
      }

      return Optional.empty();
    };
  }
}
