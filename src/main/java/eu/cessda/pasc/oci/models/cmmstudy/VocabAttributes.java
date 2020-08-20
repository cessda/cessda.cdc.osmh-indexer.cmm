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
package eu.cessda.pasc.oci.models.cmmstudy;

import lombok.Builder;
import lombok.Value;

/**
 * VocabAttributes pojo to hold the control vocab attributes of element:
 * <p>
 * {@code
 * <keyword xml:lang="sv" ID="w8357xx45" vocab="ELSST" vocabURI="http://sv.ac.sv">
 * </keyword>
 * }
 *
 * @author moses AT doraventures DOT com
 */
@Builder
@Value
public class VocabAttributes {
    String vocab;
    String vocabUri;
    String id;
}
