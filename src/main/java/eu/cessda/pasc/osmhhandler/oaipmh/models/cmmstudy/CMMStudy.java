/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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

package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model representing a CMMStudy.
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
    "studyXmlSourceUrl"
})
@Builder
@Getter
@ToString
public class CMMStudy {

  @JsonProperty("creators")
  private Map<String, List<String>> creators;

  @JsonProperty("dataCollectionPeriodStartdate")
  private String dataCollectionPeriodStartdate;

  @JsonProperty("dataCollectionPeriodEnddate")
  private String dataCollectionPeriodEnddate;

  @JsonProperty("dataCollectionYear")
  private int dataCollectionYear;

  @JsonProperty("dataCollectionFreeTexts")
  private Map<String, List<DataCollectionFreeText>> dataCollectionFreeTexts;

  @JsonProperty("dataAccessFreeTexts")
  private Map<String, List<String>> dataAccessFreeTexts;

  @JsonProperty("publicationYear")
  private String publicationYear;

  @JsonProperty("typeOfModeOfCollections")
  private Map<String, List<TermVocabAttributes>> typeOfModeOfCollections;

  @JsonProperty("keywords")
  private Map<String, List<TermVocabAttributes>> keywords;

  @JsonProperty("samplingProcedureFreeTexts")
  private Map<String, List<String>> samplingProcedureFreeTexts;

  @JsonProperty("classifications")
  private Map<String, List<TermVocabAttributes>> classifications;

  @JsonProperty("abstract")
  private Map<String, String> abstractField;

  @JsonProperty("titleStudy")
  private Map<String, String> titleStudy;

  @JsonProperty("studyUrl")
  private Map<String, String> studyUrl;

  @JsonProperty("studyNumber")
  private String studyNumber;

  @JsonProperty("typeOfTimeMethods")
  private Map<String, List<TermVocabAttributes>> typeOfTimeMethods;

  @JsonProperty("fileLanguages")
  private Set<String> fileLanguages;

  @JsonProperty("typeOfSamplingProcedures")
  private Map<String, List<VocabAttributes>> typeOfSamplingProcedures;

  @JsonProperty("publisher")
  private Map<String, Publisher> publisher;

  @JsonProperty("studyAreaCountries")
  private Map<String, List<Country>> studyAreaCountries;

  @JsonProperty("unitTypes")
  private Map<String, List<TermVocabAttributes>> unitTypes;

  @JsonProperty("pidStudies")
  private Map<String, List<Pid>> pidStudies;

  @JsonProperty("lastModified")
  private String lastModified;

  @JsonProperty("isActive")
  private boolean active;

  @JsonProperty("studyXmlSourceUrl")
  private String studyXmlSourceUrl;
}
