/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.exception.InternalSystemException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import lombok.experimental.UtilityClass;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Optional;

@UtilityClass
public class ListIdentifiersResponseValidator {

    /**
     * Checks if the response has an {@literal <error>} element.
     *
     * @param document the document to map to.
     * @throws OaiPmhException         if an {@literal <error>} element was present.
     * @throws InternalSystemException if the given document has no OAI element.
     */
    public static void validateResponse(Document document) throws InternalSystemException, OaiPmhException {
        Optional<NodeList> oAINode = Optional.ofNullable(document.getElementsByTagName(OaiPmhConstants.OAI_PMH));

        if (oAINode.isEmpty()) {
            throw new InternalSystemException("Missing OAI element");
        }

        NodeList nodeList = oAINode.get();
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList childNodes = nodeList.item(i).getChildNodes();
            for (int childNodeIndex = 0; childNodeIndex < childNodes.getLength(); childNodeIndex++) {
                Node item = childNodes.item(childNodeIndex);
                if (OaiPmhConstants.ERROR.equals(item.getLocalName())) {
                    if (item.getTextContent() != null && !item.getTextContent().isEmpty()) {
                        throw new OaiPmhException(OaiPmhException.Code.valueOf(item.getAttributes().getNamedItem("code").getTextContent()), item.getTextContent());
                    } else {
                        throw new OaiPmhException(OaiPmhException.Code.valueOf(item.getAttributes().getNamedItem("code").getTextContent()));
                    }
                }
            }
        }
    }
}