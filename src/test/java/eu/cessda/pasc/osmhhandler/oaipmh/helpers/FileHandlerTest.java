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
package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.FileHandler;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author moses AT doraventures DOT com
 */
public class FileHandlerTest {

  @Test
  public void shouldReturnEmptyStringForBadPath() {

    // Given
    FileHandler fileHandler = new FileHandler();

    // When
    String fileContent = fileHandler.getFileWithUtil("xml/ddi_record_1683_does_not_exist.xml");

    assertThat(fileContent).isEqualTo("");
  }

  @Test
  public void shouldReturnContentOfValidFilePath() {

    // Given
    FileHandler fileHandler = new FileHandler();

    // When
    String fileContent = fileHandler.getFileWithUtil("xml/ddi_record_1683.xml");

    assertThat(fileContent).contains("<request verb=\"GetRecord\" identifier=\"1683\"");
  }
}