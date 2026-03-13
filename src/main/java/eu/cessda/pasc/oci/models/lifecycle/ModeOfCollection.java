package eu.cessda.pasc.oci.models.lifecycle;

public record ModeOfCollection(
        ObjectInformation objInf,
        ControlledVocabulary cv,
        String modeOfCollection
) {
}
