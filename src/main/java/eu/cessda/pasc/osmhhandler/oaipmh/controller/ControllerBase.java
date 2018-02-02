package eu.cessda.pasc.osmhhandler.oaipmh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.MESSAGE;

/**
 * Base helper class for controllers to inherit from
 *
 * @author moses@doraventures.com
 */
@Slf4j
public class ControllerBase {

  @Autowired
  ObjectMapper objectMapper;

  static ResponseEntity<String> logAndGetResponseEntityMessage(String message, HttpStatus httpStatus, Logger logger) {
    logger.error(message);
    return getResponseEntityMessage(message, httpStatus);
  }

  static ResponseEntity<String> getResponseEntityMessage(String message, HttpStatus httpStatus) {
    return getResponseEntity(getSimpleResponseMessage(message), httpStatus);
  }

  static ResponseEntity<String> getResponseEntity(String message, HttpStatus httpStatus) {
    return new ResponseEntity<>(message, httpStatus);
  }

  private static String getSimpleResponseMessage(String messageString) {
    JSONObject obj = new JSONObject();
    obj.put(MESSAGE, messageString);
    return obj.toJSONString();
  }
}
