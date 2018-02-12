package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.Country;
import org.jdom2.Element;

import java.util.function.Function;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.DocElementParser.getAttributeValue;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.ABBR_ATTR;

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
  static <T> Function<Element, T> countryParserStrategyFunction() {
    return element -> (T) Country.builder()
        .iso2LetterCode(getAttributeValue(element, ABBR_ATTR).orElse(""))
        .countryName(element.getText()).build();
  }
}
