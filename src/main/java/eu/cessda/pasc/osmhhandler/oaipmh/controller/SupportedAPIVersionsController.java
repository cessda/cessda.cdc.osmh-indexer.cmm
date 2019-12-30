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
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.BAD_REQUEST;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.NOT_FOUND;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.RETURN_404_FOR_OTHER_PATHS;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.SUCCESSFUL_OPERATION;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.SYSTEM_ERROR;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.THE_GIVEN_URL_IS_NOT_FOUND;

/**
 * Controller to handle request for services, versions and metadata formats supported by this api
 *
 * @author moses AT doraventures DOT com
 */
@RestController
@RequestMapping("/SupportedVersions")
@Api(value = "SupportedVersions", tags = {"SupportedVersions"})
public class SupportedAPIVersionsController extends ControllerBase {

  private final APISupportedService apiSupportedService;

  @Autowired
  public SupportedAPIVersionsController(APISupportedService apiSupportedService) {
    this.apiSupportedService = apiSupportedService;
  }

  @GetMapping()
  @ApiOperation(value = "Gets a list of supported api version numbers.",
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = SUCCESSFUL_OPERATION, response = String[].class),
      @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMessage.class),
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class),
      @ApiResponse(code = 500, message = SYSTEM_ERROR, response = ErrorMessage.class)
  })
  public ResponseEntity<List<String>> getSupportedVersion() {
    return new ResponseEntity<>(apiSupportedService.getSupportedVersion(), HttpStatus.OK);
  }

  @GetMapping(path = "*")
  @ApiOperation(value = RETURN_404_FOR_OTHER_PATHS,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getSupportedVersionForOtherPaths() {
    return getResponseEntityMessage(THE_GIVEN_URL_IS_NOT_FOUND, HttpStatus.NOT_FOUND);
  }
}
