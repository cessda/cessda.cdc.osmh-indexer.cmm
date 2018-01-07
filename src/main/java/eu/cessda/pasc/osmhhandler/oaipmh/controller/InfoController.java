package eu.cessda.pasc.osmhhandler.oaipmh.controller;

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

/**
 * Controller to handle request about services, versions and metadata formats supported by this api
 *
 * @author moses@doraventures.com
 */
@RestController
@RequestMapping("/supportedversion")
@Api(value = "Operations to handle request about services, versions and metadata formats supported by this api")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InfoController {

  @RequestMapping(method = RequestMethod.GET)
  @ApiOperation(value = "supported api version",
      response = String.class,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully returns supported versions", response = ResponseEntity.class),
      @ApiResponse(code = 400, message = "Not Successful. Bad Request", response = ResponseEntity.class)
  })
  public ResponseEntity<String> getSupportedVersion() {
    return new ResponseEntity<>("{\"version\":[\"v1.0\"]}", HttpStatus.OK);
  }
}
