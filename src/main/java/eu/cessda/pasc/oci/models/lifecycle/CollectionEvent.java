package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;

public record CollectionEvent(
        ObjectInformation objInf,
        DateType collectionDate,
        List<ModeOfCollection> modesOfCollection
) implements DDIObject {
}
