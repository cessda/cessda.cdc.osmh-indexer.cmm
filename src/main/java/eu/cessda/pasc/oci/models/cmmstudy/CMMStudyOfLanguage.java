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
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Model representing a CMMStudy.
 *
 * @author moses AT doraventures DOT com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Value
@With
@SuppressWarnings("ReferenceEquality")
public class CMMStudyOfLanguage {

    String id;

    @JsonProperty("code")
    String code;

    @JsonProperty("creators")
    List<String> creators;

    @JsonProperty("dataCollectionPeriodStartdate")
    String dataCollectionPeriodStartdate;

    @JsonProperty("dataCollectionPeriodEnddate")
    String dataCollectionPeriodEnddate;

    @JsonProperty("dataCollectionYear")
    int dataCollectionYear;

    @JsonProperty("dataCollectionFreeTexts")
    List<DataCollectionFreeText> dataCollectionFreeTexts;

    @JsonProperty("dataAccessFreeTexts")
    List<String> dataAccessFreeTexts;

    @JsonProperty("publicationYear")
    String publicationYear;

    @JsonProperty("typeOfModeOfCollections")
    List<TermVocabAttributes> typeOfModeOfCollections;

    @JsonProperty("keywords")
    List<TermVocabAttributes> keywords;

    @JsonProperty("samplingProcedureFreeTexts")
    List<String> samplingProcedureFreeTexts;

    @JsonProperty("classifications")
    List<TermVocabAttributes> classifications;

    @JsonProperty("abstract")
    String abstractField;

    @JsonProperty("titleStudy")
    String titleStudy;

    @JsonProperty("studyUrl")
    URI studyUrl;

    @JsonProperty("studyNumber")
    String studyNumber;

    @JsonProperty("typeOfTimeMethods")
    List<TermVocabAttributes> typeOfTimeMethods;

    @JsonProperty("fileLanguages")
    Set<String> fileLanguages;

    @JsonProperty("typeOfSamplingProcedures")
    List<VocabAttributes> typeOfSamplingProcedures;

    @JsonProperty("publisher")
    Publisher publisher;

    @JsonProperty("publisherFilter")
    Publisher publisherFilter;

    @JsonProperty("studyAreaCountries")
    List<Country> studyAreaCountries;

    @JsonProperty("unitTypes")
    List<TermVocabAttributes> unitTypes;

    @JsonProperty("pidStudies")
    List<Pid> pidStudies;

    @JsonProperty("universes")
    List<Universe> universes;

    @JsonProperty("lastModified")
    String lastModified;

    @JsonProperty("isActive")
    boolean active;

    @JsonProperty("langAvailableIn")
    Set<String> langAvailableIn;

    @JsonProperty("studyXmlSourceUrl")
    String studyXmlSourceUrl;
}
