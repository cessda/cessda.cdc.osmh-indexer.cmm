package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

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
public class Publisher {
  @JsonProperty("abbr")
  private String iso2LetterCode;
  @JsonProperty("publisher")
  private String publisher;
}
