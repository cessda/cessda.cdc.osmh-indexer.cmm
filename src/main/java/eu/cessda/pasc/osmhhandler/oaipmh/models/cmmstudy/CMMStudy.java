package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

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
    "associatedInstitutions",
    "fileLanguages",
    "creators",
    "classIdentifier",
    "typeOfSamplingProcedures",
    "samplingProcedure",
    "typeOfModeOfCollections",
    "dataCollectionPeriodStartdate",
    "dataCollectionPeriodEnddate",
    "dataAccess",
    "accessClass",
    "institutionFullName",
    "lastModified",
    "isActive",
    "recordType"
})
@Builder
@Getter
public class CMMStudy {

  @JsonProperty("classIdentifier")
  private String classIdentifier;

  @JsonProperty("associatedInstitutions")
  private String[] associatedInstitutions;

  @JsonProperty("institutionFullName")
  private Map<String, String> institutionFullName;

  @JsonProperty("creators")
  private String[] creators;

  @JsonProperty("dataCollectionPeriodStartdate")
  private String dataCollectionPeriodStartdate;

  @JsonProperty("dataCollectionPeriodEnddate")
  private String dataCollectionPeriodEnddate;

  @JsonProperty("publicationYear")
  private int publicationYear;

  @JsonProperty("typeOfModeOfCollections")
  private String[] typeOfModeOfCollections;

  @JsonProperty("keywords")
  private String[] keywords;

  @JsonProperty("recordType")
  private RecordType recordType;

  @JsonProperty("samplingProcedure")
  private Map<String, String> samplingProcedure;

  @JsonProperty("classifications")
  private String[] classifications;

  @JsonProperty("dataAccess")
  private Map<String, String> dataAccess;

  @JsonProperty("abstract")
  private Map<String, String> abstractField;

  @JsonProperty("titleStudy")
  private Map<String, String> titleStudy;

  @JsonProperty("studyNumber")
  private String studyNumber;

  @JsonProperty("typeOfTimeMethods")
  private String[] typeOfTimeMethods;

  @JsonProperty("fileLanguages")
  private String[] fileLanguages;

  @JsonProperty("accessClass")
  private String accessClass;


  @JsonProperty("typeOfSamplingProcedures")
  private String[] typeOfSamplingProcedures;

  @JsonProperty("publisher")
  private String publisher;

  @JsonProperty("studyAreaCountries")
  private String[] studyAreaCountries;

  @JsonProperty("unitTypes")
  private String[] unitTypes;

  @JsonProperty("pidStudies")
  private String[] pidStudies;

  @JsonProperty("lastModified")
  private String lastModified;

  @JsonProperty("isActive")
  private boolean active;
}
