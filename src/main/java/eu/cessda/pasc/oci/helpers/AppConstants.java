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
package eu.cessda.pasc.oci.helpers;

import java.util.TimeZone;

/**
 * Harvester (OSMH) Handler concept Constants
 *
 * @author moses AT doraventures DOT com
 */
public class AppConstants {

  static final String[] EXPECTED_DATE_FORMATS = new String[]{"yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ssZ"};
  static final String UTC_ID = TimeZone.getTimeZone("UTC").getID();
  public static final String LAST_MODIFIED_FIELD = "lastModified";

  private AppConstants() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }
}
