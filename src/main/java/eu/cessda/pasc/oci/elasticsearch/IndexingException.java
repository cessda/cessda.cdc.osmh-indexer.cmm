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
package eu.cessda.pasc.oci.elasticsearch;

import java.io.Serial;

public class IndexingException extends Exception {

    @Serial
    private static final long serialVersionUID = 7423147835862189418L;

    IndexingException(String message) {
        super(message);
    }

    IndexingException(String message, Throwable cause) {
        super(message, cause);
    }

    IndexingException(Throwable cause) {
        super(cause);
    }
}
