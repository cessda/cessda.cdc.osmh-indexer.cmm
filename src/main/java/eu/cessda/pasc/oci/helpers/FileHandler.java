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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

/**
 * File handling helper methods
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class FileHandler {

  public String getFileWithUtil(String fileName) {

    String result = "";
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      result = IOUtils.toString(Objects.requireNonNull(classLoader.getResourceAsStream(fileName)));
    } catch (IOException | NullPointerException e) {
      log.error("Could not read file [{}]. Exception Message [{}]", fileName, e.getMessage());
    }
    return result;
  }
}
