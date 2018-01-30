package eu.cessda.pasc.osmhhandler.oaipmh.exception;

/**
 * Exception for internally encountered Exceptions
 *
 * @author moses@doraventures.com
 */
public class InternalSystemException extends CustomHandlerException{

  private static final long serialVersionUID = -1848837478104997356L;

  public InternalSystemException(String message) {
    super(message);
  }

  public InternalSystemException(String message, Throwable cause) {
    super(message, cause);
  }
}
