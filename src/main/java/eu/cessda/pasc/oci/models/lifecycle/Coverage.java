package eu.cessda.pasc.oci.models.lifecycle;

public record Coverage(
        TopicalCoverage topicalCoverage,
        SpatialCoverage spatialCoverage,
        TemporalCoverage temporalCoverage
) {
}
