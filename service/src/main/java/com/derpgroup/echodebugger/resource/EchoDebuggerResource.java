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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SpeechletException;
import com.derpgroup.echodebugger.configuration.MainConfig;
import com.derpgroup.echodebugger.exceptions.ExceptionType;
import com.derpgroup.echodebugger.exceptions.ResponderException;
import com.derpgroup.echodebugger.logger.EchoDebuggerLogger;
import com.derpgroup.echodebugger.model.IntentResponses;
import com.derpgroup.echodebugger.model.ResponseKey;
import com.derpgroup.echodebugger.model.User;
import com.derpgroup.echodebugger.model.UserDao;
import com.derpgroup.echodebugger.util.AlexaResponseUtil;
import com.derpgroup.echodebugger.util.ResponderUtils;
import com.derpgroup.echodebugger.util.UserSorter;


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
	final static Set<String> RESERVED_PARAM_NAMES = new HashSet<>(Arrays.asList("intent","append","state"));

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

	// Deprecate this, we want to move users away from it
	@Path("/user/{userId}")
	@POST
	public Map<String, Object> saveResponseForUserId_old(
			Map<String, Object> body,
			@PathParam("userId") String userId
			){
		LOG.info(userId+" is still using legacy POST /user/{userId}");
		saveResponseForUserId_default(body, userId);
		Map<String, Object> response = new HashMap<>();
		response.put("Warning", "This endpoint is deprecated and will be removed October 31st 2016. Responder has recently changed the API to use plural resource identifiers. Please use /users/{userId} instead.");
		return response;
	}

	/**
	 * This is a default endpoint for saving responses. It saves responses under the intent "GETRESPONSE".
	 * @param body
	 * @param userId
	 * @return
	 */
	@Path("/users/{userId}")
	@POST
	public Map<String, Object> saveResponseForUserId_default(
			Map<String, Object> body,
			@PathParam("userId") String userId
			){
		return saveResponseForUserId(body, userId, "GETRESPONSE");
	}

	/**
	 * This is the primary endpoint used for saving responses
	 * @param body
	 * @param userId
	 * @param intentName
	 * @return
	 */
	@Path("/users/{userId}/intents/{intentName}")
	@POST
	public Map<String, Object> saveResponseForUserId(
			Map<String, Object> body,
			@PathParam("userId") String userId,
			@PathParam("intentName") String intentName){

		if(StringUtils.isEmpty(userId)){
			throw new ResponderException("A userId is required for this endpoint.", ExceptionType.UNRECOGNIZED_ID);
		}
		if(StringUtils.isEmpty(intentName)){
			throw new ResponderException("An intent name is required for this endpoint.", ExceptionType.UNRECOGNIZED_ID);
		}

		User user = (userDao.getUserById(userId)!=null) ? userDao.getUserById(userId) : userDao.getUserByEchoId(userId);

		boolean requestIsAllowed = debugMode || user != null;
		EchoDebuggerLogger.logSaveNewResponse(body, userId, requestIsAllowed);	// TODO: Update this to store the intentName

		if(user == null){
			// DebugMode let's us register responses for accounts that don't exist
			if(debugMode){
				userDao.createUser(userId);
				user = userDao.getUserByEchoId(userId);
			}
			// If the request is for an ID that we haven't seen before, refuse it
			else{
				throw new ResponderException("This is not a known id. Please access this skill through your Echo to automatically register your Echo and obtain an id.", ExceptionType.UNRECOGNIZED_ID);
			}
		}

		user.setLastUploadTime(Instant.now());
		user.setNumContentUploads(user.getNumContentUploads()+1);

		// Abort storing it if the request is too long
		int responseLength = ResponderUtils.getLengthOfContent(body);
		user.setNumCharactersUploaded(user.getNumCharactersUploaded()+responseLength);
		if(responseLength > maxAllowedResponseLength){
			user.setNumUploadsTooLarge(user.getNumUploadsTooLarge()+1);
			userDao.saveUser(user);
			throw new ResponderException("The response is too long. Alexa limits response sizes to 8000 characters."
					+ "This response was "+responseLength+" characters long. Please see their restrictions here: "
					+ "https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#Response%20Format",
					ExceptionType.RESPONSE_TOO_LONG);
		}

		IntentResponses intentResponses = user.getIntents().get(intentName);
		if(intentResponses == null){
			intentResponses = new IntentResponses();
			intentResponses.setIntentName(intentName);
		}
		intentResponses.setData(body);
		user.getIntents().put(intentName, intentResponses);

		userDao.saveUser(user);

		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("Response path","/responder/users/"+userId+"/intents/"+intentName);
		return responseMap;
	}

	// TODO: Remove this endpoint after people stop using it
	@Path("/user/{userId}")
	@GET
	public Map<String, Object> getDefaultResponseByUserId_legacy(@PathParam("userId") String userId){
		LOG.info(userId+" is still using legacy GET /user/{userId}");
		getDefaultResponseByUserId(userId);
		Map<String, Object> response = new HashMap<>();
		response.put("Warning", "This endpoint is deprecated and will be removed October 31st 2016. Responder has recently changed the API to use plural resource identifiers. Please use /users/{userId} instead.");
		return response;
	}

	// TODO: Change this endpoint to be a UI in a webpage that lets people manually edit their entries
	@Path("/users/{userId}")
	@GET
	public Map<String, Object> getDefaultResponseByUserId(@PathParam("userId") String userId){

		User user = (userDao.getUserById(userId)!=null) ? userDao.getUserById(userId) : userDao.getUserByEchoId(userId);
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}
		EchoDebuggerLogger.logAccessRequest(user.getEchoId(),"SINGLE_RESPONSE",true);
		user.setLastWebDownloadTime(Instant.now());
		user.setNumContentDownloads(user.getNumContentDownloads()+1);

		// Get the response
		Map<String, Object> response = null;
		Map<String, IntentResponses> intentResponses = user.getIntents();
		if(MapUtils.isNotEmpty(intentResponses) &&
				intentResponses.containsKey("GETRESPONSE")){
			response = intentResponses.get("GETRESPONSE").getData();
		}

		int responseLength = ResponderUtils.getLengthOfContent(response);
		user.setNumCharactersDownloaded(user.getNumCharactersDownloaded()+responseLength);
		userDao.saveUser(user);

		if(response==null){
			throw new ResponderException("There are no responses stored for user ("+userId+")", ExceptionType.NO_SAVED_RESPONSE);
		}
		return response;
	}

	@Path("/users/{userId}/intents")
	@GET
	public Map<String, Object> getResponsesForUser(
			@PathParam("userId") String userId){
		User user = (userDao.getUserById(userId)!=null) ? userDao.getUserById(userId) : userDao.getUserByEchoId(userId);
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"ALL_INTENTS",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		// TODO: Build a presentation-layer version of this object instead of returning the actual object
		return user.getIntents().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
	}

	@Path("/users/{userId}/intents/{intentName}")
	@GET
	public Object getResponsesForUser(
			@PathParam("userId") String userId,
			@PathParam("intentName") String intentName){
		User user = (userDao.getUserById(userId)!=null) ? userDao.getUserById(userId) : userDao.getUserByEchoId(userId);
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"ALL_INTENTS",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		// TODO: Build a presentation-layer version of this object instead of returning the actual object
		IntentResponses intentResponses = user.getIntents().get(intentName);
		return intentResponses;
	}

	@Path("/users")
	@GET
	public Object getAllResponses(@QueryParam("p") String p){
		if(p==null || !p.equals(password)){
			EchoDebuggerLogger.logAccessRequest("ROOT","ALL_RESPONSES,p="+p,false);
			Map<String, Object> response = new HashMap<String, Object>();
			response.put("Result", "To retrieve your response please use the format /responder/users/{yourId}");
			return response;
		}
		EchoDebuggerLogger.logAccessRequest("ROOT","ALL_RESPONSES,p="+p,true);
		List<User> users = userDao.getAllUserData();
		Collections.sort(users, UserSorter.SORT_USER_BY_MOST_RECENT_UPLOAD);
		return users;
	}

	// Deprecate this
	@Path("/user")
	@GET
	public Object getAllResponses_old(@QueryParam("p") String p){
		LOG.info("Legacy request for GET /user");
		Map<String,String> response = new HashMap<>();
		response.put("Result", "This endpoint is deprecated and will be removed October 31st 2016. Responder has recently changed the API to use plural resource identifiers. Please use /users instead.");
		return response;
	}

	@Path("/users/{userId}")
	@DELETE
	public Object deleteUserById(
			@PathParam("userId") String userId,
			@QueryParam("p") String p){

		if(p==null || !p.equals(password)){
			EchoDebuggerLogger.logAccessRequest("ROOT","DELETE_USER,p="+p,false);
			Map<String, Object> response = new HashMap<String, Object>();
			response.put("Result", "You must be an admin to delete a user");
			return response;
		}
		EchoDebuggerLogger.logAccessRequest("ROOT","DELETE_USER,p="+p,true);

		User user = (userDao.getUserById(userId)!=null) ? userDao.getUserById(userId) : userDao.getUserByEchoId(userId);
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"DELETE_USER",false);
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		return userDao.deleteUser(user);
	}

	@Path("/users/{userId}/intents/{intentName}")
	@DELETE
	public Object deleteIntent(
			@PathParam("userId") String userId,
			@PathParam("intentName") String intentName){

		User user = (userDao.getUserById(userId)!=null) ? userDao.getUserById(userId) : userDao.getUserByEchoId(userId);
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"DELETE_INTENT",false);
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}
		IntentResponses intentResponses = userDao.deleteIntent(user, intentName);
		if(intentResponses == null){
			throw new ResponderException("There is no intent with the id of ("+intentName+") registered for user ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}
		else {
			return intentResponses;
		}
	}

	/**
	 * This is the primary entry point for Echo requests made by Amazon.
	 * Anything that returns from here is what gets played through your Echo.
	 * @param request
	 * @return
	 * @throws SpeechletException
	 */
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

		Map<String, String> slots = ResponderUtils.getMessageAsMap(request.getRequest());

		// If the user has intents registered to override default Responder intents, then use them
		Set<String> registeredIntents = user.getIntents().keySet();
		if(registeredIntents.contains(intent)){
			return getUserContent(user, intent, null, slots);
		}

		// Else the user inherits some default intent processing from Responder
		switch(intent){
		case "AMAZON.HelpIntent":
			return getIntro(user.getId().toString());
		case "WHATISMYID":
			String title = "Echo ID";
			String content = "Your ID is "+user.getId().toString();
			String ssml = "Your ID is now printed in the Alexa app. Please check to see the exact spelling. It is case-sensitive. This ID is unique between you and this skill. If you delete the skill you will be issued a new ID when you next connect.";
			return AlexaResponseUtil.createSimpleResponse(title,content,ssml);
		case "AMAZON.StopIntent":
			return AlexaResponseUtil.createSimpleResponse(null,null,null);
		case "AMAZON.CancelIntent":
			return AlexaResponseUtil.createSimpleResponse(null,null,null);
		case "START_OF_CONVERSATION":
			intent = "GETRESPONSE";
			return getUserContent(user, intent, null, slots);
		case "GETRESPONSE":
		default:
			return getUserContent(user, intent, null, slots);
		}
	}

	public UserDao getUserDao() {return userDao;}
	public void setUserDao(UserDao userDao) {this.userDao = userDao;}

	public Object getUserContent(User user, String intent, String state, Map<String,String> slots){

		if(MapUtils.isEmpty(slots)){slots = null;}

		Object response = null;
		IntentResponses intentResponses = user.getIntents().get(intent);
		if(intentResponses==null || MapUtils.isEmpty(intentResponses.getData())){
			ResponseKey responseKey = new ResponseKey(intent, slots, state);
			String serializedResponseKey = ResponderUtils.serialize(responseKey);
			response = AlexaResponseUtil.createSimpleResponse("There is no response for this input","There is no response for this input\n"+serializedResponseKey,"There is no response for this input");
		}
		else {
			response = intentResponses.getData();
		}

		// Update statistics for the user
		user.setNumContentDownloads(user.getNumContentDownloads()+1);
		user.setLastEchoDownloadTime(Instant.now());
		int contentLength = ResponderUtils.getLengthOfContent(response);
		user.setNumCharactersDownloaded(user.getNumCharactersDownloaded() + contentLength);
		userDao.saveUser(user);

		return response;
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
}
