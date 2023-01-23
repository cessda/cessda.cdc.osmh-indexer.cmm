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
package eu.cessda.pasc.oci.exception;

import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.Getter;

import java.io.Serial;

/**
 * Represents that a repository's handler was not configured.
 */
@Getter
public class HandlerNotFoundException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = 229579898689610309L;

    /**
     * The repository that caused this exception.
     */
    private final Repo repo;

    /**
     * Constructs a {@link HandlerNotFoundException} with the specified repository.
     *
     * @param repo the repository to construct the message from
     */
    public HandlerNotFoundException(Repo repo) {
        super("Handler " + repo.getHandler() + " for repository " + repo.getCode() + " not configured");
        this.repo = repo;
    }
}
