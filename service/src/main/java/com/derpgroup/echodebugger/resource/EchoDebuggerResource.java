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

import io.dropwizard.setup.Environment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SpeechletException;
import com.derpgroup.echodebugger.configuration.MainConfig;
import com.derpgroup.echodebugger.logger.EchoDebuggerLogger;
import com.derpgroup.echodebugger.model.Response;
import com.derpgroup.echodebugger.model.ResponseGroup;
import com.derpgroup.echodebugger.model.ResponseKey;
import com.derpgroup.echodebugger.model.User;
import com.derpgroup.echodebugger.model.UserDao;
import com.derpgroup.echodebugger.util.AlexaResponseUtil;
import com.derpgroup.echodebugger.util.UserSorter;
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
@Consumes({MediaType.APPLICATION_JSON,MediaType.TEXT_PLAIN})
public class EchoDebuggerResource {
	final static Logger LOG = LoggerFactory.getLogger(EchoDebuggerResource.class);

	private UserDao userDao;
	private String password;
	private Integer maxAllowedResponseLength;
	private Boolean debugMode;
	private String baseUrl;

	public EchoDebuggerResource(MainConfig config, Environment env) {
		password = config.getEchoDebuggerConfig().getPassword();
		maxAllowedResponseLength = config.getEchoDebuggerConfig().getMaxAllowedResponseLength();
		debugMode = config.getEchoDebuggerConfig().getDebugMode();
		baseUrl = config.getEchoDebuggerConfig().getBaseUrl();
	}

	@Path("/user/{userId}")
	@POST
	public Map<String, Object> saveResponseForUserId(Map<String, Object> body, @PathParam("userId") String userId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}

		boolean requestIsAllowed = debugMode || user != null;
		EchoDebuggerLogger.logSaveNewResponse(body, userId, requestIsAllowed);

		if(user == null){
			// DebugMode let's us register responses for accounts that don't exist
			if(debugMode){
				userDao.createUser(userId);
				user = userDao.getUserByEchoId(userId);
			}
			// If the request is for an ID that we haven't seen before, refuse it
			else{
				Map<String, Object> response = new HashMap<String, Object>();
				response.put("Result", "This is not a known id. Please access this skill through your Echo to automatically register your Echo and obtain an id.");
				return response;
			}
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
		if(CollectionUtils.isEmpty(user.getResponseGroups())){
			user.setResponseGroups(new ArrayList<ResponseGroup>());
		}
		ResponseKey responseKey = new ResponseKey();
		Response responseBody = new Response(body);

		// Hacky, this is currently just overwriting everything the user has
		ResponseGroup responseGroup = new ResponseGroup(responseKey, responseBody);
		user.getResponseGroups().clear();
		user.getResponseGroups().add(responseGroup);

		userDao.saveUser(user);
		return body;
	}

	// TODO: Change this endpoint to be a UI manipulatable through a webpage
	@Path("/user/{userId}")
	@GET
	public Map<String, Object> getResponseForEchoId(@PathParam("userId") String userId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}

		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);
			Map<String, Object> response = new HashMap<String, Object>();
			response.put("Result", "There is no user with the id of ("+userId+")");
			return response;
		}
		EchoDebuggerLogger.logAccessRequest(user.getEchoId(),"SINGLE_RESPONSE",true);
		user.setLastWebDownloadTime(Instant.now());
		user.setNumContentDownloads(user.getNumContentDownloads()+1);

		// Get the response
		List<ResponseGroup> responseGroups = user.getResponseGroups();
		Map<String, Object> response = null;
		if(CollectionUtils.isNotEmpty(responseGroups) &&
				responseGroups.get(0) != null &&
				CollectionUtils.isNotEmpty(responseGroups.get(0).getResponses()) &&
				responseGroups.get(0).getResponses().get(0) != null){
			response = responseGroups.get(0).getResponses().get(0).getData();
		}

		int responseLength = getLengthOfContent(response);
		user.setNumCharactersDownloaded(user.getNumCharactersDownloaded()+responseLength);
		userDao.saveUser(user);

		if(response==null){
			response = new HashMap<String, Object>();
			response.put("Result", "There are no responses stored for user ("+userId+")");
			return response;
		}
		return response;
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
		List<User> users = userDao.getAllUserData();
		Collections.sort(users, UserSorter.SORT_USER_BY_MOST_RECENT_UPLOAD);
		return users;
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
		String echoId = request.getSession().getUser().getUserId();
		User user = userDao.getUserByEchoId(echoId);
		if(user == null){
			user = userDao.createUser(echoId);
			return getIntro(user.getId().toString());
		}
		EchoDebuggerLogger.logEchoRequest(echoId,intent);

		switch(intent){
		case "AMAZON.HelpIntent":
			return getIntro(user.getId().toString());

		case "WHATISMYID":
			String title = "Echo ID";
			String content = "Your ID is "+user.getId().toString();
			String ssml = "Your ID is now printed in the Alexa app. Please check to see the exact spelling. It is case-sensitive. This ID is unique between you and this skill. If you delete the skill you will be issued a new ID when you next connect.";
			return AlexaResponseUtil.createSimpleResponse(title,content,ssml);
		case "START_OF_CONVERSATION":
		case "GETRESPONSE":
		default:
			return getUserContent(user);
		}
	}

	public UserDao getUserDao() {return userDao;}
	public void setUserDao(UserDao userDao) {this.userDao = userDao;}

	// TODO: Soon to be deprecated
	public Object getUserContent(User user){
		user.setNumContentDownloads(user.getNumContentDownloads()+1);
		user.setLastEchoDownloadTime(Instant.now());
		int contentLength = getLengthOfContent(user.getData());
		user.setNumCharactersDownloaded(user.getNumCharactersDownloaded() + contentLength);
		userDao.saveUser(user);

		List<ResponseGroup> responseGroups = user.getResponseGroups();
		if(CollectionUtils.isEmpty(responseGroups) ||
				responseGroups.get(0) == null ||
				CollectionUtils.isEmpty(responseGroups.get(0).getResponses()) ||
				responseGroups.get(0).getResponses().get(0) == null){

			return AlexaResponseUtil.createSimpleResponse("You have no saved responses","There are no saved responses for your ID ("+user.getId().toString()+")","There are no saved responses");
		}
		return responseGroups.get(0).getResponses().get(0).getData();
	}

	public Object getIntro(String userId){
		String plaintext = "Welcome to the Alexa Skills Kit Responder. This is a tool that allows you to create mock skill responses, and then play them through your Echo. "
				+ "You'll need your unique ID to upload mock responses. I have just printed it below for you. To print your ID again you can just say, \"Alexa, ask Responder, what is my ID?\" "
				+ "With that ID you can upload your mock response using an HTTP POST request.\n"
				+ "To play your response just say \"Alexa, open Responder.\" To hear these instructions again say, \"Alexa, ask Responder for help.\" For details, follow the documentation link listed below.\n"
				+ "Your Echo ID is: "+userId+"\n"
				+ "Documentation: "+baseUrl+"responder/";

		String ssml = "Welcome to the Alexa Skills Kit Responder. This is a tool that allows you to create mock skill responses, and then play them through your Echo. <break time=\"700ms\"/>"
				+ "You'll need your unique ID to upload mock responses. I have just printed it in the Alexa App for you. To print your ID again you can just say, Alexa, ask Responder, what is my ID<break time=\"700ms\"/>"
				+ "With that ID you can upload your mock response using an HTTP POST request. <break/>"
				+ "To play your response just say <break/> Alexa, open Responder<break time=\"700ms\"/>"
				+ "To hear these instructions again, say, Alexa, ask Responder for help. "
				+ "For details, follow the documentation link that I've just printed in your Alexa app.";
		return AlexaResponseUtil.createSimpleResponse("How to use the A.S.K. Responder",plaintext,ssml);
	}

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
