package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerErrorMessageUtil.getSimpleResponseMessage;

/**
 * Controller to handle request for getting a remote Record.
 *
 * @author moses@doraventures.com
 */
@RestController
@RequestMapping("{version}/GetRecord")
@Api(value = "GetRecord", description = "REST API for getting a remote Record",
    tags = {"GetRecord"})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GetRecordController {

  private static final String GETS_A_RECORD = "Gets a Record with the given identifier";

  @Autowired
  APISupportedService apiSupportedService;

  @Autowired
  GetRecordService recordService;

  @Autowired
  ObjectMapper objectMapper;

  @RequestMapping(value = "/CMMStudy/{study_id}", method = RequestMethod.GET)
  @ApiOperation(value = GETS_A_RECORD,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = SUCCESSFUL_OPERATION, response = CMMStudy.class),
      @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMessage.class),
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class),
      @ApiResponse(code = 500, message = SYSTEM_ERROR, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getRecordHeaders(
      @PathVariable String version, @PathVariable String study_id, @RequestParam("Repository") String repository) {

    try {
      if (!apiSupportedService.isSupportedVersion(version)) {
        String message = String.format(UNSUPPORTED_API_VERSION, version);
        return new ResponseEntity<>(getSimpleResponseMessage(message), HttpStatus.BAD_REQUEST);
      }

      CMMStudy cmmStudy = recordService.getRecord(repository, study_id);
      String valueAsString = CMMConverter.toJsonString(cmmStudy);
      return new ResponseEntity<>(valueAsString, HttpStatus.OK);
    } catch (Exception e) {

      log.debug(SYSTEM_ERROR, e.getMessage());
      return new ResponseEntity<>(
          getSimpleResponseMessage(SYSTEM_ERROR + " " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @RequestMapping(path = "/**", method = RequestMethod.GET)
  @ApiOperation(value = RETURN_404_FOR_OTHER_PATHS,
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = NOT_FOUND, response = ErrorMessage.class)
  })
  public ResponseEntity<String> getRecordHeadersForOtherPaths() {

    return new ResponseEntity<>(getSimpleResponseMessage(THE_GIVEN_URL_IS_NOT_FOUND), HttpStatus.NOT_FOUND);
  }
}
