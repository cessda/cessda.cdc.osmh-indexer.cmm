package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorMessage;
import eu.cessda.pasc.osmhhandler.oaipmh.service.APISupportedService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.*;

/**
 * Controller to handle request for supported Record Types per supported API version.
 *
 * @author moses@doraventures.com
 */
@RestController
@RequestMapping("{version}/ListSupportedRecordTypes")
@Api(value = "ListSupportedRecordTypes", description = "REST API for supported record types",
    tags = {"ListSupportedRecordTypes"})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ListSupportedRecordTypesController extends ControllerBase{

  private static final String GETS_A_LIST_OF_SUPPORTED_RECORD_TYPES = "Gets a list of Supported Record Types.";

  @Autowired
  APISupportedService apiSupportedService;

  @RequestMapping(method = RequestMethod.GET)
  @ApiOperation(value = GETS_A_LIST_OF_SUPPORTED_RECORD_TYPES,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = SUCCESSFUL_OPERATION, response = String[].class),
      @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMessage.class),
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class),
      @ApiResponse(code = 500, message = SYSTEM_ERROR, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getSupportedRecordTypes(@PathVariable String version) {

    try {
      if (apiSupportedService.isSupportedVersion(version)) {
        String valueAsString = objectMapper.writeValueAsString(apiSupportedService.getSupportedRecordTypes());
        return new ResponseEntity<>(valueAsString, HttpStatus.OK);
      }
      return getResponseEntityMessage(UNSUPPORTED_API_VERSION, HttpStatus.BAD_REQUEST);
    } catch (JsonProcessingException e) {
      return logAndGetResponseEntityMessage(SYSTEM_ERROR + ": " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, log);
    }
  }

  @RequestMapping(path = "*", method = RequestMethod.GET)
  @ApiOperation(value = RETURN_404_FOR_OTHER_PATHS,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getSupportedRecordTypesForOtherPaths(@PathVariable String version) {

    return getResponseEntityMessage(THE_GIVEN_URL_IS_NOT_FOUND, HttpStatus.NOT_FOUND);
  }

}
