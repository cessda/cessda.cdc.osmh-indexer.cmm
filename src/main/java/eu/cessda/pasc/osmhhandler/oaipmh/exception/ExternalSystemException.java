package eu.cessda.pasc.osmhhandler.oaipmh.exception;

/**
 * Exception for internally encountered Exceptions
 *
 * @author moses@doraventures.com
 */
public class ExternalSystemException extends CustomHandlerException{

  private static final long serialVersionUID = 928798312826959273L;

  public ExternalSystemException(String message) {
    super(message);
  }

  public ExternalSystemException(String message, Throwable cause) {
    super(message, cause);
  }
}
