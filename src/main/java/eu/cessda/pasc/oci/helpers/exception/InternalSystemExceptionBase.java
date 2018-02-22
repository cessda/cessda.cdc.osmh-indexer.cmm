package eu.cessda.pasc.oci.helpers.exception;

/**
 * Exception for internally encountered Exceptions
 *
 * @author moses@doraventures.com
 */
public class InternalSystemExceptionBase extends CustomExceptionBase {

  private static final long serialVersionUID = -1848837478104997356L;

  public InternalSystemExceptionBase(String message) {
    super(message);
  }

  public InternalSystemExceptionBase(String message, Throwable cause) {
    super(message, cause);
  }
}
