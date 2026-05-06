package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record Access(
        ObjectInformation objInf,
        Map<String, String> accessTypeName,
        Map<String, String> accessDescription
) implements DDIObject {
}
