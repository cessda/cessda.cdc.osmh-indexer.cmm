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
package eu.cessda.pasc.oci.elasticsearch;

import java.io.Serial;

public class IndexCreationFailedException extends IndexingException {

    @Serial
    private static final long serialVersionUID = 291787204525296576L;

    private final String indexName;

    IndexCreationFailedException(String indexName) {
        super("[" + indexName + "] Index creation failed");
        this.indexName = indexName;
    }

    IndexCreationFailedException(String indexName, Throwable cause) {
        super("[" + indexName + "] " + cause);
        this.indexName = indexName;
    }

    IndexCreationFailedException(String message, String indexName, Throwable cause) {
        super("[" + indexName + "] " + message + ": " + cause, cause);
        this.indexName = indexName;
    }

    public String getIndexName() {
        return indexName;
    }
}
