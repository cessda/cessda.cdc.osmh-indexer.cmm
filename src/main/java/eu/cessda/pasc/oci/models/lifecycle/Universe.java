package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record Universe(
        ObjectInformation objInf,
        Map<String, String> universeName,
        Map<String, String> label
) implements DDIObject {
}
