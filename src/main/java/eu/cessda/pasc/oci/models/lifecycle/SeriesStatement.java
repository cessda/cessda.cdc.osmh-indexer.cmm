package eu.cessda.pasc.oci.models.lifecycle;

import java.util.List;
import java.util.Map;

public record SeriesStatement(
        List<String> seriesRepositoryLocation,
        Map<String, List<String>> seriesName,
        Map<String, String> seriesDescription
) {
}
