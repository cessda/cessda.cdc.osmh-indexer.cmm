package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorMessage;
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerErrorMessageUtil.getSimpleResponseMessage;

/**
 * Controller to handle request for services, versions and metadata formats supported by this api
 *
 * @author moses@doraventures.com
 */
@RestController
@RequestMapping("/SupportedVersions")
@Api(value = "SupportedVersions", description = "REST API for supported api versions", tags = {"SupportedVersions"})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SupportedAPIVersionsController {

  @Autowired
  APISupportedService apiSupportedService;

  @RequestMapping(method = RequestMethod.GET)
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


  @RequestMapping(path = "*", method = RequestMethod.GET)
  @ApiOperation(value = RETURN_404_FOR_OTHER_PATHS,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getSupportedVersionForOtherPaths() {

    return new ResponseEntity<>(getSimpleResponseMessage(THE_GIVEN_URL_IS_NOT_FOUND), HttpStatus.NOT_FOUND);
  }
}
