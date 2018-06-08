package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HTMLFilter.CLEAN_CHARACTER_RETURNS_STRATEGY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HTMLFilter.CLEAN_MAP_VALUES;
import static org.assertj.core.api.BDDAssertions.then;

public class HTMLFilterTest {

  @Test
  public void shouldCleanOutHtmlReturnCharacters() {

    // Given
    String raw = "\n\"Arma sunt necessaria\" (Arms are necessary) Guns, " +
        "Gun Culture and Cultural Origins of the Second \nAmendment to the U.S. Constitution\n";

    // When
    String actualCleanText = CLEAN_CHARACTER_RETURNS_STRATEGY.apply(raw);

    then(actualCleanText).isEqualTo("\"Arma sunt necessaria\" (Arms are necessary) Guns, " +
        "Gun Culture and Cultural Origins of the Second Amendment to the U.S. Constitution");
  }


  @Test
  public void shouldCleanOutHtmlReturnCharactersFromMap() {

    // Given
    Map<String, String> titleMap = new HashMap<>();
    titleMap.put("en", "\n\"Arma sunt necessaria\" (Arms are necessary) Guns Constitution\n");
    titleMap.put("sv", "\n\"Arma sunt necessaria\" (Arms are necessary) Guns Constitution\n");
    titleMap.put("fi", "\nDocumentation pour \"European Social Survey in Switzerland - 2004\"");


    // When
    CLEAN_MAP_VALUES.accept(titleMap);

    then(titleMap.get("en")).isEqualTo("\"Arma sunt necessaria\" (Arms are necessary) Guns Constitution");
    then(titleMap.get("sv")).isEqualTo("\"Arma sunt necessaria\" (Arms are necessary) Guns Constitution");
    then(titleMap.get("fi")).isEqualTo("Documentation pour \"European Social Survey in Switzerland - 2004\"");
  }
}