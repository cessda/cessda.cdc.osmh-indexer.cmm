package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record Contributor(
        Reference contributorReference,
        ControlledVocabulary contributorRole,
        Map<String, String> contributorName
) {
}
