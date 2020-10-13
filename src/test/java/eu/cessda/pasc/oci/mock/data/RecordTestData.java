/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.mock.data;

import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguageConverter;
import eu.cessda.pasc.oci.parser.FileHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Test data used by multiple tests.
 *
 * @author moses AT doraventures DOT com
 */
public final class RecordTestData {

    public static final String LIST_RECORDER_HEADERS_BODY_EXAMPLE =
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

    public static final String LIST_RECORDER_HEADERS_BODY_EXAMPLE_WITH_INCREMENT =
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

    public static final String LIST_RECORDER_HEADERS_X6 =
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

    public static final String LIST_RECORDER_HEADERS_WITH_INVALID_DATETIME =
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


    public static List<CMMStudy> getASingleSyntheticCMMStudyAsList() {
        List<CMMStudy> cmmStudies = new ArrayList<>(1);
        try {
            cmmStudies.add(getSyntheticCmmStudy());
        } catch (IOException e) {
            throw new AssertionError("Unable to parse Study string to CMMStudy Object", e);
        }
        return cmmStudies;
    }


    public static List<CMMStudy> getSyntheticCMMStudyAndADeletedRecordAsList() {
        List<CMMStudy> cmmStudies = new ArrayList<>(2);
        try {
            cmmStudies.add(getSyntheticCmmStudy());
            cmmStudies.add(getDeletedCmmStudy());
        } catch (IOException e) {
            throw new AssertionError("Unable to parse Study string to CMMStudy Object", e);
        }
        return cmmStudies;
    }

    public static List<CMMStudyOfLanguage> getCmmStudyOfLanguageCodeEnX1() throws IOException {
        String syntheticCMMStudyOfLanguageEn = getSyntheticCMMStudyOfLanguageEn();
        var cmmStudyOfLanguage = new CMMStudyOfLanguageConverter().fromJsonString(syntheticCMMStudyOfLanguageEn);
        return Collections.singletonList(cmmStudyOfLanguage);
    }

    public static List<CMMStudyOfLanguage> getCmmStudyOfLanguageCodeEnX3() throws IOException {
        var studyOfLanguages = new ArrayList<CMMStudyOfLanguage>(3);
        String syntheticCMMStudyOfLanguageEn = getSyntheticCMMStudyOfLanguageEn();
        var cmmStudyOfLanguageConverter = new CMMStudyOfLanguageConverter();
        studyOfLanguages.add(cmmStudyOfLanguageConverter.fromJsonString(syntheticCMMStudyOfLanguageEn));

        CMMStudyOfLanguage cmmStudyOfLanguage2 = cmmStudyOfLanguageConverter.fromJsonString(syntheticCMMStudyOfLanguageEn)
            .withId("UK-Data-Service__999")
            .withLastModified("2017-11-15T08:08:11Z");
        studyOfLanguages.add(cmmStudyOfLanguage2);

        CMMStudyOfLanguage cmmStudyOfLanguage3 = cmmStudyOfLanguageConverter.fromJsonString(syntheticCMMStudyOfLanguageEn)
            .withId("UK-Data-Service__1000")
            .withLastModified("2017-04-05");
        studyOfLanguages.add(cmmStudyOfLanguage3);

        return studyOfLanguages;
    }

    public static CMMStudy getSyntheticCmmStudy() throws IOException {
        InputStream cmmStudyString = new FileHandler().getFileAsStream("synthetic_compliant_record.json");
        return new CMMStudyConverter().fromJsonStream(cmmStudyString);
    }

    private static CMMStudy getDeletedCmmStudy() throws IOException {
        InputStream cmmStudyString = new FileHandler().getFileAsStream("record_ukds_1031_deleted.json");
        return new CMMStudyConverter().fromJsonStream(cmmStudyString);
    }

    public static Optional<CMMStudy> getSyntheticCmmStudy(String identifier) throws IOException {
        var cmmStudy = getSyntheticCmmStudy();
        cmmStudy.withStudyNumber(identifier);
        return Optional.of(cmmStudy);
    }

    public static String getSyntheticCMMStudyOfLanguageEn() throws IOException {
        FileHandler fileHandler = new FileHandler();
        return fileHandler.getFileAsString("synthetic_complaint_record_en.json");
    }
}
