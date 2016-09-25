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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections.CollectionUtils;
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
import com.derpgroup.echodebugger.model.Response;
import com.derpgroup.echodebugger.model.ResponseGroup;
import com.derpgroup.echodebugger.model.ResponseGroupSummary;
import com.derpgroup.echodebugger.model.ResponseKey;
import com.derpgroup.echodebugger.model.User;
import com.derpgroup.echodebugger.model.UserDao;
import com.derpgroup.echodebugger.util.AlexaResponseUtil;
import com.derpgroup.echodebugger.util.ResponderUtils;
import com.derpgroup.echodebugger.util.ResponseGroupUtils;
import com.derpgroup.echodebugger.util.UserSorter;
import com.derpgroup.echodebugger.util.UserUtils;


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

	/**
	 * Validates the query params by looking for repeats
	 * @param queryParams
	 */
	public void validateQueryParams(MultivaluedMap<String, String> queryParams){
		if(MapUtils.isEmpty(queryParams)){return;}

		for(Entry<String,List<String>> entry : queryParams.entrySet()){
			extractParamValue(queryParams, entry.getKey());
		}
	}

	/**
	 * The multi value map contains a list of values for each key. That list should only
	 * be one item long. This extracts that item.
	 * @param queryParams
	 * @param key
	 * @return
	 */
	public String extractParamValue(MultivaluedMap<String, String> queryParams, String key){
		if(MapUtils.isEmpty(queryParams) || StringUtils.isEmpty(key)){return null;}
		List<String> values = queryParams.get(key);
		if(CollectionUtils.isEmpty(values)){return null;}
		if(values.size() != 1){
			throw new ResponderException("Repeated query parameters are not allowed. The repeated parameter is ("+key+")", ExceptionType.REPEAT_QUERY_PARAMETER);
		}
		return values.get(0);
	}

	/**
	 * Extracts a map of non-reserved query params. This is used to obtain a map
	 * of all the custom slots specified in the request.
	 * @param queryParams
	 * @return
	 */
	public Map<String,String> extractSlots(MultivaluedMap<String, String> queryParams){
		if(MapUtils.isEmpty(queryParams)){return null;}

		Map<String,String> paramMap = new HashMap<>();
		for(Entry<String,List<String>> entry : queryParams.entrySet()){
			if(!RESERVED_PARAM_NAMES.contains(entry.getKey())){
				paramMap.put(entry.getKey(), extractParamValue(queryParams, entry.getKey()));
			}
		}
		if(MapUtils.isEmpty(paramMap)){return null;}
		return paramMap;
	}

	@Path("/users/{userId}/responses/{responseGroupId}/{responseId}")
	@POST
	public Map<String, Object> saveResponseByResponseId(
			Map<String, Object> body,
			@PathParam("userId") String userId,
			@PathParam("responseGroupId") Integer responseGroupId,
			@PathParam("responseId") Integer responseId
			){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}
		boolean requestIsAllowed = user != null;
		EchoDebuggerLogger.logSaveNewResponse(body, userId, requestIsAllowed);	// TODO: Update this to store query params
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
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

		ResponseGroup responseGroup = UserUtils.getResponseGroup(user, responseGroupId);
		if(responseGroup == null){
			throw new ResponderException("There is no saved group with the id of ("+responseGroupId+") for user ("+userId+")", ExceptionType.NO_SAVED_RESPONSE);
		}

		Response response = ResponseGroupUtils.getResponse(responseGroup, responseId);
		if(response == null){
			throw new ResponderException("There is no saved response with the id of ("+responseId+") in group ("+responseGroupId+") for user ("+userId+")", ExceptionType.NO_SAVED_RESPONSE);
		}

		response.setData(body);
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("Response path","/responder/users/"+userId+"/responses/"+responseGroup.getId()+"/"+response.getId().toString());
		return responseMap;
	}

	/**
	 * This is the primary endpoint used for saving responses
	 * @param body
	 * @param userId
	 * @return
	 */
	@Path("/users/{userId}")
	@POST
	public Map<String, Object> saveResponseForUserId(
			Map<String, Object> body,
			@PathParam("userId") String userId,
			@Context UriInfo uriInfo
			){
		MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
		validateQueryParams(queryParams);

		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}

		boolean requestIsAllowed = debugMode || user != null;
		EchoDebuggerLogger.logSaveNewResponse(body, userId, requestIsAllowed);	// TODO: Update this to store query params

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

		// Save the response to the User object
		boolean shouldAppendResponse = Boolean.parseBoolean(extractParamValue(queryParams, "append"));
		String intent = extractParamValue(queryParams, "intent");
		if(StringUtils.isEmpty(intent)){intent = ResponseKey.DEFAULT_RESPONDER_INTENT;}
		String state = extractParamValue(queryParams, "state");
		Map<String, String> slots = extractSlots(queryParams);

		ResponseKey responseKey = new ResponseKey(intent, slots, state);
		Response response = new Response(body);

		UserUtils.saveResponse(user, responseKey, response, shouldAppendResponse);
		userDao.saveUser(user);

		ResponseGroup responseGroup = UserUtils.getResponseGroupByResponseKey(user, responseKey);

		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("Response path","/responder/users/"+userId+"/responses/"+responseGroup.getId()+"/"+response.getId().toString());
		return responseMap;
	}

	// Deprecate this, we want to move users away from it
	@Path("/user/{userId}")
	@POST
	public Map<String, Object> saveResponseForUserId_old(
			Map<String, Object> body,
			@PathParam("userId") String userId,
			@Context UriInfo uriInfo){
		return saveResponseForUserId(body, userId, uriInfo);
	}

	// TODO: Remove this endpoint after people stop using it
	@Path("/user/{userId}")
	@GET
	public Map<String, Object> getDefaultResponseByUserId_old(@PathParam("userId") String userId){
		return getDefaultResponseByUserId(userId);
	}

	// TODO: Change this endpoint to be a UI in a webpage that lets people manually edit their entries
	@Path("/users/{userId}")
	@GET
	public Map<String, Object> getDefaultResponseByUserId(@PathParam("userId") String userId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}

		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
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

		int responseLength = ResponderUtils.getLengthOfContent(response);
		user.setNumCharactersDownloaded(user.getNumCharactersDownloaded()+responseLength);
		userDao.saveUser(user);

		if(response==null){
			throw new ResponderException("There are no responses stored for user ("+userId+")", ExceptionType.NO_SAVED_RESPONSE);
		}
		return response;
	}

	@Path("/users/{userId}/responses")
	@GET
	public Map<String, Object> getResponsesForUser(
			@PathParam("userId") String userId,
			@PathParam("responseGroupId") Integer responseGroupId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		// Accumulate the ResponseGroups
		List<ResponseGroupSummary> responseGroupSummaries = new ArrayList<>();
		List<ResponseGroup> responseGroups = user.getResponseGroups();
		if(CollectionUtils.isNotEmpty(responseGroups)){
			for(ResponseGroup responseGroup : responseGroups){
				responseGroupSummaries.add(ResponseGroupUtils.buildResponseGroupSummary(user, responseGroup));
			}
		}

		Map<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("Description","This is a list of all responses saved for this user");
		responseMap.put("ResponseGroups", responseGroupSummaries);
		return responseMap;
	}

	@Path("/users/{userId}/responses/{responseGroupId}")
	@GET
	public Map<String, Object> getResponseById(
			@PathParam("userId") String userId,
			@PathParam("responseGroupId") Integer responseGroupId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		ResponseGroup responseGroup = UserUtils.getResponseGroup(user, responseGroupId);
		if(responseGroup == null){
			throw new ResponderException("There is no saved response group with the id of ("+responseGroupId+") for user ("+userId+")", ExceptionType.NO_SAVED_RESPONSE);
		}

		Map<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("Description","This is a list of all responses saved for these input parameters");
		responseMap.put("ResponseGroup", ResponseGroupUtils.buildResponseGroupSummary(user, responseGroup));
		return responseMap;
	}

	@Path("/users/{userId}/responses/{responseGroupId}/{responseId}")
	@GET
	public Map<String, Object> getResponseById(
			@PathParam("userId") String userId,
			@PathParam("responseGroupId") Integer responseGroupId,
			@PathParam("responseId") Integer responseId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		ResponseGroup responseGroup = UserUtils.getResponseGroup(user, responseGroupId);
		if(responseGroup == null){
			throw new ResponderException("There is no saved group with the id of ("+responseGroupId+") for user ("+userId+")", ExceptionType.NO_SAVED_RESPONSE);
		}

		Response response = ResponseGroupUtils.getResponse(responseGroup, responseId);
		if(response == null){
			throw new ResponderException("There is no saved response with the id of ("+responseId+") in group ("+responseGroupId+") for user ("+userId+")", ExceptionType.NO_SAVED_RESPONSE);
		}

		return response.getData();
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
		return getAllResponses(p);
	}

	@Path("/users/{userId}")
	@DELETE
	public Map<String, Object> deleteUserById(
			@PathParam("userId") String userId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		// TODO: Implement this
		return null;
	}

	@Path("/users/{userId}/responses/{responseGroupId}")
	@DELETE
	public Map<String, Object> deleteResponseGroupById(
			@PathParam("userId") String userId,
			@PathParam("responseGroupId") Integer responseGroupId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		// TODO: Implement this
		return null;
	}

	@Path("/users/{userId}/responses/{responseGroupId}/{responseId}")
	@DELETE
	public Map<String, Object> deleteResponseById(
			@PathParam("userId") String userId,
			@PathParam("responseGroupId") Integer responseGroupId,
			@PathParam("responseId") Integer responseId){
		User user = userDao.getUserById(userId);

		// If we didn't find the user by "id", then they may be a legacy user registered by "echoId"
		if(user == null){
			user = userDao.getUserByEchoId(userId);
		}
		if(user==null){
			EchoDebuggerLogger.logAccessRequest(userId,"SINGLE_RESPONSE",false);	// TODO: Upgrade this
			throw new ResponderException("There is no user with the id of ("+userId+")", ExceptionType.UNRECOGNIZED_ID);
		}

		// TODO: Implement this
		return null;
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

		// If the user has intents registered to override default Responder intents, then use them
		Set<String> registeredIntents = UserUtils.getRegisteredIntents(user);

		Map<String, String> slots = ResponderUtils.getMessageAsMap(request.getRequest());

		switch(intent){
		case "AMAZON.HelpIntent":
			if(!registeredIntents.contains("AMAZON.HelpIntent")){
				return getIntro(user.getId().toString());
			}
			return getUserContent(user, intent, null, slots);
		case "WHATISMYID":
			String title = "Echo ID";
			String content = "Your ID is "+user.getId().toString();
			String ssml = "Your ID is now printed in the Alexa app. Please check to see the exact spelling. It is case-sensitive. This ID is unique between you and this skill. If you delete the skill you will be issued a new ID when you next connect.";
			return AlexaResponseUtil.createSimpleResponse(title,content,ssml);
		case "START_OF_CONVERSATION":
			if(!registeredIntents.contains("START_OF_CONVERSATION")){
				intent = "GETRESPONSE";
			}
			return getUserContent(user, intent, null, slots);
		case "GETRESPONSE":
		default:
			return getUserContent(user, intent, null, slots);
		}
	}

	public UserDao getUserDao() {return userDao;}
	public void setUserDao(UserDao userDao) {this.userDao = userDao;}

	// TODO: Soon to be deprecated
	public Object getDefaultUserContent(User user){

		Object response = null;
		List<ResponseGroup> responseGroups = user.getResponseGroups();
		if(CollectionUtils.isNotEmpty(responseGroups) &&
				responseGroups.get(0) != null &&
				CollectionUtils.isNotEmpty(responseGroups.get(0).getResponses()) &&
				responseGroups.get(0).getResponses().get(0) != null){
			response = responseGroups.get(0).getResponses().get(0).getData();
		}
		else {
			response = AlexaResponseUtil.createSimpleResponse("You have no saved responses","There are no saved responses for your ID ("+user.getId().toString()+")","There are no saved responses");
		}

		// Update statistics for the user
		user.setNumContentDownloads(user.getNumContentDownloads()+1);
		user.setLastEchoDownloadTime(Instant.now());
		int contentLength = ResponderUtils.getLengthOfContent(response);
		user.setNumCharactersDownloaded(user.getNumCharactersDownloaded() + contentLength);
		userDao.saveUser(user);

		return response;
	}

	public Object getUserContent(User user, String intent, String state, Map<String,String> slots){

		if(MapUtils.isEmpty(slots)){slots = null;}

		ResponseKey responseKey = new ResponseKey(intent, slots, state);
		ResponseGroup responseGroup = UserUtils.getResponseGroupByResponseKey(user, responseKey);

		Object response = null;

		String serializedResponseKey = ResponderUtils.serialize(responseKey);
		Object errorResponse = AlexaResponseUtil.createSimpleResponse("There is no response for this input","There is no response for this input\n"+serializedResponseKey,"There is no response for this input");
		if(responseGroup == null){
			response = errorResponse;
		}
		else{
			Response savedResponse = ResponseGroupUtils.getRandomResponse(responseGroup);
			if(savedResponse != null){
				response = savedResponse.getData();
			}
			else{
				response = errorResponse;
			}
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
