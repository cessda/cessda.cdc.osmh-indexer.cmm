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

import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.xpath.XPathFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecordResponseValidator {

  /**
   * Checks if the record has an <error> element.
   *
   * @param document the document to map to.
   * @param xFactory the Path Factory.
   * @return ErrorStatus of the record.
   */
  public static ErrorStatus validateResponse(Document document, XPathFactory xFactory) {

    ErrorStatus.ErrorStatusBuilder statusBuilder = ErrorStatus.builder();
    DocElementParser.getFirstElement(document, xFactory, OaiPmhConstants.ERROR_PATH)
        .ifPresent((Element element) ->
            statusBuilder.hasError(true).message(element.getAttributeValue(OaiPmhConstants.CODE_ATTR) + ": " + element.getText())
        );

    return statusBuilder.build();
  }
}