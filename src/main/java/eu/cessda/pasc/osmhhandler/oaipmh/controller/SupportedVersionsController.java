package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cessda.pasc.osmhhandler.oaipmh.configurations.PaSCHandlerOaiPmhConfig;
import eu.cessda.pasc.osmhhandler.oaipmh.models.errors.ErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller to handle request about services, versions and metadata formats supported by this api
 *
 * @author moses@doraventures.com
 */
@RestController
@RequestMapping("/SupportedVersions")
@Api(value = "SupportedVersions",
    description = "REST API for supported api versions", tags = {"SupportedVersions"})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SupportedVersionsController {

  @Autowired
  @Qualifier("PaSCHandlerOaiPmhConfig")
  PaSCHandlerOaiPmhConfig pmhConfig;

  @Autowired
  public ObjectMapper objectMapper;

  @RequestMapping(method = RequestMethod.GET)
  @ApiOperation(value = "Gets a list of supported api version numbers.",
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful operation!", response = String[].class),
      @ApiResponse(code = 400, message = "Bad request!", response = ErrorMessage.class),
      @ApiResponse(code = 404, message = "Not found!", response = ErrorMessage.class),
      @ApiResponse(code = 500, message = "System error!", response = ErrorMessage.class)
  })
  public ResponseEntity<List<String>> getSupportedVersion() {

    List<String> supportedApiVersions = pmhConfig.getOaiPmh().getSupportedApiVersions();
    return new ResponseEntity<>(supportedApiVersions, HttpStatus.OK);
  }


  @RequestMapping(path = "*", method = RequestMethod.GET)
  @ApiOperation(value = "Return 404 for other paths.",
      response = String.class, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Not found!", response = ErrorMessage.class)
  })
  public ResponseEntity<ErrorMessage> getSupportedVersionForOtherPaths() {

    ErrorMessage errorMessage = new ErrorMessage();
    errorMessage.setMessage("The given url is not found!");
    return new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
  }
}
