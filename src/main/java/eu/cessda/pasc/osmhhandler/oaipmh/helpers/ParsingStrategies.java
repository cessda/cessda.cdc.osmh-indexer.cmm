package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.Country;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.Publisher;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.TermVocabAttributes;
import org.jdom2.Element;

import java.util.Optional;
import java.util.function.Function;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.getAttributeValue;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.parseTermVocabAttrAndValues;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.NOT_AVAIL;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.ABBR_ATTR;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.DDI_NS;

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
  static <T> Function<Element, T> countryStrategyFunction() {
    return element -> (T) Country.builder()
        .iso2LetterCode(getAttributeValue(element, ABBR_ATTR).orElse(NOT_AVAIL))
        .countryName(element.getText()).build();
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, T> publisherStrategyFunction() {
    return element -> (T) Publisher.builder()
        .iso2LetterCode(getAttributeValue(element, ABBR_ATTR).orElse(NOT_AVAIL))
        .publisher(element.getText()).build();
  }

  @SuppressWarnings("unchecked")
  static <T> Function<Element, T> termVocabAttributeStrategyFunction() {
    return element -> {
      Optional<Element> concept = Optional.ofNullable(element.getChild("concept", DDI_NS));
      TermVocabAttributes vocabValueAttrs =
          parseTermVocabAttrAndValues(element, concept.orElse(new Element("empty")));

      return (T)vocabValueAttrs;
    };
  }
}
