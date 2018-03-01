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
 * @author moses@doraventures.com
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
    "dataCollectionFreeTexts",
    "dataAccessFreeTexts",
    "lastModified",
    "isActive"
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

  @JsonProperty("dataCollectionFreeTexts")
  private Map<String, List<DataCollectionFreeText>>  dataCollectionFreeTexts;

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
}
