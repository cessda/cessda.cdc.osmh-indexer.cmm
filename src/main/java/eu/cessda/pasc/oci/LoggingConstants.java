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
package eu.cessda.pasc.oci;

import lombok.experimental.UtilityClass;

/**
 * Contains static logging fields that are used for structured logging
 */
@UtilityClass
public class LoggingConstants {
    public static final String STUDY_ID = "study_id";
    public static final String OAI_ERROR_CODE = "oai_error_code";
    public static final String OAI_ERROR_MESSAGE = "oai_error_message";
    public static final String REPO_NAME = "repo_name";
    public static final String LANG_CODE = "lang_code";
    public static final String REASON = "rejection_reason";
}
