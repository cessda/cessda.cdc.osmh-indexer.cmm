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
package eu.cessda.pasc.oci.mock.data;

import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.configurations.Harvester;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.experimental.UtilityClass;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@UtilityClass
public class ReposTestData
{

    public static Repo getUKDSRepo() {
        try {
            Repo repo = new Repo();
            repo.setCode("UKDS");
            repo.setUrl(URI.create("https://oai.ukdataservice.ac.uk:8443/oai/provider"));
            repo.setPath(Path.of(ResourceHandler.getResource("xml/ddi_2_5/").toURI()));
            repo.setPreferredMetadataParam("ddi");
            return repo;
        } catch (FileNotFoundException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Repo getUKDSLanguageOverrideRepository() {
        Repo repo = getUKDSRepo();
        repo.setDefaultLanguage("zz");
        return repo;
    }

    public static Repo getGesisEnRepo() {
        Repo repo = new Repo();
        repo.setCode("GESIS");
        repo.setUrl(URI.create("https://dbkapps.gesis.org/dbkoai3"));
        return repo;
    }

    public static Repo getFSDRepo() {
        var repo = new Repo();
        repo.setUrl(URI.create("http://services.fsd.uta.fi/v0/oai"));
        repo.setCode("FSD");
        repo.setPreferredMetadataParam("oai_ddi25");
        repo.setSetSpec("study_groups:energia");
        return repo;
    }

    public static Repo getNSDRepo() {
        var repo = new Repo();
        repo.setUrl(URI.create("https://oai-pmh.nsd.no/oai-pmh"));
        repo.setCode("NSD");
        repo.setPreferredMetadataParam("oai_ddi");
        return repo;
    }

    public static Harvester getOaiPmhHarvester() {
        var nesstarHarvester = new Harvester();
        nesstarHarvester.setUrl(URI.create("http://localhost:9091"));
        nesstarHarvester.setVersion("v0");
        return nesstarHarvester;
    }

    public static Harvester getNesstarHarvester() {
        var nesstarHarvester = new Harvester();
        nesstarHarvester.setUrl(URI.create("http://localhost:9842"));
        nesstarHarvester.setVersion("v0");
        return nesstarHarvester;
    }

    public static AppConfigurationProperties.Endpoints getSingleEndpoint() {
        var endpoints = new AppConfigurationProperties.Endpoints();
        endpoints.setRepos(List.of(getUKDSRepo()));
        endpoints.setHarvesters(Map.ofEntries(
            entry("DDI_2_5", getOaiPmhHarvester()),
            entry("NESSTAR", getNesstarHarvester())
        ));
        return endpoints;
    }

    public static AppConfigurationProperties.Endpoints getEndpoints() {
        var endpoints = new AppConfigurationProperties.Endpoints();
        endpoints.setRepos(List.of(
            getUKDSRepo(),
            getGesisEnRepo(),
            getFSDRepo()
        ));
        endpoints.setHarvesters(Map.ofEntries(
            entry("DDI_2_5", getOaiPmhHarvester()),
            entry("NESSTAR", getNesstarHarvester())
        ));
        return endpoints;
    }

    public static List<String> getListOfLanguages() {
        return List.of("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");
    }
}
