package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.Country;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.Pid;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.Publisher;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.TermVocabAttributes;
import org.jdom2.Element;

import java.util.Optional;
import java.util.function.Function;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.getAttributeValue;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.parseTermVocabAttrAndValues;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.*;
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
      return Optional.ofNullable((T) country);
    };
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> pidStrategyFunction() {
    return element -> {
      Pid agency = Pid.builder()
          .agency(getAttributeValue(element, AGENCY_ATTR).orElse(NOT_AVAIL))
          .pid(element.getText())
          .build();
      return Optional.ofNullable((T) agency);
    };
  }

  @SuppressWarnings("unchecked")
  static Function<Element, String> creatorStrategyFunction() {
    return element -> getAttributeValue(element, CREATOR_AFFILIATION_ATTR)
        .map(s -> (element.getText() + " (" + s + ")"))
        .orElseGet(element::getText);
  }

  @SuppressWarnings("unchecked")
  static Function<Element, String> rawTextStrategyFunction() {
    return Element::getText;
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
      return Optional.ofNullable((T) vocabValueAttrs);
    };
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, Optional<T>> samplingTermVocabAttributeStrategyFunction() {

    return element -> {

      Optional<T> vocabValueAttrs1 = Optional.empty();
      Optional<Element> concept = Optional.ofNullable(element.getChild(CONCEPT_EL, DDI_NS));
      if (concept.isPresent()) {
        TermVocabAttributes vocabValueAttrs = parseTermVocabAttrAndValues(element, concept.orElse(new Element(EMPTY_EL)));
        vocabValueAttrs1 = Optional.ofNullable((T) vocabValueAttrs);
      }

      return vocabValueAttrs1;
    };
  }
}
