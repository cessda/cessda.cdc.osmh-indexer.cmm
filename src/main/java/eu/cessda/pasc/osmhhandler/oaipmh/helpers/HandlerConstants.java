/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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

package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import lombok.experimental.UtilityClass;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Harvester (OSMH) Handler concept Constants
 *
 * @author moses AT doraventures DOT com
 */
@UtilityClass
public class HandlerConstants {

  // Messaging and Exceptions
  public static final String UNSUPPORTED_API_VERSION = "Unsupported API-version [%s]";
  public static final String SYSTEM_ERROR = "Internal OAI-PMH Handler System error!";
  public static final String SUCCESSFUL_OPERATION = "Successful operation!";
  public static final String BAD_REQUEST = "Bad request!";
  public static final String NOT_FOUND = "Not found!";
  public static final String THE_GIVEN_URL_IS_NOT_FOUND = "The given url is not found!";
  public static final String RETURN_404_FOR_OTHER_PATHS = "Return 404 for other paths.";
  public static final String MESSAGE = "message";
  public static final String RECORD_HEADER = "RecordHeader";

  // Metadata handling
  static final String NOT_AVAIL = "not available";
  static final String EMPTY_EL = "empty";
  public static final String STUDY = "Study";

  // Date / time
  static final String[] EXPECTED_DATE_FORMATS = new String[]{
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-dd-MM HH:mm:ss.SSS",
      "yyyy-MM-dd",
      "yyyy-MM-dd'T'HH:mm:ssZ",
      "yyyy-MM", "yyyy"
  };

  // System
  private static final String UTC_ID = TimeZone.getTimeZone("UTC").getID();
  static final ZoneId UTC_ZONE_ID = ZoneId.of(UTC_ID);
}
