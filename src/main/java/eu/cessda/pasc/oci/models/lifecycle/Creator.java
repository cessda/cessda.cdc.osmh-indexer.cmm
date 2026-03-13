package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record Creator(Reference creatorReference, Map<String, String> creatorName) {
}
