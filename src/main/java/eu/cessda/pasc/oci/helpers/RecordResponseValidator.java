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
package eu.cessda.pasc.oci.helpers;

import eu.cessda.pasc.oci.exception.OaiPmhException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RecordResponseValidator {

    private final DocElementParser docElementParser;

    @Autowired
    public RecordResponseValidator(DocElementParser docElementParser) {
        this.docElementParser = docElementParser;
    }

    /**
     * Checks if the record has an {@literal <error>} element.
     *
     * @param document the document to map to.
     * @throws OaiPmhException if an {@literal <error>} element was present.
     */
    public void validateResponse(Document document) throws OaiPmhException {

        final Optional<Element> optionalElement = docElementParser.getFirstElement(document, OaiPmhConstants.ERROR_PATH);

        if (optionalElement.isPresent()) {
            final Element element = optionalElement.get();
            if (element.getText() != null && !element.getText().trim().isEmpty()) {
                throw new OaiPmhException(OaiPmhException.Code.valueOf(element.getAttributeValue(OaiPmhConstants.CODE_ATTR)), element.getText());
            } else {
                throw new OaiPmhException(OaiPmhException.Code.valueOf(element.getAttributeValue(OaiPmhConstants.CODE_ATTR)));
            }
        }
    }
}