package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record SamplingProcedure(
        ObjectInformation objInf,
        ControlledVocabulary typeOfSamplingProcedure,
        Map<String, String> content
) {
}
