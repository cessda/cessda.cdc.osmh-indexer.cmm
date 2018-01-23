package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;

/**
 * @author moses@doraventures.com
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.util.Map;

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
    "personName",
    "associatedInstitutions",
    "fileLanguages",
    "principalInvestigatorPeople",
    "classIdentifier",
    "typeOfSamplingProcedures",
    "samplingProcedure",
    "typeOfModeOfCollections",
    "dataCollectionPeriodStartdate",
    "dataCollectionPeriodEnddate",
    "dataAccess",
    "accessClass",
    "principalInvestigatorInstitutes",
    "institutionFullName",
    "lastModified",
    "isActive",
    "recordType"
})
@Builder
public class CMMStudy {
  private String personName;
  private String classIdentifier;
  private String[] associatedInstitutions;
  private Map<String, String> institutionFullName;
  private String[] principalInvestigatorPeople;
  private String dataCollectionPeriodStartdate;
  private String dataCollectionPeriodEnddate;
  private int publicationYear;
  private String[] typeOfModeOfCollections;
  private String[] keywords;
  private RecordType recordType;
  private Map<String, String> samplingProcedure;
  private String[] classifications;
  private Map<String, String> dataAccess;
  private Map<String, String> abstractField;
  private Map<String, String> titleStudy;
  private String studyNumber;
  private String[] typeOfTimeMethods;
  private String[] fileLanguages;
  private String accessClass;
  private String[] principalInvestigatorInstitutes;
  private String[] typeOfSamplingProcedures;
  private String publisher;
  private String[] studyAreaCountries;
  private String[] unitTypes;
  private String[] pidStudies;
  private String lastModified;
  private boolean isActive;

  @JsonProperty("dataCollectionPeriodEnddate")
  public String getDataCollectionPeriodEnddate() {
    return dataCollectionPeriodEnddate;
  }

  @JsonProperty("dataCollectionPeriodEnddate")
  public void setDataCollectionPeriodEnddate(String value) {
    this.dataCollectionPeriodEnddate = value;
  }

  @JsonProperty("personName")
  public String getPersonName() {
    return personName;
  }

  @JsonProperty("personName")
  public void setPersonName(String value) {
    this.personName = value;
  }

  @JsonProperty("classIdentifier")
  public String getClassIdentifier() {
    return classIdentifier;
  }

  @JsonProperty("classIdentifier")
  public void setClassIdentifier(String value) {
    this.classIdentifier = value;
  }

  @JsonProperty("associatedInstitutions")
  public String[] getAssociatedInstitutions() {
    return associatedInstitutions;
  }

  @JsonProperty("associatedInstitutions")
  public void setAssociatedInstitutions(String[] value) {
    this.associatedInstitutions = value;
  }

  @JsonProperty("institutionFullName")
  public Map<String, String> getInstitutionFullName() {
    return institutionFullName;
  }

  @JsonProperty("institutionFullName")
  public void setInstitutionFullName(Map<String, String> value) {
    this.institutionFullName = value;
  }

  @JsonProperty("principalInvestigatorPeople")
  public String[] getPrincipalInvestigatorPeople() {
    return principalInvestigatorPeople;
  }

  @JsonProperty("principalInvestigatorPeople")
  public void setPrincipalInvestigatorPeople(String[] value) {
    this.principalInvestigatorPeople = value;
  }

  @JsonProperty("dataCollectionPeriodStartdate")
  public String getDataCollectionPeriodStartdate() {
    return dataCollectionPeriodStartdate;
  }

  @JsonProperty("dataCollectionPeriodStartdate")
  public void setDataCollectionPeriodStartdate(String value) {
    this.dataCollectionPeriodStartdate = value;
  }

  @JsonProperty("publicationYear")
  public int getPublicationYear() {
    return publicationYear;
  }

  @JsonProperty("publicationYear")
  public void setPublicationYear(int value) {
    this.publicationYear = value;
  }

  @JsonProperty("typeOfModeOfCollections")
  public String[] getTypeOfModeOfCollections() {
    return typeOfModeOfCollections;
  }

  @JsonProperty("typeOfModeOfCollections")
  public void setTypeOfModeOfCollections(String[] value) {
    this.typeOfModeOfCollections = value;
  }

  @JsonProperty("keywords")
  public String[] getKeywords() {
    return keywords;
  }

  @JsonProperty("keywords")
  public void setKeywords(String[] value) {
    this.keywords = value;
  }

  @JsonProperty("recordType")
  public RecordType getRecordType() {
    return recordType;
  }

  @JsonProperty("recordType")
  public void setRecordType(RecordType value) {
    this.recordType = value;
  }

  @JsonProperty("samplingProcedure")
  public Map<String, String> getSamplingProcedure() {
    return samplingProcedure;
  }

  @JsonProperty("samplingProcedure")
  public void setSamplingProcedure(Map<String, String> value) {
    this.samplingProcedure = value;
  }

  @JsonProperty("classifications")
  public String[] getClassifications() {
    return classifications;
  }

  @JsonProperty("classifications")
  public void setClassifications(String[] value) {
    this.classifications = value;
  }

  @JsonProperty("dataAccess")
  public Map<String, String> getDataAccess() {
    return dataAccess;
  }

  @JsonProperty("dataAccess")
  public void setDataAccess(Map<String, String> value) {
    this.dataAccess = value;
  }

  @JsonProperty("abstract")
  public Map<String, String> getAbstractField() {
    return abstractField;
  }

  @JsonProperty("abstract")
  public void setAbstractField(Map<String, String> value) {
    this.abstractField = value;
  }

  @JsonProperty("titleStudy")
  public Map<String, String> getTitleStudy() {
    return titleStudy;
  }

  @JsonProperty("titleStudy")
  public void setTitleStudy(Map<String, String> value) {
    this.titleStudy = value;
  }

  @JsonProperty("studyNumber")
  public String getStudyNumber() {
    return studyNumber;
  }

  @JsonProperty("studyNumber")
  public void setStudyNumber(String value) {
    this.studyNumber = value;
  }

  @JsonProperty("typeOfTimeMethods")
  public String[] getTypeOfTimeMethods() {
    return typeOfTimeMethods;
  }

  @JsonProperty("typeOfTimeMethods")
  public void setTypeOfTimeMethods(String[] value) {
    this.typeOfTimeMethods = value;
  }

  @JsonProperty("fileLanguages")
  public String[] getFileLanguages() {
    return fileLanguages;
  }

  @JsonProperty("fileLanguages")
  public void setFileLanguages(String[] value) {
    this.fileLanguages = value;
  }

  @JsonProperty("accessClass")
  public String getAccessClass() {
    return accessClass;
  }

  @JsonProperty("accessClass")
  public void setAccessClass(String value) {
    this.accessClass = value;
  }

  @JsonProperty("principalInvestigatorInstitutes")
  public String[] getPrincipalInvestigatorInstitutes() {
    return principalInvestigatorInstitutes;
  }

  @JsonProperty("principalInvestigatorInstitutes")
  public void setPrincipalInvestigatorInstitutes(String[] value) {
    this.principalInvestigatorInstitutes = value;
  }

  @JsonProperty("typeOfSamplingProcedures")
  public String[] getTypeOfSamplingProcedures() {
    return typeOfSamplingProcedures;
  }

  @JsonProperty("typeOfSamplingProcedures")
  public void setTypeOfSamplingProcedures(String[] value) {
    this.typeOfSamplingProcedures = value;
  }

  @JsonProperty("publisher")
  public String getPublisher() {
    return publisher;
  }

  @JsonProperty("publisher")
  public void setPublisher(String value) {
    this.publisher = value;
  }

  @JsonProperty("studyAreaCountries")
  public String[] getStudyAreaCountries() {
    return studyAreaCountries;
  }

  @JsonProperty("studyAreaCountries")
  public void setStudyAreaCountries(String[] value) {
    this.studyAreaCountries = value;
  }

  @JsonProperty("unitTypes")
  public String[] getUnitTypes() {
    return unitTypes;
  }

  @JsonProperty("unitTypes")
  public void setUnitTypes(String[] value) {
    this.unitTypes = value;
  }

  @JsonProperty("pidStudies")
  public String[] getPidStudies() {
    return pidStudies;
  }

  @JsonProperty("pidStudies")
  public void setPidStudies(String[] value) {
    this.pidStudies = value;
  }

  @JsonProperty("lastModified")
  public String getLastModified() {
    return lastModified;
  }

  @JsonProperty("lastModified")
  public void setLastModified(String value) {
    this.lastModified = value;
  }


  @JsonProperty("isActive")
  public boolean isActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setActive(boolean active) {
    isActive = active;
  }
}
