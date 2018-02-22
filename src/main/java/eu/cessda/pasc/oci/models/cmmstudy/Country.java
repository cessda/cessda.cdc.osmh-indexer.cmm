package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Country pojo to hold
 * - "abbr": "ISO 3166 2-letter code for country 1",
 * - "country": "The name of the country in a given Language"
 *
 * @author moses@doraventures.com
 */
@Builder
@Getter
public class Country {
  @JsonProperty("abbr")
  private String iso2LetterCode;
  @JsonProperty("country")
  private String countryName;
}
