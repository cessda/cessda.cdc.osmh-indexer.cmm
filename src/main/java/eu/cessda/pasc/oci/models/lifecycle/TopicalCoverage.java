package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;

public record TopicalCoverage(
        ObjectInformation objectInformation,
        java.util.HashMap<String, List<ControlledVocabulary>> subjects,
        java.util.HashMap<String, List<ControlledVocabulary>> keywords
) {
}
