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
import com.derpgroup.echodebugger.logger.EchoDebuggerLogger;
import com.derpgroup.echodebugger.model.ContentDao;
import com.derpgroup.echodebugger.util.AlexaResponseUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * REST APIs for requests generating from Amazon Alexa
 *
 * @author David
 * @since 0.0.1
 */
@Path("/responder")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EchoDebuggerResource {
  final static Logger log = LoggerFactory.getLogger(EchoDebuggerResource.class);

  private ContentDao contentDao;
  private String password;
  private Integer maxAllowedResponseLength;
  private Boolean debugMode;

  public EchoDebuggerResource(MainConfig config, Environment env) {
    password = config.getEchoDebuggerConfig().getPassword();
    maxAllowedResponseLength = config.getEchoDebuggerConfig().getMaxAllowedResponseLength();
    debugMode = config.getEchoDebuggerConfig().getDebugMode();
  }
  
  @Path("/user/{echoId}")
  @POST
  public Map<String, Object> saveResponseForEchoId(Map<String, Object> body, @PathParam("echoId") String echoId){
    EchoDebuggerLogger.logSaveNewResponse(body, echoId);
    
    ObjectMapper mapper = new ObjectMapper();
    String serializedBody = null;
    try {
      serializedBody = mapper.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      serializedBody = "Failed to save response";
    }
    
    // Abort storing it if the request is too long
    int responseLength = serializedBody.length();
    if(responseLength > maxAllowedResponseLength){
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "The response is too long. Alexa limits response sizes to 8000 characters. This response was "+responseLength+" characters long. Please see their restrictions here: https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#Response%20Format");
      return response;
    }
    
    // If the request is for an ID that we haven't seen before, refuse it
    if(debugMode || !contentDao.containsUser(echoId)){
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "The response is too long. Alexa limits response sizes to 8000 characters. This response was "+responseLength+" characters long. Please see their restrictions here: https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#Response%20Format");
      return response;
    }
    contentDao.saveUserData(echoId, body);
    return body;
  }
  
  @Path("/user/{echoId}")
  @GET
  public Map<String, Object> getResponseForEchoId(@PathParam("echoId") String echoId){
    EchoDebuggerLogger.logAccessRequest(echoId,"SINGLE_RESPONSE");
    if(!contentDao.containsUser(echoId)){
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "There are no responses stored for user ("+echoId+")");
      return response;
    }
    return contentDao.getUserContent(echoId);
  }
  
  @Path("/user")
  @GET
  public Object getAllResponses(@QueryParam("p") String p){
    EchoDebuggerLogger.logAccessRequest("ROOT","ALL_RESPONSES,p="+p);
    if(p==null || !p.equals(password)){
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "To retrieve your response please use the format /responder/user/{yourEchoId}");
      return response;
    }
    return contentDao.getUserData().entrySet();
  }

  @POST
  public Object handleEchoRequest(SpeechletRequestEnvelope request) throws SpeechletException{
    
    if (request==null || request.getRequest() == null) {
      return new SpeechletResponseEnvelope(); // TODO: Give some error
    }

    String intent = "NOINTENT";
    if(request.getRequest()!=null){
      if(!(request.getRequest() instanceof IntentRequest)){
        log.info("handleEchoRequest(): Intent is not an IntentRequest: "+request.getRequest().toString());
        return new SpeechletResponseEnvelope();
      }
      IntentRequest intentRequest = (IntentRequest) request.getRequest();
      intent = intentRequest.getIntent().getName();
    }
    
    String userId = request.getSession().getUser().getUserId();
    EchoDebuggerLogger.logEchoRequest(userId,intent);

    switch(intent){
    case "NOINTENT":
      return AlexaResponseUtil.createSimpleResponse("How to use Echo Debugger",
          "TODO: Attach information here",// TODO: Here
          "You must upload your desired Echo response before you can retrieve them using your Echo. I have attached additional information in this response to help you do this.");
      
    case "GETRESPONSE":
      if(!contentDao.containsUser(userId)){
        return AlexaResponseUtil.createSimpleResponse("You have no saved responses","There are no saved responses for your Echo ID ("+userId+")","There are no saved responses");
      }
      return contentDao.getUserContent(userId);
      
    case "WHATISMYID":
      String title = "Echo ID";
      String content = "Your Echo ID is "+request.getSession().getUser().getUserId();
      String ssml = "Your Echo ID is "+request.getSession().getUser().getUserId()+" <break /> I hope you got that. Because I won't repeat it. <break /> "
          + "I'm just kidding. Please check your Alexa app to see the exact spelling. It is case-sensitive. This ID is unique between you and this skill. If you delete the skill you will be issued a new ID when you next connect.";
      return AlexaResponseUtil.createSimpleResponse(title,content,ssml);
      
    default:
      throw new SpeechletException("Invalid Intent");
    }
  }

  public ContentDao getContentDao() {return contentDao;}
  public void setContentDao(ContentDao contentDao) {this.contentDao = contentDao;}
}
