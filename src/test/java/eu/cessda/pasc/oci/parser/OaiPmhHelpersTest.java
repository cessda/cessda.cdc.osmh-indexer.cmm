/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static eu.cessda.pasc.oci.parser.OaiPmhHelpers.buildGetStudyFullUrl;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Test class for {@link OaiPmhHelpers}
 *
 * @author moses AT doraventures DOT com
 */
public class OaiPmhHelpersTest {

    private static final String STUDY_IDENTIFIER = "15454";

    private final AppConfigurationProperties appConfigurationProperties;

    public OaiPmhHelpersTest() {
        appConfigurationProperties = new AppConfigurationProperties(null, null, null, ReposTestData.getEndpoints());
    }

    @Test
    public void shouldAppendMetaDataPrefixForGivenFSD() throws URISyntaxException {

        // Given
        Repo fsdEndpoint = appConfigurationProperties.repos()
            .stream().filter(repo -> repo.code().equals("FSD")).findAny().orElseThrow();

        var expectedReqUrl = URI.create("http://services.fsd.uta.fi/v0/oai?verb=GetRecord&identifier=" + STUDY_IDENTIFIER +
            "&metadataPrefix=" + fsdEndpoint.preferredMetadataParam());

        // When
        URI builtUrl = buildGetStudyFullUrl(fsdEndpoint.url(), STUDY_IDENTIFIER, fsdEndpoint.preferredMetadataParam());

        then(builtUrl).isEqualTo(expectedReqUrl);
    }

    @Test
    public void shouldAppendMetaDataPrefixForGivenUKDS() throws URISyntaxException {

        // Given
        Repo ukdsEndpoint = appConfigurationProperties.repos()
                .stream().filter(repo -> repo.code().equals("UKDS")).findAny().orElseThrow();

        var expectedReqUrl = URI.create("https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=" + STUDY_IDENTIFIER +
            "&metadataPrefix=" + ukdsEndpoint.preferredMetadataParam());

        // When
        URI builtUrl = buildGetStudyFullUrl(ukdsEndpoint.url(), STUDY_IDENTIFIER, ukdsEndpoint.preferredMetadataParam());

        then(builtUrl).isEqualTo(expectedReqUrl);
    }
}
