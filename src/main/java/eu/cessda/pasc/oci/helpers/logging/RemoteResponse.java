package eu.cessda.pasc.oci.helpers.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.logging.LogLevel;

import java.time.LocalDateTime;

/**
 * Pojo to hold details of remote detail. To be used for logging JSON
 *
 * @author moses@doraventures.com
 */
@JsonInclude()
@JsonPropertyOrder({
    "logLevel",
    "responseCode",
    "responseMessage",
    "occurredAt"
})
@Builder
@Getter
@ToString
public class RemoteResponse {
  LogLevel logLevel;
  int responseCode;
  String responseMessage;
  LocalDateTime occurredAt;
}
