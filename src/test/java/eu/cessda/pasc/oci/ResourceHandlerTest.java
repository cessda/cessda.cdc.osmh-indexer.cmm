/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author moses AT doraventures DOT com
 */
public class ResourceHandlerTest {

    @Test
    public void shouldReturnContentToAValidFilePath() throws IOException {
        String recordUkds998 = ResourceHandler.getResourceAsString("record_ukds_998.json");
        assertThat(recordUkds998).isNotEmpty();
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldThrowFileNotFoundExceptionForAnInvalidValidFilePath() throws IOException {
        ResourceHandler.getResourceAsString("does_not_exist.json");
    }
}
