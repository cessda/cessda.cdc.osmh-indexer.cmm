/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model representing a CMMStudy. Used to deserialize documents from json.
 *
 * @author moses AT doraventures DOT com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "studyNumber",
    "titleStudy",
    "abstract",
    "classifications",
    "keywords",
    "typeOfTimeMethods",
    "studyAreaCountries",
        "unitTypes",
        "publisher",
        "publicationYear",
        "pidStudies",
        "fileLanguages",
        "creators",
        "typeOfSamplingProcedures",
        "samplingProcedureFreeTexts",
        "typeOfModeOfCollections",
        "dataCollectionPeriodStartdate",
        "dataCollectionPeriodEnddate",
        "dataCollectionYear",
    "dataCollectionFreeTexts",
    "dataAccessFreeTexts",
    "lastModified",
    "studyUrl",
    "isActive",
    "langAvailableIn",
    "studyXmlSourceUrl"
})
@AllArgsConstructor
@Builder
@Value
@With
@SuppressWarnings("ReferenceEquality")
public class CMMStudy {

    @JsonProperty("creators")
    Map<String, List<String>> creators;

    @JsonProperty("dataCollectionPeriodStartdate")
    String dataCollectionPeriodStartdate;

    @JsonProperty("dataCollectionPeriodEnddate")
    String dataCollectionPeriodEnddate;

    @JsonProperty("dataCollectionYear")
    int dataCollectionYear;

    @JsonProperty("dataCollectionFreeTexts")
    Map<String, List<DataCollectionFreeText>> dataCollectionFreeTexts;

    @JsonProperty("dataAccessFreeTexts")
    Map<String, List<String>> dataAccessFreeTexts;

    @JsonProperty("publicationYear")
    String publicationYear;

    @JsonProperty("typeOfModeOfCollections")
    Map<String, List<TermVocabAttributes>> typeOfModeOfCollections;

    @JsonProperty("keywords")
    Map<String, List<TermVocabAttributes>> keywords;

    @JsonProperty("samplingProcedureFreeTexts")
    Map<String, List<String>> samplingProcedureFreeTexts;

    @JsonProperty("classifications")
    Map<String, List<TermVocabAttributes>> classifications;

    @JsonProperty("abstract")
    Map<String, String> abstractField;

    @JsonProperty("titleStudy")
    Map<String, String> titleStudy;

    @JsonProperty("studyUrl")
    Map<String, URI> studyUrl;

    @JsonProperty("studyNumber")
    String studyNumber;

    @JsonProperty("typeOfTimeMethods")
    Map<String, List<TermVocabAttributes>> typeOfTimeMethods;

    @JsonProperty("fileLanguages")
    Set<String> fileLanguages;

    @JsonProperty("typeOfSamplingProcedures")
    Map<String, List<VocabAttributes>> typeOfSamplingProcedures;

    @JsonProperty("publisher")
    Map<String, Publisher> publisher;

    @JsonProperty("studyAreaCountries")
    Map<String, List<Country>> studyAreaCountries;

    @JsonProperty("unitTypes")
    Map<String, List<TermVocabAttributes>> unitTypes;

    @JsonProperty("pidStudies")
    Map<String, List<Pid>> pidStudies;

    @JsonProperty("lastModified")
    String lastModified;

    @JsonProperty("isActive")
    boolean active;

    @JsonProperty("studyXmlSourceUrl")
    String studyXmlSourceUrl;
}
