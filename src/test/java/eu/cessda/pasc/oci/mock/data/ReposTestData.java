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
package eu.cessda.pasc.oci.mock.data;

import eu.cessda.pasc.oci.models.configurations.Endpoints;
import eu.cessda.pasc.oci.models.configurations.Harvester;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.util.List;

@UtilityClass
public class ReposTestData
{

    public static Repo getUKDSRepo() {
        Repo repo = new Repo();
        repo.setCode("UKDS");
        repo.setUrl(URI.create("https://oai.ukdataservice.ac.uk:8443/oai/provider"));
        repo.setHandler("OAI-PMH");
        repo.setPreferredMetadataParam("ddi");
        return repo;
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
        repo.setHandler("OAI-PMH");
        return repo;
    }

    public static Repo getFSDRepo() {
        var repo = new Repo();
        repo.setUrl(URI.create("http://services.fsd.uta.fi/v0/oai"));
        repo.setCode("FSD");
        repo.setPreferredMetadataParam("oai_ddi25");
        repo.setSetSpec("study_groups:energia");
        repo.setHandler("OAI-PMH");
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

    public static Endpoints getSingleEndpoint() {
        Endpoints endpoints = new Endpoints();
        endpoints.getRepos().add(getUKDSRepo());
        endpoints.getHarvesters().put("OAI-PMH", getOaiPmhHarvester());
        endpoints.getHarvesters().put("NESSTAR", getNesstarHarvester());
        return endpoints;
    }

    public static Endpoints getEndpoints() {
        Endpoints endpoints = new Endpoints();
        endpoints.getRepos().add(getUKDSRepo());
        endpoints.getRepos().add(getGesisEnRepo());
        endpoints.getRepos().add(getFSDRepo());
        endpoints.getHarvesters().put("OAI-PMH", getOaiPmhHarvester());
        endpoints.getHarvesters().put("NESSTAR", getNesstarHarvester());
        return endpoints;
    }

    public static List<String> getListOfLanguages() {
        return List.of("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");
    }
}
