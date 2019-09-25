/*
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

import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorMessage;
import eu.cessda.pasc.osmhhandler.oaipmh.models.response.RecordHeader;
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedService;
import eu.cessda.pasc.osmhhandler.oaipmh.service.ListRecordHeadersServiceImpl;
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

import java.util.List;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.*;

/**
 * Controller to handle request for Record Headers.
 *
 * @author moses AT doraventures DOT com
 */
@RestController
@RequestMapping("{version}/ListRecordHeaders")
@Api(value = "ListRecordHeaders", description = "REST API for Listing Record Headers",
    tags = {"ListRecordHeaders"})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ListRecordHeadersController extends ControllerBase {

  private static final String GETS_A_LIST_OF_ALL_SUPPORTED_RECORD_TYPES = "Gets a list of all supported record types";

  @Autowired
  APISupportedService apiSupportedService;

  @Autowired
  ListRecordHeadersServiceImpl listRecordHeadersService;

  @GetMapping()
  @ApiOperation(value = GETS_A_LIST_OF_ALL_SUPPORTED_RECORD_TYPES,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = SUCCESSFUL_OPERATION, response = RecordHeader[].class),
      @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMessage.class),
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class),
      @ApiResponse(code = 500, message = SYSTEM_ERROR, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getRecordHeaders(
      @PathVariable String version, @RequestParam("Repository") String repository) {

    try {

      if (!apiSupportedService.isSupportedVersion(version)) {
        return getResponseEntityMessage(UNSUPPORTED_API_VERSION, HttpStatus.BAD_REQUEST);
      }

      List<RecordHeader> recordHeaders = listRecordHeadersService.getRecordHeaders(repository);
      String valueAsString = objectMapper.writeValueAsString(recordHeaders);
      return getResponseEntity(valueAsString, HttpStatus.OK);
    } catch (Exception e) {
      return logAndGetResponseEntityMessage(SYSTEM_ERROR + ": " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, log);
    }
  }


  @GetMapping(path = "*")
  @ApiOperation(value = RETURN_404_FOR_OTHER_PATHS,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getRecordHeadersForOtherPaths(
      @PathVariable String version, @RequestParam("Repository") String repository) {

    return getResponseEntityMessage(THE_GIVEN_URL_IS_NOT_FOUND, HttpStatus.NOT_FOUND);
  }
}
