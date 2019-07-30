/**
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMConverter;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorMessage;
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedService;
import eu.cessda.pasc.osmhhandler.oaipmh.service.GetRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.*;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Controller to handle request for getting a remote Record.
 *
 * @author moses AT doraventures DOT com
 */
@RestController
@RequestMapping("{version}/GetRecord")
@Api(value = "GetRecord", description = "REST API for getting a remote Record",
    tags = {"GetRecord"})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GetRecordController extends ControllerBase {

  private static final String GETS_A_RECORD = "Gets a Record with the given identifier";

  @Autowired
  APISupportedService apiSupportedService;

  @Autowired
  GetRecordService getRecordService;

  @RequestMapping(value = "/CMMStudy/{studyId}", method = RequestMethod.GET)
  @ApiOperation(value = GETS_A_RECORD,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = SUCCESSFUL_OPERATION, response = CMMStudy.class),
      @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMessage.class),
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class),
      @ApiResponse(code = 500, message = SYSTEM_ERROR, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getRecordHeaders(
      @PathVariable String version, @PathVariable String studyId, @RequestParam("Repository") String repository) {

    try {
      if (!apiSupportedService.isSupportedVersion(version)) {
        return getResponseEntityMessage(String.format(UNSUPPORTED_API_VERSION, version), HttpStatus.BAD_REQUEST);
      }
      CMMStudy cmmStudy = getRecordService.getRecord(repository, studyId);
      String valueAsString = CMMConverter.toJsonString(cmmStudy);
      return getResponseEntity(valueAsString, HttpStatus.OK);
    } catch (CustomHandlerException e) {
      log.debug("[{}] [{}]", e.getClass().getSimpleName(), e.getMessage());
      return logAndGetResponseEntityMessage(e.getClass().getSimpleName() + ": " + e.getMessage(), INTERNAL_SERVER_ERROR, log);
    } catch (Exception e) {
      log.debug("[{}] [{}]", e.getClass().getSimpleName(), e.getMessage(), e);
      return logAndGetResponseEntityMessage(SYSTEM_ERROR + ": " + e.getMessage(), INTERNAL_SERVER_ERROR, log);
    }
  }

  @RequestMapping(path = "/**", method = RequestMethod.GET)
  @ApiOperation(value = RETURN_404_FOR_OTHER_PATHS,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getRecordHeadersForOtherPaths() {
    return getResponseEntityMessage(THE_GIVEN_URL_IS_NOT_FOUND, HttpStatus.NOT_FOUND);
  }
}
