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

import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static eu.cessda.pasc.oci.helpers.OaiPmhConstants.*;

/**
 * Helper methods to deal with Oai-pmh protocol
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@UtilityClass
public class OaiPmhHelpers {

  public static URI buildGetStudyFullUrl(Repo repo, String studyIdentifier) throws URISyntaxException {
    return new URI(String.format(
            GET_RECORD_URL_TEMPLATE, repo.getUrl(),
            VERB_PARAM_KEY, GET_RECORD_VALUE, // verb=GetRecord
            IDENTIFIER_PARAM_KEY, URLEncoder.encode(studyIdentifier, StandardCharsets.UTF_8), //&identifier=1683
            METADATA_PREFIX_PARAM_KEY, URLEncoder.encode(repo.getPreferredMetadataParam(), StandardCharsets.UTF_8) //&metadataPrefix=ddi
    ));
  }

  public static URI appendListRecordParams(Repo repoConfig) {
    if (StringUtils.isBlank(repoConfig.getSetSpec())) {
      return URI.create(String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, repoConfig.getUrl(),
              VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
              METADATA_PREFIX_PARAM_KEY, repoConfig.getPreferredMetadataParam())); //&metadataPrefix=ddi
    } else {
      return URI.create(String.format(LIST_RECORD_HEADERS_PER_SET_URL_TEMPLATE, repoConfig.getUrl(),
              VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
              METADATA_PREFIX_PARAM_KEY, repoConfig.getPreferredMetadataParam(), //&metadataPrefix=ddi
              SET_SPEC_PARAM_KEY, repoConfig.getSetSpec())); //&set=my:set
    }
  }

  public static URI appendListRecordResumptionToken(URI baseRepoUrl, String resumptionToken) {
    return URI.create(String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, baseRepoUrl,
            VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
            RESUMPTION_TOKEN_KEY, URLEncoder.encode(resumptionToken, StandardCharsets.UTF_8)) // &resumptionToken=0001/500....
    );
  }

}
