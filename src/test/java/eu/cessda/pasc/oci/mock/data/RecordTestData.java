/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test data used by multiple tests.
 *
 * @author moses AT doraventures DOT com
 */
public final class RecordTestData {

    //language=JSON
    public static final String LIST_RECORDER_HEADERS_BODY_EXAMPLE = """
[
  {
    "lastModified": "2018-02-21T07:48:38Z",
    "identifier": "997"
  },
  {
    "lastModified": "2018-02-19",
    "identifier": "998"
  }
]""";

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        var cmmStudies = new ArrayList<CMMStudy>(2);
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
        var cmmStudyOfLanguage = objectMapper.readValue(syntheticCMMStudyOfLanguageEn, CMMStudyOfLanguage.class);
        return Collections.singletonList(cmmStudyOfLanguage);
    }

    public static List<CMMStudyOfLanguage> getCmmStudyOfLanguageCodeEnX3() throws IOException {
        var studyOfLanguages = new ArrayList<CMMStudyOfLanguage>(3);
        String syntheticCMMStudyOfLanguageEn = getSyntheticCMMStudyOfLanguageEn();
        studyOfLanguages.add(objectMapper.readValue(syntheticCMMStudyOfLanguageEn, CMMStudyOfLanguage.class));

        CMMStudyOfLanguage cmmStudyOfLanguage2 = objectMapper.readValue(syntheticCMMStudyOfLanguageEn, CMMStudyOfLanguage.class)
            .withId("UK-Data-Service__999")
            .withLastModified("2017-11-15T08:08:11Z");
        studyOfLanguages.add(cmmStudyOfLanguage2);

        CMMStudyOfLanguage cmmStudyOfLanguage3 = objectMapper.readValue(syntheticCMMStudyOfLanguageEn, CMMStudyOfLanguage.class)
            .withId("UK-Data-Service__1000")
            .withLastModified("2017-04-05");
        studyOfLanguages.add(cmmStudyOfLanguage3);

        return studyOfLanguages;
    }

    public static CMMStudy getSyntheticCmmStudy() throws IOException {
        var cmmStudyStream = ResourceHandler.getResourceAsStream("synthetic_compliant_record.json");
        return objectMapper.readValue(cmmStudyStream, CMMStudy.class);
    }

    private static CMMStudy getDeletedCmmStudy() throws IOException {
        var cmmStudyStream = ResourceHandler.getResourceAsStream("record_ukds_1031_deleted.json");
        return objectMapper.readValue(cmmStudyStream, CMMStudy.class);
    }

    public static CMMStudy getSyntheticCmmStudy(String identifier) throws IOException {
        var cmmStudy = getSyntheticCmmStudy();
        return cmmStudy.withStudyNumber(identifier);
    }

    public static String getSyntheticCMMStudyOfLanguageEn() throws IOException {
        return ResourceHandler.getResourceAsString("synthetic_compliant_record_en.json");
    }
}
