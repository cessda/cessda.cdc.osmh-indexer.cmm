package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;

public record Methodology(
        ObjectInformation objInf,
        SamplingProcedure samplingProcedure,
        List<TimeMethod> timeMethod
) implements DDIObject {
}
