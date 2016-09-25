package com.derpgroup.echodebugger.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.derpgroup.echodebugger.model.Response;
import com.derpgroup.echodebugger.model.ResponseGroup;
import com.derpgroup.echodebugger.model.ResponseKey;
import com.derpgroup.echodebugger.model.User;

public class UserUtils {

	/**
	 * Loads all ResponseGroups and Responses into the User object.
	 * This populates the map of ResponseGroups indexed by their serialized ResponseKey,
	 * loads the ResponseGroups indexed by their ID,
	 * and loads all the Responses into the ResponseGroups indexed by their ID.
	 */
	public static void mapResponsesIntoUser(User user){
		if(user == null){return;}

		// Reset the runtime User data structures
		user.setData(new HashMap<String, ResponseGroup>());
		user.setMapOfResponseGroupsById(new HashMap<Integer, ResponseGroup>());

		// Cycle through all the ResponseGroups...
		for(ResponseGroup responseGroup : user.getResponseGroups()){
			mapResponseIntoUser(user, responseGroup);
		}
	}

	/**
	 * Load a ResponseGroup and its Responses into the User object.
	 * This populates into the map of ResponseGroups indexed by their serialized ResponseKey,
	 * loads the ResponseGroups indexed by their ID,
	 * and loads all the Responses into the ResponseGroups indexed by their ID.
	 */
	protected static void mapResponseIntoUser(User user, ResponseGroup responseGroup){
		if(user == null || responseGroup == null){return;}

		// Data structures we're going to be populating
		Map<String, ResponseGroup> responseGroupsMappedByKey = user.getData();
		Map<Integer, ResponseGroup> responseGroupsMappedById = user.getMapOfResponseGroupsById();

		// Map the ResponseGroups by ResponseKey
		ResponseKey key = responseGroup.getKey();
		String serializedKey = ResponderUtils.serialize(key);
		responseGroupsMappedByKey.put(serializedKey, responseGroup);

		// Map the ResponseGroup by id
		responseGroupsMappedById.put(Integer.valueOf(responseGroup.getId()), responseGroup);

		// Map the individual Responses into the ResponseGroup by id
		ResponseGroupUtils.mapResponsesFromResponseGroup(responseGroup);
	}

	/**
	 * Saves a response into a User object
	 * @param user
	 * @param responseKey
	 * @param response
	 * @param shouldAppendResponse
	 */
	public static void saveResponse(User user, ResponseKey responseKey, Response response, boolean shouldAppendResponse){

		if(user == null || responseKey == null || response == null){return;}
		if(MapUtils.isEmpty(user.getData())){
			user.setData(new HashMap<String, ResponseGroup>());
		}

		String serializedResponseKey = ResponderUtils.serialize(responseKey);
		ResponseGroup responseGroup = user.getData().get(serializedResponseKey);

		// If it's a new ResponseGroup, then register it
		if(responseGroup == null){
			responseGroup = new ResponseGroup(responseKey, response);
			responseGroup.setId(UserUtils.getAndIncrementResponseGroupId(user));
			if(CollectionUtils.isEmpty(user.getResponseGroups())){
				user.setResponseGroups(new ArrayList<ResponseGroup>());
			}
			user.getResponseGroups().add(responseGroup);
		}
		// Else it's an existing ResponseGroup, so replace or append
		else {
			if(shouldAppendResponse){
				responseGroup.getResponses().add(response);
			}
			else {
				List<Response> responsesList = new ArrayList<>();
				responsesList.add(response);
				responseGroup.setResponses(responsesList);
			}
		}
		Integer newResponseId = Integer.valueOf(ResponseGroupUtils.getAndIncrementResponseId(responseGroup));
		response.setId(newResponseId.toString());
		UserUtils.mapResponsesIntoUser(user);
	}

	public static ResponseGroup getResponseGroupByResponseKey(User user, ResponseKey responseKey){
		String serializedResponseKey = ResponderUtils.serialize(responseKey);
		if(MapUtils.isEmpty(user.getData())){return null;}
		return user.getData().get(serializedResponseKey);
	}

	public static int getAndIncrementResponseGroupId(User user){
		int responseGroupId = user.getNextResponseGroupId();
		user.setNextResponseGroupId(responseGroupId+1);
		return responseGroupId;
	}

	public static ResponseGroup getResponseGroup(User user, int responseGroupId){
		if(user == null){return null;}

		Map<Integer, ResponseGroup> mapOfResponseGroupsById = user.getMapOfResponseGroupsById();
		if(MapUtils.isEmpty(mapOfResponseGroupsById) ){
			return null;
		}
		return mapOfResponseGroupsById.get(Integer.valueOf(responseGroupId));
	}

	public static Response getResponse(User user, int responseGroupId, int responseId){
		if(user == null){return null;}

		return ResponseGroupUtils.getResponse(getResponseGroup(user, responseGroupId), responseId);
	}

	public static Set<String> getRegisteredIntents(User user){
		Set<String> registeredIntents = new HashSet<>();
		List<ResponseGroup> responseGroups = user.getResponseGroups();
		if(CollectionUtils.isEmpty(responseGroups)){return registeredIntents;}

		for(ResponseGroup responseGroup : responseGroups){
			ResponseKey key = responseGroup.getKey();
			if(key == null){continue;}
			String intentName = key.getIntentName();
			if(StringUtils.isNotEmpty(intentName)){
				registeredIntents.add(intentName);
			}
		}
		return registeredIntents;
	}
}
