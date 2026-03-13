package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;

public record TopicalCoverage(
        ObjectInformation objectInformation,
        List<ControlledVocabulary> subjects,
        List<ControlledVocabulary> keywords
) {
}
