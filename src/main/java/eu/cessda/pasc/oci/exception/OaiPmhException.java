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

import lombok.NonNull;

import java.util.Optional;

/**
 * Represents OAI-PMH errors.
 *
 * @see <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#ErrorConditions">OAI-PMH Error and Exception Conditions</a>
 */
public class OaiPmhException extends CustomHandlerException {
    private static final long serialVersionUID = 4623452253483287507L;

    /**
     * The OAI-PMH error code.
     */
    @NonNull
    private final Code code;
    /**
     * The OAI-PMH error message. This is an optional field.
     */
    private final String oaiErrorMessage;

    /**
     * Constructs a new exception with the specified OAI-PMH error code.
     * This initialises the exception with no OAI-PMH error message.
     *
     * @param code The OAI-PMH error code.
     */
    public OaiPmhException(Code code) {
        super(code.toString());
        this.code = code;
        this.oaiErrorMessage = null;
    }

    /**
     * Constructs a new exception with the specified OAI-PMH error code and message.
     *
     * @param code            The OAI-PMH error code.
     * @param oaiErrorMessage The OAI-PMH error message.
     */
    public OaiPmhException(Code code, @NonNull String oaiErrorMessage) {
        super(code.toString() + ": " + oaiErrorMessage);
        this.code = code;
        this.oaiErrorMessage = oaiErrorMessage;
    }

    /**
     * Gets the OAI-PMH error code.
     */
    public Code getCode() {
        return code;
    }

    /**
     * Gets an optional containing an OAI-PMH error message, or an empty optional if no message was set.
     */
    public Optional<String> getOaiErrorMessage() {
        return Optional.ofNullable(oaiErrorMessage);
    }

    /**
     * OAI-PMH error codes
     */
    public enum Code {
        badArgument,
        badResumptionToken,
        badVerb,
        cannotDisseminateFormat,
        idDoesNotExist,
        noRecordsMatch,
        noMetadataFormats,
        noSetHierarchy
    }
}
