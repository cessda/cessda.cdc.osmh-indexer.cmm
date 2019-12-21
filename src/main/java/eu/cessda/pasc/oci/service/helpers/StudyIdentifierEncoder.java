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
package eu.cessda.pasc.oci.service.helpers;

import java.util.function.Function;

/**
 * Utility to encode(String replace) known Special Characters in HTTP rest context.
 *
 * This is reversed by the respective handler using the same string replace token here
 * before calling the remote Service providers repo
 *
 * @author moses AT doravenetures DOT com
 */
public class StudyIdentifierEncoder {

  public static Function<String, String> encodeStudyIdentifier(){
    return studyIdentifier -> studyIdentifier.replace(".", "_dt_")
        .replace("/", "_sl_")
        .replace(":", "_cl_");
  }
}
