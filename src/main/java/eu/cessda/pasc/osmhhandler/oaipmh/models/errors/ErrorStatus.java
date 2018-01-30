package eu.cessda.pasc.osmhhandler.oaipmh.models.errors;

import lombok.Builder;
import lombok.Getter;

/**
 * Internal placeholder for error status and messages.
 *
 * @author moses@doraventures.com
 */
@Getter
@Builder
public class ErrorStatus {
  String message;
  boolean hasError;
}