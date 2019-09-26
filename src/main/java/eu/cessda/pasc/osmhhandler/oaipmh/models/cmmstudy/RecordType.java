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
package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.IOException;

/**
 * @author moses AT doraventures DOT com
 */
public enum  RecordType {
  CMM_STUDY;

  @JsonValue
  public String toValue() {
      if (this == RecordType.CMM_STUDY) {
          return "CMMStudy";
      }
    return null;
  }

  @JsonCreator
  public static RecordType forValue(String value) throws IOException {
    if (value.equals("CMMStudy")) return CMM_STUDY;
    throw new IOException("Cannot deserialize RecordType");
  }
}
