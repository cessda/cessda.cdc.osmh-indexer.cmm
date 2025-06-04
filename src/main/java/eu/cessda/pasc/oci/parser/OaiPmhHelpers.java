/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.input.SAXBuilder;

import javax.xml.XMLConstants;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Helper methods to deal with Oai-pmh protocol
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@UtilityClass
public class OaiPmhHelpers {

    @SuppressWarnings("java:S5164") // This is only used by threads that will exit.
    private static final ThreadLocal<SAXBuilder> SAX_BUILDER_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        var saxBuilder = new SAXBuilder();
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return saxBuilder;
    });

    public static URI buildGetStudyFullUrl(@NonNull URI repoUrl, @NonNull String studyIdentifier, @NonNull String metadataPrefix) throws URISyntaxException {
        return new URI(repoUrl +
            // verb=GetRecord
            "?verb=GetRecord" +
            //&identifier=1683
            "&" + OaiPmhConstants.IDENTIFIER + "=" + URLEncoder.encode(studyIdentifier, StandardCharsets.UTF_8) +
            //&metadataPrefix=ddi
            "&metadataPrefix=" + URLEncoder.encode(metadataPrefix, StandardCharsets.UTF_8)
        );
    }

    /**
     * Retrieve an instance of a {@link SAXBuilder}.
     */
    static SAXBuilder getSaxBuilder() {
        return SAX_BUILDER_THREAD_LOCAL.get();
    }
}
