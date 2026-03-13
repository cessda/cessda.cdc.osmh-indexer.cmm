package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record Publisher(Reference publisherReference, Map<String, String> publisherName) {
}
