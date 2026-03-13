package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;
import java.util.Map;

public record StudyUnit(
        ObjectInformation objInf,
        Citation citation,
        Map<String, String> abstractMap,
        Reference universe,
        List<FundingInformation> fundingInformation,
        Coverage coverage,
        List<ControlledVocabulary> analysisUnit,
        List<ControlledVocabulary> kindOfData,
        List<Reference> dataCollectionReference,
        List<Reference> physicalInstanceReference,
        List<Reference> archiveReference
) implements DDIObject {
}
