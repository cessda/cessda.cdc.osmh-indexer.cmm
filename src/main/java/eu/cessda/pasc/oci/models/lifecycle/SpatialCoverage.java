package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;
import java.util.Map;

public record SpatialCoverage(
        ObjectInformation objectInformation,
        Map<String, String> description,
        List<String> countryCodes
) {
}
