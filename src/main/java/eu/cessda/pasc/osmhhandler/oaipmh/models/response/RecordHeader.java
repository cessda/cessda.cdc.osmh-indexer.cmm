package eu.cessda.pasc.osmhhandler.oaipmh.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

/**
 *
 * @author moses@doraventures.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "lastModified",
    "type",
    "recordType",
    "identifier"
})
@Builder
public class RecordHeader {

  @JsonProperty("lastModified")
  private String lastModified;
  @JsonProperty("type")
  private String type;
  @JsonProperty("recordType")
  private String recordType;
  @JsonProperty("identifier")
  private String identifier;
}
