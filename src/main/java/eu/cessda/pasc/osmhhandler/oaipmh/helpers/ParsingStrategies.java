/**
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.*;
import org.jdom2.Element;

import java.util.Optional;
import java.util.function.Function;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HTMLFilter.CLEAN_CHARACTER_RETURNS_STRATEGY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.EMPTY_EL;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.NOT_AVAIL;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;
import static java.util.Optional.ofNullable;

/**
 * Placeholder for various strategies to use to extract metadata for each field type
 *
 * @author moses AT doraventures DOT com
 */
class ParsingStrategies {

  private ParsingStrategies() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> countryStrategyFunction() {
    return element -> {
      Country country = Country.builder()
          .iso2LetterCode(getAttributeValue(element, ABBR_ATTR).orElse(NOT_AVAIL))
          .countryName(CLEAN_CHARACTER_RETURNS_STRATEGY.apply(element.getText()))
          .build();
      return Optional.of((T) country);
    };
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> pidStrategyFunction() {
    return element -> {
      Pid agency = Pid.builder()
          .agency(getAttributeValue(element, AGENCY_ATTR).orElse(NOT_AVAIL))
          .pid(element.getText())
          .build();
      return Optional.of((T) agency);
    };
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> nullableElementValueStrategyFunction() {
    return element -> {
      String value = element.getText();
      return (value.isEmpty())
          ? Optional.empty() :
          Optional.of((T) value);
    };
  }

  @SuppressWarnings("unchecked")
  static Function<Element, Optional<String>> creatorStrategyFunction() {
    return element -> Optional.of(
        getAttributeValue(element, CREATOR_AFFILIATION_ATTR).map(
            valueString -> (element.getText() + " (" + valueString + ")")).orElseGet(element::getText)
    );
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, T> publisherStrategyFunction() {
    return element -> (T) Publisher.builder()
        .iso2LetterCode(getAttributeValue(element, ABBR_ATTR).orElse(NOT_AVAIL))
        .publisher(CLEAN_CHARACTER_RETURNS_STRATEGY.apply(element.getText())).build();
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> termVocabAttributeStrategyFunction(boolean hasControlledValue) {
    return element -> {
      Optional<Element> concept = ofNullable(element.getChild(CONCEPT_EL, DDI_NS));
      Element conceptVal = concept.orElse(new Element(EMPTY_EL));
      TermVocabAttributes vocabValueAttrs = parseTermVocabAttrAndValues(element, conceptVal, hasControlledValue);
      return Optional.of((T) vocabValueAttrs);
    };
  }

  static Function<Element, String> uriStrategyFunction() {
    return element -> ofNullable(element.getAttributeValue(URI_ATTR)).orElse("");
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> samplingTermVocabAttributeStrategyFunction(boolean hasControlledValue) {

    return element -> {
      Optional<T> vocabValueAttrsOpt = Optional.empty();
      Optional<Element> concept = ofNullable(element.getChild(CONCEPT_EL, DDI_NS));
      if (concept.isPresent()) {  //PUG req. only process if element has a <concept>
        Element conceptVal = concept.orElse(new Element(EMPTY_EL));
        VocabAttributes vocabValueAttrs = parseVocabAttrAndValues(element, conceptVal, hasControlledValue);
        vocabValueAttrsOpt = Optional.of((T) vocabValueAttrs);
      }
      return vocabValueAttrsOpt;
    };
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> dataCollFreeTextStrategyFunction() {

    return element -> {

      Optional<T> dataCollFTxt = Optional.empty();
      Optional<String> dateAttrValue = getAttributeValue(element, DATE_ATTR);

      // PUG requirement:  Only extract if there is no @date in <collDate>
      if (!dateAttrValue.isPresent()) {
        dataCollFTxt = Optional.of((T) DataCollectionFreeText.builder()
            .event(getAttributeValue(element, EVENT_ATTR).orElse(NOT_AVAIL))
            .dataCollectionFreeText(element.getText())
            .build());
      }

      return dataCollFTxt;
    };
  }
}
