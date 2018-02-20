package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.*;
import org.jdom2.Element;

import java.util.Optional;
import java.util.function.Function;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.getAttributeValue;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.parseTermVocabAttrAndValues;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.EMPTY_EL;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.NOT_AVAIL;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;

/**
 * Placeholder for various strategies to use to extract metadata for each field type
 *
 * @author moses@doraventures.com
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
          .countryName(element.getText())
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
        .publisher(element.getText()).build();
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> termVocabAttributeStrategyFunction() {
    return element -> {
      Optional<Element> concept = Optional.ofNullable(element.getChild(CONCEPT_EL, DDI_NS));
      TermVocabAttributes vocabValueAttrs = parseTermVocabAttrAndValues(element, concept.orElse(new Element(EMPTY_EL)));
      return Optional.of((T) vocabValueAttrs);
    };
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> samplingTermVocabAttributeStrategyFunction() {

    return element -> {
      Optional<T> vocabValueAttrsOpt = Optional.empty();
      Optional<Element> concept = Optional.ofNullable(element.getChild(CONCEPT_EL, DDI_NS));
      if (concept.isPresent()) {
        TermVocabAttributes vocabValueAttrs = parseTermVocabAttrAndValues(element, concept.orElse(new Element(EMPTY_EL)));
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
