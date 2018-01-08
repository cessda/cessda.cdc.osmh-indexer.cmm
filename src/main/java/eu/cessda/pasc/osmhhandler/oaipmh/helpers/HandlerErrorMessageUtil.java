package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import org.json.simple.JSONObject;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.HandlerConstants.MESSAGE;

/**
 * @author moses@doraventures.com
 */
public class HandlerErrorMessageUtil {

  private HandlerErrorMessageUtil() {
    // Private to keep class for static constants only
  }

  public static  String getSimpleResponseMessage(String messageString) {
    JSONObject obj = new JSONObject();
    obj.put(MESSAGE, messageString);
    return obj.toJSONString();
  }
}
