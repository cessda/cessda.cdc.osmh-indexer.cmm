/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.models.cmmstudy;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.With;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Model representing a CMMStudy.
 *
 * @param lastModified Internal metadata
 * @author moses AT doraventures DOT com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"isActive"})
@Builder
@With
public record CMMStudyOfLanguage(
    String id,
    @JsonProperty("abstract") String abstractField,
    @JsonProperty("classifications") List<TermVocabAttributes> classifications,
    @JsonProperty("code") String code,
    @JsonProperty("creators") List<Creator> creators,
    @JsonProperty("dataAccess") String dataAccess,
    @JsonProperty("dataAccessFreeTexts") List<String> dataAccessFreeTexts,
    @JsonProperty("dataAccessUrl") URI dataAccessUrl,
    @JsonProperty("dataCollectionPeriodStartdate") String dataCollectionPeriodStartdate,
    @JsonProperty("dataCollectionPeriodEnddate") String dataCollectionPeriodEnddate,
    @JsonProperty("dataCollectionYear") Integer dataCollectionYear,
    @JsonProperty("dataCollectionFreeTexts") List<DataCollectionFreeText> dataCollectionFreeTexts,
    @JsonProperty("dataKindFreeTexts") List<DataKindFreeText> dataKindFreeTexts,
    @JsonProperty("fileLanguages") Set<String> fileLanguages,
    @JsonProperty("funding") List<Funding> funding,
    @JsonProperty("generalDataFormats") List<TermVocabAttributes> generalDataFormats,
    @JsonProperty("keywords") List<TermVocabAttributes> keywords,
    @JsonProperty("pidStudies") List<Pid> pidStudies,
    @JsonProperty("publicationYear") String publicationYear,
    @JsonProperty("publisher") Publisher publisher,
    @JsonProperty("publisherFilter") Publisher publisherFilter,
    @JsonProperty("relatedPublications") List<RelatedPublication> relatedPublications,
    @JsonProperty("samplingProcedureFreeTexts") List<String> samplingProcedureFreeTexts,
    @JsonProperty("series") List<Series> series,
    @JsonProperty("studyAreaCountries") List<Country> studyAreaCountries,
    @JsonProperty("studyNumber") String studyNumber,
    @JsonProperty("studyUrl") URI studyUrl,
    @JsonProperty("typeOfModeOfCollections") List<TermVocabAttributes> typeOfModeOfCollections,
    @JsonProperty("titleStudy") String titleStudy,
    @JsonProperty("typeOfTimeMethods") List<TermVocabAttributes> typeOfTimeMethods,
    @JsonProperty("typeOfSamplingProcedures") List<VocabAttributes> typeOfSamplingProcedures,
    @JsonProperty("unitTypes") List<TermVocabAttributes> unitTypes,
    @JsonProperty("universe") Universe universe,
    @JsonProperty("lastModified") String lastModified,
    @JsonProperty("langAvailableIn") Set<String> langAvailableIn,
    @JsonProperty("studyXmlSourceUrl") String studyXmlSourceUrl
) {

}
