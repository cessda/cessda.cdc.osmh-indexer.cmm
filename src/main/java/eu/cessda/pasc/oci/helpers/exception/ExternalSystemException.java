package eu.cessda.pasc.oci.helpers.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Exception for external encountered Exceptions
 *
 * @author moses@doraventures.com
 */
public class ExternalSystemException extends CustomExceptionBase {

  private static final long serialVersionUID = 928798312826959273L;

  @Getter
  @Setter
  private String externalResponseBody;

  public ExternalSystemException(String message, Throwable cause) {
    super(message, cause);
  }
}
