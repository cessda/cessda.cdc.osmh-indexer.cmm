package eu.cessda.pasc.oci.models.lifecycle;

import java.util.Map;

public record SamplingProcedure(
        ObjectInformation objInf,
        ControlledVocabulary cv,
        String typeOfSamplingProcedure,
        Map<String, String> content
) {
}
