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
package eu.cessda.pasc.oci.exception;

import eu.cessda.pasc.oci.models.cmmstudy.Universe;

import java.io.Serial;
import java.util.Arrays;

public class InvalidUniverseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8948529259968345480L;

    /**
     * Constructs a new invalid universe exception with the invalid clusion and the cause.
     * @param invalidClusion the invalid clusion.
     * @param cause the exception that caused this exception.
     */
    public InvalidUniverseException(String invalidClusion, IllegalArgumentException cause) {
        super("[" + invalidClusion + "] is not a valid inclusion/exclusion - valid values: " + Arrays.toString(Universe.Clusion.values()), cause);
    }
}
