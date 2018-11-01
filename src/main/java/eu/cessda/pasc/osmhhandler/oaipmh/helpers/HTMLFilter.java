package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.InternalSystemException;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for cleaning html characters
 *
 * @author moses@doraventures.com
 */
class HTMLFilter {

  private HTMLFilter() throws InternalSystemException {
    throw new InternalSystemException("Unsupported operation");
  }

  static final Function<String, String> CLEAN_CHARACTER_RETURNS_STRATEGY = candidate ->
      candidate.replace("\n", "").trim();

  static final Consumer<Map<String, String>> CLEAN_MAP_VALUES = candidateMap ->
      candidateMap.replaceAll((key, value) -> CLEAN_CHARACTER_RETURNS_STRATEGY.apply(value));
}
