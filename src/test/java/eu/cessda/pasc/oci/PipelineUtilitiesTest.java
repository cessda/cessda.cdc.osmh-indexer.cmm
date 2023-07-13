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
import eu.cessda.pasc.oci.configurations.Repo;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineUtilitiesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PipelineUtilities pipelineUtilities = new PipelineUtilities(objectMapper);

    @Test
    void shouldDiscoverRepositories() {
        var pipeline = Path.of("src/test/resources/pipeline");
        var discoveredRepositories = pipelineUtilities.discoverRepositories(pipeline);

        // Should discover 2 repositories, ignoring the invalid definition.
        var repositoryAssert = assertThat(discoveredRepositories);
        repositoryAssert.hasSize(2);
        repositoryAssert.map(Repo::code).containsOnly("APIS", "UniData");
        repositoryAssert.map(Repo::preferredMetadataParam).containsAnyElementsOf(List.of("ddi_c", "oai_ddi25"));
    }

    @Test
    void shouldReturnEmptyListIfNoRepositoriesWereDiscovered() {
        // This is a directory that contains XML files, but no pipeline definitions.
        var xmlDirectory = Path.of("src/test/resources/xml/ddi_2_5");
        var discoveredRepositories = pipelineUtilities.discoverRepositories(xmlDirectory);

        // There are no instances of pipeline.json, so no repositories should be discovered.
        assertThat(discoveredRepositories).isEmpty();
    }

    @Test
    void shouldThrowIfAnIOErrorOccurs() {
        // Define a directory that doesn't exist
        var nonExistentDirectory = Path.of("this/directory/does/not/exist");
        assertThat(Files.exists(nonExistentDirectory)).isFalse();

        pipelineUtilities.discoverRepositories(nonExistentDirectory);
    }
}
