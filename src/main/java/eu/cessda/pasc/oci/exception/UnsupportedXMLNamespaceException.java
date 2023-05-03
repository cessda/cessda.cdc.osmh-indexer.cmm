package eu.cessda.pasc.oci.exception;

import org.jdom2.Namespace;

import java.io.Serial;

/**
 * Thrown when attempting to parse a DDI document with an unsupported XML namespace.
 */
public class UnsupportedXMLNamespaceException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = -5524959625579025110L;

    private final Namespace namespace;

    /**
     * Constructs an UnsupportedXMLNamespaceException with the specified namespace.
     * @param namespace the namespace.
     */
    public UnsupportedXMLNamespaceException(Namespace namespace) {
        super("XML namespace \"" + namespace.getURI() + "\" not supported");
        this.namespace = namespace;
    }

    public Namespace getNamespace() {
        return namespace;
    }
}
