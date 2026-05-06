package eu.cessda.pasc.oci.models.lifecycle;

public record ControlledVocabulary(
        String id,
        String name,
        String agencyName,
        String versionId,
        String urn,
        String content
) {
}
