package eu.cessda.pasc.osmhhandler.oaipmh.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Exception for internally encountered Exceptions
 *
 * @author moses@doraventures.com
 */
public class ExternalSystemException extends CustomHandlerException{

  private static final long serialVersionUID = 928798312826959273L;

  @Getter
  @Setter
  private String externalResponseBody;

  public ExternalSystemException(String message) {
    super(message);
  }

  public ExternalSystemException(String message, Throwable cause) {
    super(message, cause);
  }
}
