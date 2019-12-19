/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Model representing a CMMStudy. Used to deserialize documents from json.
 *
 * @author moses AT doravenetures DOT com
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
    "langAvailableIn"
})
@Getter
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

  @Setter
  @JsonProperty("studyUrl")
  private Map<String, String> studyUrl;

  @Setter
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

  @Setter
  @JsonProperty("isActive")
  private boolean active;

  /**
   * This is added information by this application after checking the the CMM record meets
   * the minimum CMM Fields requirements for given Lang Iso Code.
   * @see eu.cessda.pasc.oci.service.helpers.LanguageAvailabilityMapper#setAvailableLanguages(CMMStudy)
   */
  @Setter
  @Getter
  @JsonProperty("langAvailableIn")
  private Set<String> langAvailableIn = new HashSet<>();
}
