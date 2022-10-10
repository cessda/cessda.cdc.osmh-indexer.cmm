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
package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * The group of persons or other elements that are the object of research and to which any analytic results refer.
 * <p>
 * Example universes may look like:
 * <pre>
 * {@code
 * <universe clusion="I">Individuals 15-19 years of age.</universe>
 * <universe clusion="E">Individuals younger than 15 and older than 19 years of age.</universe>
 * }
 * </pre>
 */
@Data
public class Universe {

    /**
     * The main content of the universe element.
     */
    @JsonProperty("content")
    String content;

    /**
     * Marks whether this universe is included or excluded. This defaults to an inclusion.
     */
    @JsonProperty("clusion")
    Clusion clusion = Clusion.I;

    /**
     * Is the universe included or excluded.
     */
    public enum Clusion { I, E }
}
