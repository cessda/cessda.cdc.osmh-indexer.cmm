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
package eu.cessda.pasc.oci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.models.PipelineMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
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
    @SuppressWarnings({"resource", "StreamResourceLeak"}) // closed by calling method
    public Stream<Repo> discoverRepositories(Path baseDirectory) {
        try {
            var directoryStream = Files.find(baseDirectory, Integer.MAX_VALUE,
                (path, attributes) -> attributes.isRegularFile() && path.getFileName().equals(Path.of("pipeline.json"))
            );
            return directoryStream.map(this::parseRepositoryConfiguration).filter(Objects::nonNull);
        } catch (IOException e) {
            log.error("Error occurred when loading repositories: {}", e.toString());
            return Stream.empty();
        }
    }

    /**
     * Parse a repository configuration JSON.
     *
     * @param json the path to the repository configuration.
     * @return a repository configuration, or {@code null} if an IO error occurs or
     * if the repository doesn't have the {@code CDC} role.
     */
    private Repo parseRepositoryConfiguration(Path json) {
        try (var inputStream = Files.newInputStream(json)) {
            PipelineMetadata sharedModel = repositoryModelObjectReader.readValue(inputStream);

            // Only harvest the repository if it has a CDC role
            if (sharedModel.role().contains("CDC")) {

                // Convert the shared model to a Repo object
                return new Repo(
                    sharedModel.url(),
                    json.getParent(),
                    sharedModel.code(),
                    sharedModel.name(),
                    sharedModel.metadataPrefix(),
                    sharedModel.defaultLanguage()
                );
            }
        } catch (IOException e) {
            log.error("Failed to load pipeline definition from \"{}\": {}", json, e.toString());
        }

        return null;
    }
}
