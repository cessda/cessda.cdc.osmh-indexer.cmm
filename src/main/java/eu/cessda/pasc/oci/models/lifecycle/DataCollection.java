package eu.cessda.pasc.oci.models.lifecycle;

public record DataCollection(
        ObjectInformation objInf,
        CollectionEvent collectionEvent,
        Reference methodologyReference
) implements DDIObject {
}
