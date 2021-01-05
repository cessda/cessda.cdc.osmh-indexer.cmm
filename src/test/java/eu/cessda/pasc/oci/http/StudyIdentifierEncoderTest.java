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
package eu.cessda.pasc.oci.http;

import org.junit.Test;

import static eu.cessda.pasc.oci.harvester.StudyIdentifierEncoder.encodeStudyIdentifier;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
public class StudyIdentifierEncoderTest {

  @Test
  public void shouldReplaceAllSpecialCharacters() {

    // Given
    String expectedEncodedId = "oai_cl_dbk_dt_gesis_dt_org_cl_DBK_sl_ZA0001";
    String identifier = "oai:dbk.gesis.org:DBK/ZA0001";

    // When
    String encodedId = encodeStudyIdentifier(identifier);

    then(encodedId).isEqualTo(expectedEncodedId);
  }

  @Test
  public void shouldDoEncoding() {

    // Given
    String identifier = "http://nesstar.ucd.ie:80/obj/fStudy/PVTYV1-Anon";
    String expectedEncoded = "http_cl__sl__sl_nesstar_dt_ucd_dt_ie_cl_80_sl_obj_sl_fStudy_sl_PVTYV1-Anon";

    // When
    String actual = encodeStudyIdentifier(identifier);

    then(actual).isEqualTo(expectedEncoded);
  }
}
