package eu.cessda.pasc.oci.helpers.exception;

/**
 * Exception for external encountered Exceptions
 *
 * @author moses@doraventures.com
 */
public class ExternalSystemException extends CustomExceptionBase {

  private static final long serialVersionUID = 928798312826959273L;

  public ExternalSystemException(String message) {
    super(message);
  }

  public ExternalSystemException(String message, Throwable cause) {
    super(message, cause);
  }
}
