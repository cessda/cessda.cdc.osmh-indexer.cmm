package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record Organization(
        ObjectInformation objInf,
        Map<String, String> names
) implements DDIObject {
}
