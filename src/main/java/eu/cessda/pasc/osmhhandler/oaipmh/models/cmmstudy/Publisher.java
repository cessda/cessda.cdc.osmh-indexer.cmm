package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Publisher pojo to hold
 * - "abbr": "Control Value(CV) for institution abbreviation.",
 * - "publisher": "e.g The Social Science Data Archive"
 *
 * Publisher value can be in multiple language translations.
 *
 * @author moses@doraventures.com
 */
@Builder
@Getter
public class Publisher {
  @JsonProperty("abbr")
  private String iso2LetterCode;
  @JsonProperty("publisher")
  private String publisher;
}
