package eu.cessda.pasc.osmhhandler.oaipmh.exception;

/**
 * Exception for internally encountered Exceptions
 *
 * @author moses@doraventures.com
 */
public class CustomHandlerException extends Exception{

  private static final long serialVersionUID = 5715687019114712665L;

  public CustomHandlerException(String message) {
    super(message);
  }

  CustomHandlerException(String message, Throwable cause) {
    super(message, cause);
  }
}
