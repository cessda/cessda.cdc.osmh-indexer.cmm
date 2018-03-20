package eu.cessda.pasc.oci.data;

import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.fail;

/**
 * @author moses@doraventures.com
 */
public class RecordTestData {

  private RecordTestData() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  public static final String LIST_RECORDER_HEADERS_BODY_EXAMPLE = "" +
      "[\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-21T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"997\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-19\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"998\"\n" +
      "  }\n" +
      "]";

  public static final String LIST_RECORDER_HEADERS_BODY_EXAMPLE_WITH_INCREMENT = "" +
      "[\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-21T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"997\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-03-22T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"999\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-23\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"1000\"\n" +
      "  }\n" +
      "]";

  public static final String LIST_RECORDER_HEADERS_X6 = "" +
      "[\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-22T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"997\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-01T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"999\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-22T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"998\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-01-05T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"1000\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-01-15T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"1001\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2016-02-22T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"1002\"\n" +
      "  }\n" +
      "]";

  public static final String LIST_RECORDER_HEADERS_WITH_INVALID_DATETIME = "" +
      "[\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-22\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"997\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-01T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"999\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-00-00\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"998\"\n" +
      "  }\n" +
      "]";


  public static List<Optional<CMMStudy>> getASingleSyntheticCMMStudyAsList() {
    List<Optional<CMMStudy>> cmmStudies = new ArrayList<>();
    try {
      cmmStudies.add(getSyntheticCmmStudy());
    } catch (IOException e) {
      fail("Unable to parse Study string to CMMStudy Object");
    }
    return cmmStudies;
  }


  public static List<Optional<CMMStudy>> getSyntheticCMMStudyAndADeletedRecordAsList() {
    List<Optional<CMMStudy>> cmmStudies = new ArrayList<>();
    try {
      cmmStudies.add(getSyntheticCmmStudy());
      cmmStudies.add(getDeletedCmmStudy());
    } catch (IOException e) {
      fail("Unable to parse Study string to CMMStudy Object");
    }
    return cmmStudies;
  }

  public static List<CMMStudyOfLanguage> getCmmStudyOfLanguageCodeEnX1() throws IOException {
    List<CMMStudyOfLanguage> studyOfLanguages = new ArrayList<>();
    String syntheticCMMStudyOfLanguageEn = getSyntheticCMMStudyOfLanguageEn();
    CMMStudyOfLanguage cmmStudyOfLanguage = CMMStudyOfLanguageConverter.fromJsonString(syntheticCMMStudyOfLanguageEn);
    studyOfLanguages.add(cmmStudyOfLanguage);
    return studyOfLanguages;
  }

  public static List<CMMStudyOfLanguage> getCmmStudyOfLanguageCodeEnX3() throws IOException {
    List<CMMStudyOfLanguage> studyOfLanguages = new ArrayList<>();
    String syntheticCMMStudyOfLanguageEn = getSyntheticCMMStudyOfLanguageEn();
    studyOfLanguages.add(CMMStudyOfLanguageConverter.fromJsonString(syntheticCMMStudyOfLanguageEn));

    CMMStudyOfLanguage cmmStudyOfLanguage2 = CMMStudyOfLanguageConverter.fromJsonString(syntheticCMMStudyOfLanguageEn);
    cmmStudyOfLanguage2.setId("UK-Data-Service__999");
    cmmStudyOfLanguage2.setLastModified("2017-11-15T08:08:11Z");
    studyOfLanguages.add(cmmStudyOfLanguage2);

    CMMStudyOfLanguage cmmStudyOfLanguage3 = CMMStudyOfLanguageConverter.fromJsonString(syntheticCMMStudyOfLanguageEn);
    cmmStudyOfLanguage3.setId("UK-Data-Service__1000");
    cmmStudyOfLanguage3.setLastModified("2017-04-05");
    studyOfLanguages.add(cmmStudyOfLanguage3);

    return studyOfLanguages;
  }

  public static Optional<CMMStudy> getSyntheticCmmStudy() throws IOException {
    String cmmStudyString = new FileHandler().getFileWithUtil("synthetic_compliant_record.json");
    return Optional.ofNullable(CMMStudyConverter.fromJsonString(cmmStudyString));
  }

  private static Optional<CMMStudy> getDeletedCmmStudy() throws IOException {
    String cmmStudyString = new FileHandler().getFileWithUtil("record_ukds_1031_deleted.json");
    return Optional.ofNullable(CMMStudyConverter.fromJsonString(cmmStudyString));
  }

  public static Optional<CMMStudy> getSyntheticCmmStudy(String identifier) throws IOException {
    Optional<CMMStudy> optionalCmmStudy = getSyntheticCmmStudy();
    optionalCmmStudy.ifPresent(cmmStudy -> cmmStudy.setStudyNumber(identifier));
    return optionalCmmStudy;
  }

  public static String getSyntheticCMMStudyOfLanguageEn() {
    FileHandler fileHandler = new FileHandler();
    return fileHandler.getFileWithUtil("synthetic_complaint_record_en.json");
  }
}
