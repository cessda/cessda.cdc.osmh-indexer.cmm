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
      "    \"lastModified\": \"2018-02-22T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"997\"\n" +
      "  },\n" +
      "  {\n" +
      "    \"lastModified\": \"2018-02-22T07:48:38Z\",\n" +
      "    \"type\": \"Study\",\n" +
      "    \"recordType\": \"RecordHeader\",\n" +
      "    \"identifier\": \"998\"\n" +
      "  }\n" +
      "]";


  public static List<Optional<CMMStudy>> getASingleSyntheticCMMStudyAsList() {
    List<Optional<CMMStudy>> cmmStudies = new ArrayList<>();
    try {
      cmmStudies.add(getCmmStudy());
    } catch (IOException e) {
      fail("Unable to parse Study string to CMMStudy Object");
    }
    return cmmStudies;
  }


  public static List<Optional<CMMStudy>> getSyntheticCMMStudyAndADeletedRecordAsList() {
    List<Optional<CMMStudy>> cmmStudies = new ArrayList<>();
    try {
      cmmStudies.add(getCmmStudy());
      cmmStudies.add(getDeletedCmmStudy());
    } catch (IOException e) {
      fail("Unable to parse Study string to CMMStudy Object");
    }
    return cmmStudies;
  }

  public static List<CMMStudyOfLanguage> getCmmStudyOfLanguageCodeEn() throws IOException {
    List<CMMStudyOfLanguage> studyOfLanguages = new ArrayList<>();
    String syntheticCMMStudyOfLanguageEn = getSyntheticCMMStudyOfLanguageEn();
    CMMStudyOfLanguage cmmStudyOfLanguage = CMMStudyOfLanguageConverter.fromJsonString(syntheticCMMStudyOfLanguageEn);
    studyOfLanguages.add(cmmStudyOfLanguage);
    return studyOfLanguages;
  }

  public static Optional<CMMStudy> getCmmStudy() throws IOException {
    String cmmStudyString = new FileHandler().getFileWithUtil("synthetic_compliant_record.json");
    return Optional.ofNullable(CMMStudyConverter.fromJsonString(cmmStudyString));
  }

  private static Optional<CMMStudy> getDeletedCmmStudy() throws IOException {
    String cmmStudyString = new FileHandler().getFileWithUtil("record_ukds_1031_deleted.json");
    return Optional.ofNullable(CMMStudyConverter.fromJsonString(cmmStudyString));
  }

  public static Optional<CMMStudy> getCmmStudy(String identifier) throws IOException {
    Optional<CMMStudy> optionalCmmStudy = getCmmStudy();
    optionalCmmStudy.ifPresent(cmmStudy -> cmmStudy.setStudyNumber(identifier));
    return optionalCmmStudy;
  }

  public static String getSyntheticCMMStudyOfLanguageEn() {
    FileHandler fileHandler = new FileHandler();
    return fileHandler.getFileWithUtil("synthetic_complaint_record_en.json");
  }
}
