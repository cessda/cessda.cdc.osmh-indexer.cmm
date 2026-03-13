package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;

public record PhysicalInstance(
        ObjectInformation objInf,
        Citation citation,
        List<String> dataFileUris
) implements DDIObject {
}
