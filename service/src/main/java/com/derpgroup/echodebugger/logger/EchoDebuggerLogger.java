package com.derpgroup.echodebugger.logger;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EchoDebuggerLogger {
  private static final Logger LOG = LoggerFactory.getLogger(EchoDebuggerLogger.class);
  
  /**
   * Primary metrics logging function. Logs a user request and associated metadata.
   * @param serviceInput
   */
  public static void logSaveNewResponse(Map<String, Object> postBody, String echoId){
    ObjectMapper mapper = new ObjectMapper();
    String result = null;
    try {
      result = mapper.writeValueAsString(postBody);
    } catch (JsonProcessingException e) {
      result = "Could not parse";
    }
    LOG.info(echoId+",NEW_RESPONSE,"+result);
  }
  
  public static void logAccessRequest(String echoId, String message){
    LOG.info(echoId+",WEB_REQUEST,"+message);
  }
  
  public static void logEchoRequest(String echoId, String intent){
    LOG.info(echoId+",ECHO_REQUEST,"+intent);
  }
}
