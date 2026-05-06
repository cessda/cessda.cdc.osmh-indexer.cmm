package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;

public record DataCollection(
        ObjectInformation objInf,
        List<CollectionEvent> collectionEvent,
        Reference methodologyReference
) implements DDIObject {
}
