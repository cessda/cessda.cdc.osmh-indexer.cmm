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

    private static final String MISSING_ELEMENT_MISSING_OAI_ELEMENT = "MissingElement: Missing Oai element";

    /**
     * Checks if the response has an <error> element.
     *
     * @param document the document to map to.
     * @return ErrorStatus of the record.
     */
    public static ErrorStatus validateResponse(Document document) {
        ErrorStatus.ErrorStatusBuilder statusBuilder = ErrorStatus.builder();
        Optional<NodeList> oAINode = ofNullable(document.getElementsByTagName(OaiPmhConstants.OAI_PMH));

        if (oAINode.isEmpty()) {
            return statusBuilder.hasError(true).message(MISSING_ELEMENT_MISSING_OAI_ELEMENT).build();
        }

        NodeList nodeList = oAINode.get();
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList childNodes = nodeList.item(i).getChildNodes();
            for (int childNodeIndex = 0; childNodeIndex < childNodes.getLength(); childNodeIndex++) {
                Node item = childNodes.item(childNodeIndex);
                if (OaiPmhConstants.ERROR.equals(item.getLocalName())) {
                    return statusBuilder.hasError(true)
                            .message(item.getAttributes().getNamedItem("code").getTextContent() + ": " + item.getTextContent())
                            .build();
                }
            }
        }

        return statusBuilder.build();
    }
}