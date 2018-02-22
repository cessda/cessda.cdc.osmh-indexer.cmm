package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Pid - Study Persistent Identifier pojo to hold
 * <p>
 * <pre>
 * {@code
 * {
 *  "agency": "the agency of the pid in <Finnish>",
 *  "pid": "The pid"
 * }
 *}
 * </pre>
 *
 * @author moses@doraventures.com
 */
@Builder
@Getter
public class Pid {
  @JsonProperty("agency")
  private String agency;
  @JsonProperty("pid")
  private String pid;
}
