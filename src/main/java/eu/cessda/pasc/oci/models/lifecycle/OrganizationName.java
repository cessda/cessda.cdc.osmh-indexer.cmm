package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record OrganizationName(
        Map<String, String> names,
        Map<String, String> abbreviations
) {
}
