/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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
import eu.cessda.pasc.oci.configurations.Repo;
import lombok.experimental.UtilityClass;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

@UtilityClass
public class ReposTestData
{

    public static Repo getUKDSRepo() {
        try {
            return new Repo(
                URI.create("https://oai.ukdataservice.ac.uk:8443/oai/provider"),
                Path.of(ResourceHandler.getResource("xml/ddi_2_5/").toURI()),
                "UKDS",
                null,
                "ddi",
                null,
                null
            );
        } catch (FileNotFoundException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Repo getUKDSLanguageOverrideRepository() {
        try {
            return new Repo(
                URI.create("https://oai.ukdataservice.ac.uk:8443/oai/provider"),
                Path.of(ResourceHandler.getResource("xml/ddi_2_5/").toURI()),
                "UKDS",
                null,
                "ddi",
                null,
                "zz"
            );
        } catch (FileNotFoundException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Repo getGesisEnRepo() {
        return new Repo(
            URI.create("https://dbkapps.gesis.org/dbkoai3"),
            null,
            "GESIS",
            null,
            null,
            null,
            null
        );
    }

    public static Repo getFSDRepo() {
        return new Repo(
            URI.create("http://services.fsd.uta.fi/v0/oai"),
            null,
            "FSD",
            null,
            "oai_ddi25",
            null,
            null
        );
    }

    public static Repo getNSDRepo() {
        return new Repo(
            URI.create("https://oai-pmh.nsd.no/oai-pmh"),
            null,
            "NSD",
            null,
            "oai_ddi",
            null,
            null
        );
    }

    public static List<Repo> getSingleEndpoint() {
        return List.of(getUKDSRepo());
    }

    public static List<Repo> getEndpoints() {
        return List.of(
            getUKDSRepo(),
            getGesisEnRepo(),
            getFSDRepo()
        );
    }

    public static List<String> getListOfLanguages() {
        return List.of("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");
    }
}
