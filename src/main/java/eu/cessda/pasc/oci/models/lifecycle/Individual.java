package eu.cessda.pasc.oci.models.lifecycle;

public record Individual(
        ObjectInformation objInf,
        IndividualIdentification individualIdentification) implements DDIObject {
}
