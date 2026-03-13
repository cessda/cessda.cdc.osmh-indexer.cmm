package eu.cessda.pasc.oci.models.lifecycle;

sealed public interface DDIObject permits CollectionEvent, DataCollection, Methodology, Organization, PhysicalInstance, Reference, StudyUnit, Universe {
    ObjectInformation objInf();
}
