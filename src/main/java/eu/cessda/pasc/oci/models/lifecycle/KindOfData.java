package eu.cessda.pasc.oci.models.lifecycle;

public record KindOfData(
        ControlledVocabulary controlledVocabulary,
        String type
) {
}
