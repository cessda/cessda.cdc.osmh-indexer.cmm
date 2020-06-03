/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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

package eu.cessda.pasc.oci.helpers;

import eu.cessda.pasc.oci.models.errors.ErrorStatus;
import lombok.experimental.UtilityClass;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@UtilityClass
public class ListIdentifiersResponseValidator {

    /**
     * Checks if the response has an {@literal <error>} element.
     *
     * @param document the document to map to.
     * @return ErrorStatus of the record, or an empty optional if no error was present.
     */
    public static Optional<ErrorStatus> validateResponse(Document document) {
        Optional<NodeList> oAINode = ofNullable(document.getElementsByTagName(OaiPmhConstants.OAI_PMH));

        if (oAINode.isEmpty()) {
            return Optional.of(new ErrorStatus("missingElement", "Missing OAI element"));
        }

        NodeList nodeList = oAINode.get();
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList childNodes = nodeList.item(i).getChildNodes();
            for (int childNodeIndex = 0; childNodeIndex < childNodes.getLength(); childNodeIndex++) {
                Node item = childNodes.item(childNodeIndex);
                if (OaiPmhConstants.ERROR.equals(item.getLocalName())) {
                    return Optional.of(new ErrorStatus(item.getAttributes().getNamedItem("code").getTextContent(), item.getTextContent()));
                }
            }
        }

        return Optional.empty();
    }
}