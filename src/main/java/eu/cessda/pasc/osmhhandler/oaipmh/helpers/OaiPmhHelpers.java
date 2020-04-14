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

package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.Repo;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;

/**
 * Helper methods to deal with Oai-pmh protocol
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@UtilityClass
public class OaiPmhHelpers {

  public String buildGetStudyFullUrl(String repositoryUrl, String studyIdentifier,
                                     HandlerConfigurationProperties oaiPmhConfig)
          throws CustomHandlerException {
    String decodedStudyId = decodeStudyNumber(studyIdentifier);
    return appendGetRecordParams(repositoryUrl, decodedStudyId, oaiPmhConfig.getOaiPmh());
  }

  public String appendListRecordParams(String repoUrl, OaiPmh oaiPmh) throws CustomHandlerException {
    Repo repoConfig = getMetadataPrefix(repoUrl, oaiPmh);

    if (StringUtils.isBlank(repoConfig.getSetSpec())) {
      return String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, repoUrl,
              VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
              METADATA_PREFIX_PARAM_KEY, repoConfig.getPreferredMetadataParam()); //&metadataPrefix=ddi
    } else {
      return String.format(LIST_RECORD_HEADERS_PER_SET_URL_TEMPLATE, repoUrl,
              VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
              METADATA_PREFIX_PARAM_KEY, repoConfig.getPreferredMetadataParam(), //&metadataPrefix=ddi
          SET_SPEC_PARAM_KEY, repoConfig.getSetSpec()); //&set=my:set
    }
  }

  public String appendListRecordResumptionToken(String baseRepoUrl, String resumptionToken) {
    return String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, baseRepoUrl,
            VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
            RESUMPTION_TOKEN_KEY, URLEncoder.encode(resumptionToken, StandardCharsets.UTF_8) // &resumptionToken=0001/500....
    );
  }

  private String appendGetRecordParams(String repositoryUrl, String identifier, OaiPmh oaiPmh)
          throws CustomHandlerException {
    return String.format(
            GET_RECORD_URL_TEMPLATE, repositoryUrl,
            VERB_PARAM_KEY, GET_RECORD_VALUE, // verb=GetRecord
            IDENTIFIER_PARAM_KEY, identifier, //&identifier=1683
            METADATA_PREFIX_PARAM_KEY, getMetadataPrefix(repositoryUrl, oaiPmh).getPreferredMetadataParam() //&metadataPrefix=ddi
    );
  }

  private String decodeStudyNumber(String encodedStudyNumber) {
    return encodedStudyNumber.replace("_dt_", ".")
            .replace("_sl_", "/")
            .replace("_cl_", ":");
  }

  private Repo getMetadataPrefix(String repoBaseUrl, OaiPmh oaiPmh) throws CustomHandlerException {

    Repo repo = oaiPmh.getRepos()
            .stream()
            .filter(currentStreamRepo -> currentStreamRepo.getUrl().equalsIgnoreCase(repoBaseUrl))
            .findFirst()
            .orElseThrow(() -> new CustomHandlerException(String.format("Configuration not found for Repo [%s]", repoBaseUrl)));

    log.debug("Retrieved Params for repoBaseUrl [{}] as [{}]", repoBaseUrl, repo);
    return repo;
  }
}
