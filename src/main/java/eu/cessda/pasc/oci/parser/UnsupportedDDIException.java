package eu.cessda.pasc.oci.parser;

import javax.xml.namespace.QName;
import java.io.Serial;

public class UnsupportedDDIException extends Exception {
    @Serial
    private static final long serialVersionUID = -5334076698095582036L;

    public UnsupportedDDIException(QName qname) {
        super("Unsupported DDI element \"" + qname + "\"");
    }
}
