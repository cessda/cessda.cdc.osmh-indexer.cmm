package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * DataCollectionFreeText pojo to hold
 * "dataCollectionFreeText": "Free text 1 in a given Language",
 * "event": "event start, single or end"
 *
 * @author moses@doraventures.com
 */
@Builder
@Getter
@AllArgsConstructor
public class DataCollectionFreeText {

  @JsonProperty("dataCollectionFreeText")
  private String dataCollectionFreeText;
  @JsonProperty("event")
  private String event;
}