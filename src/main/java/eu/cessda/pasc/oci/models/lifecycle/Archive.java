package eu.cessda.pasc.oci.models.lifecycle;

public record Archive(
        ObjectInformation objInf,
        Access access
) implements DDIObject {
}
