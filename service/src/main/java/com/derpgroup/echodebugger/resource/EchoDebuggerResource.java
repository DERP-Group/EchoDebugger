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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
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
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SpeechletException;
import com.derpgroup.echodebugger.configuration.MainConfig;
import com.derpgroup.echodebugger.logger.EchoDebuggerLogger;
import com.derpgroup.echodebugger.model.User;
import com.derpgroup.echodebugger.model.UserDao;
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
  final static Logger LOG = LoggerFactory.getLogger(EchoDebuggerResource.class);

  private UserDao userDao;
  private String password;
  private Integer maxAllowedResponseLength;
  private Boolean debugMode;
  private String baseUrl;
  private String introPage;

  public EchoDebuggerResource(MainConfig config, Environment env) {
    password = config.getEchoDebuggerConfig().getPassword();
    maxAllowedResponseLength = config.getEchoDebuggerConfig().getMaxAllowedResponseLength();
    debugMode = config.getEchoDebuggerConfig().getDebugMode();
    baseUrl = config.getEchoDebuggerConfig().getBaseUrl();
    introPage = config.getEchoDebuggerConfig().getIntroPage();
  }
  
  @Produces(MediaType.TEXT_HTML)
  @GET
  public String getRootRequest(){
    String content;
    try {
      content = new String(Files.readAllBytes(Paths.get(introPage)),Charset.defaultCharset());
    } catch (IOException e) {
      LOG.error("There was a problem serving the intro page",e);
      content = "There was a problem. Please refer to the blog posts at www.derpgroup.com for more information.";
    }
    return content;
  }
  
  @Path("/user/{echoId}")
  @POST
  public Map<String, Object> saveResponseForEchoId(Map<String, Object> body, @PathParam("echoId") String echoId){
    User user = userDao.getUser(echoId);
    
    // If the request is for an ID that we haven't seen before, refuse it
    if(user == null){
      if(debugMode){
        EchoDebuggerLogger.logSaveNewResponse(body, echoId, true);
        userDao.createUser(echoId);
        user = userDao.getUser(echoId);
      }
      else{
        EchoDebuggerLogger.logSaveNewResponse(body, echoId, false);
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("Result", "This is not a known Echo ID. Please access this skill through your Echo to automatically register your Echo's ID.");
        return response;
      }
    }
    else{
      EchoDebuggerLogger.logSaveNewResponse(body, echoId, true);
    }
    
    user.setLastUploadTime(Instant.now());
    user.setNumContentUploads(user.getNumContentUploads()+1);
    
    // Abort storing it if the request is too long
    int responseLength = getLengthOfContent(body);
    user.setNumCharactersUploaded(user.getNumCharactersUploaded()+responseLength);
    if(responseLength > maxAllowedResponseLength){
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "The response is too long. Alexa limits response sizes to 8000 characters. This response was "+responseLength+" characters long. Please see their restrictions here: https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#Response%20Format");
      user.setNumUploadsTooLarge(user.getNumUploadsTooLarge()+1);
      userDao.saveUser(user);
      return response;
    }
    user.setData(body);
    userDao.saveUser(user);
    return body;
  }
  
  @Path("/user/{echoId}")
  @GET
  public Map<String, Object> getResponseForEchoId(@PathParam("echoId") String echoId){
    User user = userDao.getUser(echoId);
    if(user==null){
      EchoDebuggerLogger.logAccessRequest(echoId,"SINGLE_RESPONSE",false);
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "There is no user with the id of ("+echoId+")");
      return response;
    }
    EchoDebuggerLogger.logAccessRequest(echoId,"SINGLE_RESPONSE",true);
    user.setLastWebDownloadTime(Instant.now());
    user.setNumContentDownloads(user.getNumContentDownloads()+1);
    int responseLength = getLengthOfContent(user.getData());
    user.setNumCharactersDownloaded(user.getNumCharactersDownloaded()+responseLength);
    userDao.saveUser(user);
    
    if(user.getData()==null){
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "There are no responses stored for user ("+echoId+")");
      return response;
    }
    
    return user.getData();
  }
  
  @Path("/user")
  @GET
  public Object getAllResponses(@QueryParam("p") String p){
    if(p==null || !p.equals(password)){
      EchoDebuggerLogger.logAccessRequest("ROOT","ALL_RESPONSES,p="+p,false);
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("Result", "To retrieve your response please use the format /responder/user/{yourEchoId}");
      return response;
    }
    EchoDebuggerLogger.logAccessRequest("ROOT","ALL_RESPONSES,p="+p,true);
    return userDao.getAllUserData();
  }

  @POST
  public Object handleEchoRequest(SpeechletRequestEnvelope request) throws SpeechletException{

    if (request==null || request.getRequest() == null) {
      throw new SpeechletException("Invalid Request format");
    }

    String intent = "UNKNOWN_INTENT";
    if(request.getRequest()!=null && request.getRequest() instanceof IntentRequest){
      IntentRequest intentRequest = (IntentRequest) request.getRequest();
      intent = intentRequest.getIntent().getName();
    }
    if(request.getRequest() instanceof LaunchRequest){
      intent = "START_OF_CONVERSATION";
    }
    else if(request.getRequest() instanceof SessionEndedRequest){
      intent = "END_OF_CONVERSATION";
    }

    // If the user doesn't exist, create them
    String userId = request.getSession().getUser().getUserId();
    if(!userDao.containsUser(userId)){
      userDao.createUser(userId);
    }
    User user = userDao.getUser(userId);
    EchoDebuggerLogger.logEchoRequest(userId,intent);

    switch(intent){
    case "START_OF_CONVERSATION":
      String noIntentContent = "Welcome to the Alexa Skills Kit Responder (A.S.K Responder). This is a basic tool that allows you to create mock skill responses, and then play them through your Echo. "
          + "To use this tool, there are four things you need to do:\n"
          
          + "1.) Set up the A.S.K. Responder skill\n"
          + " You've already done this - else you wouldn't be seeing this. Please verify that your Intent Schema and Sample Utterances match the expected values. See this webpage for further information: "+baseUrl+"responder/\n"
          
          + "2.) Get your Echo ID\n"
          + " Your Echo ID is '"+userId+"'. You will upload your mock skill responses using this ID. If at any time you forget your Echo ID you can ask, 'What is my ID?'\n"
          
          + "3.) Upload your mock skill response\n"
          + " You can register your mock response by doing an HTTP POST request to:\n"+baseUrl+"responder/user/"+userId+"/\n"
          + " Please note: The Responder is expecting the request body to be a JSON payload, and it expects the content header 'Content-Type' to be 'application/json'. Set this value in your POST request else it won't work.\n"
          
          + "4.) Play your response\n"
          + "Play your mock skill response through the A.S.K. Responder itself (this skill), by saying: 'Play my response'\n"
          + "For more detailed information on usage please visit "+baseUrl+"responder/";

      String noIntentSsml = "Welcome to the Alexa Skills Kit Responder. This is a basic tool that allows you to create mock skill responses, and then play them through your Echo. "
          + "To use this tool, there are four things you need to do. <break time=\"700ms\"/>"
          + "The first thing you must do is set up this skill on your Echo - which you've already done.<break/>"
          + "The second thing you must do, is get your Echo ID. You will upload your mock skill responses using this ID. Your ID is '"+userId+"'. Please refer to the Alexa app to see it. If at any time you forget your Echo ID you can ask me, What is my ID<break time=\"700ms\"/>"
          + "Which brings us to the third thing. With that Echo ID you can now do an HTTP POST request to register your mock response. <break/>"
          + "And lastly, you can then play your mock response through this skill itself, by saying: Play my response. <break time=\"700ms\"/>"
          + "Please read the additional information I've just printed in your Alexa app to help you do this.";
      return AlexaResponseUtil.createSimpleResponse("How to use the A.S.K. Responder",noIntentContent,noIntentSsml);

    case "GETRESPONSE":
      user.setNumContentDownloads(user.getNumContentDownloads()+1);
      user.setLastEchoDownloadTime(Instant.now());
      int contentLength = getLengthOfContent(user.getData());
      user.setNumCharactersDownloaded(user.getNumCharactersDownloaded() + contentLength);
      userDao.saveUser(user);
      
      if(user.getData()==null){
        return AlexaResponseUtil.createSimpleResponse("You have no saved responses","There are no saved responses for your Echo ID ("+userId+")","There are no saved responses");
      }
      return user.getData();
      
    case "WHATISMYID":
      String title = "Echo ID";
      String content = "Your Echo ID is "+request.getSession().getUser().getUserId();
      String ssml = "Your Echo ID is "+request.getSession().getUser().getUserId()+" <break /> Please check your Alexa app to see the exact spelling. It is case-sensitive. This ID is unique between you and this skill. If you delete the skill you will be issued a new ID when you next connect.";
      return AlexaResponseUtil.createSimpleResponse(title,content,ssml);
      
    default:
      return new SpeechletResponseEnvelope();
    }
  }

  public UserDao getUserDao() {return userDao;}
  public void setUserDao(UserDao userDao) {this.userDao = userDao;}

  public int getLengthOfContent(Map<String, Object> content){
    if(content == null){return 0;}
    try {
      ObjectMapper mapper = new ObjectMapper();
      String serializedBody = mapper.writeValueAsString(content);
      return serializedBody.length();
    } catch (JsonProcessingException e) {
      return 0;
    }
  }
}
