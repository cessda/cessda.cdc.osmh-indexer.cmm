/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.input.SAXBuilder;

import javax.xml.XMLConstants;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static eu.cessda.pasc.oci.parser.OaiPmhConstants.*;

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

    public static URI buildGetStudyFullUrl(@NonNull Repo repo, @NonNull String studyIdentifier) throws URISyntaxException {
        return buildGetStudyFullUrl(repo.getUrl(), studyIdentifier, repo.getPreferredMetadataParam());
    }

    public static URI buildGetStudyFullUrl(@NonNull URI repoUrl, @NonNull String studyIdentifier, @NonNull String metadataPrefix) throws URISyntaxException {
        return new URI(String.format(
            "%s?%s=%s&%s=%s&%s=%s", repoUrl,
            VERB_PARAM_KEY, GET_RECORD_VALUE, // verb=GetRecord
            IDENTIFIER_PARAM_KEY, URLEncoder.encode(studyIdentifier, StandardCharsets.UTF_8), //&identifier=1683
            METADATA_PREFIX_PARAM_KEY, URLEncoder.encode(metadataPrefix, StandardCharsets.UTF_8) //&metadataPrefix=ddi
        ));
    }

    public static URI appendListRecordParams(@NonNull Repo repoConfig) {
        if (StringUtils.isBlank(repoConfig.getSetSpec())) {
            return URI.create(String.format("%s?%s=%s&%s=%s", repoConfig.getUrl(),
                VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
                METADATA_PREFIX_PARAM_KEY, repoConfig.getPreferredMetadataParam())); //&metadataPrefix=ddi
        } else {
            return URI.create(String.format("%s?%s=%s&%s=%s&%s=%s", repoConfig.getUrl(),
                VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
                METADATA_PREFIX_PARAM_KEY, repoConfig.getPreferredMetadataParam(), //&metadataPrefix=ddi
                SET_SPEC_PARAM_KEY, repoConfig.getSetSpec())); //&set=my:set
        }
    }

    public static URI appendListRecordResumptionToken(@NonNull URI baseRepoUrl, @NonNull String resumptionToken) {
        return URI.create(String.format("%s?%s=%s&%s=%s", baseRepoUrl,
            VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
            RESUMPTION_TOKEN_KEY, URLEncoder.encode(resumptionToken, StandardCharsets.UTF_8)) // &resumptionToken=0001/500....
        );
    }

    /**
     * Retrieve an instance of a {@link SAXBuilder}.
     */
    static SAXBuilder getSaxBuilder() {
        return SAX_BUILDER_THREAD_LOCAL.get();
    }
}
