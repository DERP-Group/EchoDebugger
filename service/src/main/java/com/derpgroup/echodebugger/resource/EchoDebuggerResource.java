/**
 * Copyright (C) 2015 David Phillips
 * Copyright (C) 2015 Eric Olson
 * Copyright (C) 2015 Rusty Gerard
 * Copyright (C) 2015 Paul Winters
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.derpgroup.echodebugger.resource;

import java.util.HashMap;
import java.util.Map;

import io.dropwizard.setup.Environment;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.json.SpeechletResponseEnvelope;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.SpeechletException;
import com.derpgroup.echodebugger.configuration.MainConfig;
import com.derpgroup.echodebugger.manager.EchoDebuggerManager;


/**
 * REST APIs for requests generating from Amazon Alexa
 *
 * @author David
 * @since 0.0.1
 */
@Path("/echodebugger")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EchoDebuggerResource {
  final static Logger log = LoggerFactory.getLogger(EchoDebuggerResource.class);

  private EchoDebuggerManager manager;
  
  // Temp data structure
  Map<String,Map<String, Object>> userData = new HashMap<String,Map<String, Object>>();

  public EchoDebuggerResource(MainConfig config, Environment env) {
    manager = new EchoDebuggerManager();
  }
  
  @Path("/user/{echoId}")
  @POST
  public Map<String, Object> saveResponseForEchoId(Map<String, Object> body, @PathParam("echoId") String echoId){
    log.info("saveResponseForEchoId("+echoId+"): "+body.toString());
    userData.put(echoId, body);
    return body;
  }
  
  @Path("/user/{echoId}")
  @GET
  public Map<String, Object> getResponseForEchoId(@PathParam("echoId") String echoId){
    log.info("getResponseForEchoId("+echoId+"): "+userData.get(echoId));
    if(!userData.containsKey(echoId)){
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "There are no responses stored for user ("+echoId+")");
      return response;
    }
    return userData.get(echoId);
  }
  
  @Path("/user")
  @GET
  public Object getResponsesByEchoId(@QueryParam("showAllUsers") String showAllUsers){
    if(showAllUsers==null || !showAllUsers.equals("true")){
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "To retrieve your response please use the format /echodebugger/user/{yourEchoId}");
      return response;
    }
    log.info("getResponsesByEchoId(): Knew the secret query param to see all user responses");
    return userData.entrySet();
  }

  @POST
  public Object handleEchoRequest(SpeechletRequestEnvelope request) throws SpeechletException{
    
    if (request==null || request.getRequest() == null) {
      log.info("Missing request body.");
      throw new RuntimeException("Missing request body.");
    }
    log.info("handleEchoRequest(): "+request.toString());

    String intent = "NOINTENT";
    if(request.getRequest()!=null){
      if(!(request.getRequest() instanceof IntentRequest)){
        log.info("handleEchoRequest(): Intent is not an IntentRequest: "+request.getRequest().toString());
        return new SpeechletResponseEnvelope();
      }
      IntentRequest intentRequest = (IntentRequest) request.getRequest();
      intent = intentRequest.getIntent().getName();
    }
    log.info("Intent received: "+intent);

    switch(intent){
    case "NOINTENT":
      return manager.createSimpleResponse("How to use Echo Debugger",
          "TODO: Attach information here",
          "You must load Echo responses through REST calls before you can retrieve them using your Echo. I have attached additional information in this response to help you do this.");
      
    case "GETRESPONSE":
      // might have id's in slots for groupId and bucketId
      // For now just return the only one
      
      String userId = request.getSession().getUser().getUserId();
      if(!userData.containsKey(userId)){
        log.info("There were no saved responses for user ("+userId+")");
        return manager.createSimpleResponse("You have no saved responses","There are no saved responses for your Echo ID ("+userId+")","There are no saved responses");
      }
      log.info("Returning saved response for user ("+userId+"):\n"+userData.get(userId).toString());
      return userData.get(userId);
      
    case "WHATISMYID":
      log.info("Query for Echo ID by: ("+request.getSession().getUser().getUserId()+")");
      String echoResponse = "Your Echo ID is "+request.getSession().getUser().getUserId();
      return manager.createSimpleResponse(echoResponse);
      
    default:
      log.info("Encountered unexpected intent type! "+intent);
      throw new SpeechletException("Invalid Intent");
    }
  }
}
