package eu.cessda.pasc.oci.models.lifecycle;

public record Methodology(
        ObjectInformation objInf,
        SamplingProcedure samplingProcedure,
        TimeMethod timeMethod
) implements DDIObject {
}
