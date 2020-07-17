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
package eu.cessda.pasc.oci.exception;

import com.vividsolutions.jts.util.Assert;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import org.junit.Test;

public class HandlerNotFoundExceptionTest {

    @Test
    public void shouldContainDetailsOfTheRepo() {
        // When
        var ukdsRepo = ReposTestData.getUKDSRepo();

        // Then
        var handlerNotFoundException = new HandlerNotFoundException(ukdsRepo);

        // Should contain the repository as a field
        Assert.equals(ukdsRepo, handlerNotFoundException.getRepo());
        Assert.isTrue(handlerNotFoundException.getMessage().contains(ukdsRepo.getCode()));
        Assert.isTrue(handlerNotFoundException.getMessage().contains(ukdsRepo.getHandler()));
    }
}