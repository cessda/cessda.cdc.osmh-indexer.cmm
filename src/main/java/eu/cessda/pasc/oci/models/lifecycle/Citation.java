package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;
import java.util.Map;

public record Citation(
        Map<String, String> title,
        Creator creator,
        Publisher publisher,
        List<Contributor> contributors,
        DateType publicationDate,
        InternationalIdentifier internationalIdentifier
) {
}
