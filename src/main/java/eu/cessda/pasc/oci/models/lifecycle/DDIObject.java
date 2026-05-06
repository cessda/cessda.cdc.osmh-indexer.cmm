package eu.cessda.pasc.oci.models.lifecycle;

sealed public interface DDIObject permits Access, Archive, CollectionEvent, DataCollection, Individual, Methodology, Organization, PhysicalInstance, Reference, StudyUnit, Universe {
    ObjectInformation objInf();
}
