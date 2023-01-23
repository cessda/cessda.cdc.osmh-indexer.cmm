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
package eu.cessda.pasc.oci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import eu.cessda.pasc.oci.models.PipelineMetadata;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Service
@Slf4j
public class PipelineUtilities {

    private final ObjectReader repositoryModelObjectReader;

    public PipelineUtilities(ObjectMapper objectMapper) {
        repositoryModelObjectReader = objectMapper.readerFor(PipelineMetadata.class);
    }

    /**
     * Discover repositories by looking for instances of {@code pipeline.json}.
     *
     * @param baseDirectory the base directory to search from.
     * @return a list of all discovered repositories.
     */
    @SuppressWarnings("resource") // closed by calling method
    public Stream<Repo> discoverRepositories(Path baseDirectory) {
        if (baseDirectory != null) {
            try {
                var directoryStream = Files.find(baseDirectory, Integer.MAX_VALUE,
                    (path, attributes) -> attributes.isRegularFile() && path.getFileName().toString().equals("pipeline.json")
                );
                return directoryStream.flatMap(json -> {
                    try (var inputStream = Files.newInputStream(json)) {
                        PipelineMetadata sharedModel = repositoryModelObjectReader.readValue(inputStream);

                        // Convert the shared model to a Repo object
                        var repo = new Repo();
                        repo.setUrl(sharedModel.getUrl());
                        repo.setCode(sharedModel.getCode());
                        repo.setName(sharedModel.getName());
                        repo.setHandler(sharedModel.getDdiVersion());
                        repo.setPath(json.getParent());
                        repo.setDefaultLanguage(sharedModel.getDefaultLanguage());

                        // Add the repo object to the stream
                        return Stream.of(repo);
                    } catch (IOException e) {
                        log.error("Failed to load pipeline definition from \"{}\": {}", json, e.toString());
                        return Stream.empty();
                    }
                });
            } catch (IOException e) {
                log.error("Error occurred when loading repositories: {}", e.toString());
            }
        }
        return Stream.empty();
    }
}