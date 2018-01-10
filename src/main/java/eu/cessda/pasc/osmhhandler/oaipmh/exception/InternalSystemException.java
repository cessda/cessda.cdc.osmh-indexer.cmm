package eu.cessda.pasc.osmhhandler.oaipmh.exception;

/**
 * Exception for internally encountered Exceptions
 *
 * @author moses@doraventures.com
 */
public class InternalSystemException extends Exception{

  public InternalSystemException(String message) {
    super(message);
  }
}
