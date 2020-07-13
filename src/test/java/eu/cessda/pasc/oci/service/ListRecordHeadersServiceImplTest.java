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

package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.exception.CustomHandlerException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.mock.data.RecordHeadersMock;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.repository.DaoBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static eu.cessda.pasc.oci.helpers.OaiPmhHelpers.appendListRecordResumptionToken;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;


/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ListRecordHeadersServiceImplTest {

  @MockBean
  DaoBase daoBase;

  @Autowired
  ListRecordHeadersService listRecordHeadersService;

  @Autowired
  AppConfigurationProperties appConfigurationProperties;

  @Test
  public void shouldReturnRecordHeadersForGivenRepo() throws CustomHandlerException, IOException {

    // Given
    Repo ukdsEndpoint = appConfigurationProperties.getEndpoints().getRepos()
            .stream().filter(repo -> repo.getCode().equals("UKDS")).findAny().orElseThrow();
    String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";
    String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLResumptionEmpty();
    given(daoBase.getInputStream(URI.create(fullListRecordRepoUrl))).willReturn(
            new ByteArrayInputStream(mockRecordHeadersXml.getBytes(StandardCharsets.UTF_8))
    );

    // When
    List<RecordHeader> recordHeaders = listRecordHeadersService.getRecordHeaders(ukdsEndpoint);

    then(recordHeaders).hasSize(3);
    then(recordHeaders).extracting("identifier").containsOnly("850229", "850232", "850235");
    then(recordHeaders).extracting("lastModified").containsOnly("2017-11-20T10:37:18Z");
    then(recordHeaders).extracting("type").containsOnly("Study");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowWhenRequestForHeaderFails() throws CustomHandlerException, IOException {

    // Given
    Repo ukdsEndpoint = appConfigurationProperties.getEndpoints().getRepos()
            .stream().filter(repo -> repo.getCode().equals("UKDS")).findAny().orElseThrow();
    String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";
    String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLResumptionTokenNotMockedForInvalid();
    given(daoBase.getInputStream(URI.create(fullListRecordRepoUrl))).willReturn(
            new ByteArrayInputStream(mockRecordHeadersXml.getBytes(StandardCharsets.UTF_8))
    );

    // When
    listRecordHeadersService.getRecordHeaders(ukdsEndpoint);
  }

  @Test
  public void shouldRecursivelyLoopThroughTheOaiPMHResponseResumptionTokenToRetrieveReposCompleteListSize()
          throws CustomHandlerException, IOException {

    // Given
    Repo ukdsEndpoint = appConfigurationProperties.getEndpoints().getRepos()
            .stream().filter(repo -> repo.getCode().equals("UKDS")).findAny().orElseThrow();
    URI fullListRecordRepoUrl = URI.create(ukdsEndpoint.getUrl().toString() + "?verb=ListIdentifiers&metadataPrefix=ddi");
    String identifiersXML = RecordHeadersMock.getListIdentifiersXML();

    String resumptionToken01 = "0/3/7/ddi/null/2016-06-01/null";
    URI repoUrlWithResumptionToken01 = appendListRecordResumptionToken(ukdsEndpoint.getUrl(), resumptionToken01);
    String identifiersXMLWithResumption = RecordHeadersMock.getListIdentifiersXMLWithResumption();

    String resumptionToken02 = "3/6/7/ddi/null/2017-01-01/null";
    URI repoUrlWithResumptionToken02 = appendListRecordResumptionToken(ukdsEndpoint.getUrl(), resumptionToken02);
    String identifiersXMLWithResumptionLastList = RecordHeadersMock.getListIdentifiersXMLWithResumptionLastList();

    given(daoBase.getInputStream(fullListRecordRepoUrl)).willReturn(
            new ByteArrayInputStream(identifiersXML.getBytes(StandardCharsets.UTF_8))
    );

    given(daoBase.getInputStream(repoUrlWithResumptionToken01)).willReturn(
            new ByteArrayInputStream(identifiersXMLWithResumption.getBytes(StandardCharsets.UTF_8))
    );

    given(daoBase.getInputStream(repoUrlWithResumptionToken02)).willReturn(
            new ByteArrayInputStream(identifiersXMLWithResumptionLastList.getBytes(StandardCharsets.UTF_8))
    );

    // When
    List<RecordHeader> recordHeaders = listRecordHeadersService.getRecordHeaders(ukdsEndpoint);

    then(recordHeaders).hasSize(7);
    then(recordHeaders).extracting("identifier")
            .containsOnly("850229", "850232", "850235", "7753", "8300", "8301", "998");
    then(recordHeaders).extracting("lastModified")
            .containsOnly("2017-11-20T10:37:18Z", "2018-01-11T07:43:20Z", "2018-01-11T07:43:39Z");
    then(recordHeaders).extracting("type").containsOnly("Study");
  }

  @Test(expected = OaiPmhException.class)
  public void shouldThrowExceptionForRecordHeadersInvalidMetadataToken() throws CustomHandlerException, IOException {

    // Given
    Repo ukdsEndpoint = appConfigurationProperties.getEndpoints().getRepos()
            .stream().filter(repo -> repo.getCode().equals("UKDS")).findAny().orElseThrow();
    String fullListRecordRepoUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";

    String mockRecordHeadersXml = RecordHeadersMock.getListIdentifiersXMLWithInvalidMetadataTokenError();
    given(daoBase.getInputStream(URI.create(fullListRecordRepoUrl))).willReturn(
            new ByteArrayInputStream(mockRecordHeadersXml.getBytes(StandardCharsets.UTF_8))
    );

    // When
    listRecordHeadersService.getRecordHeaders(ukdsEndpoint);
  }
}
