package eu.cessda.pasc.oci.exception;

import java.net.URISyntaxException;

/**
 * Exception thrown when a URI cannot be parsed. This is an unchecked version of {@link URISyntaxException}.
 */
public class InvalidURIException extends RuntimeException {
    public InvalidURIException(URISyntaxException e) {
        super(e.getMessage(), e);
    }
}
