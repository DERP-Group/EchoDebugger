package com.derpgroup.echodebugger.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import com.derpgroup.echodebugger.model.Response;
import com.derpgroup.echodebugger.model.ResponseGroup;
import com.derpgroup.echodebugger.model.ResponseGroupSummary;
import com.derpgroup.echodebugger.model.User;

public class ResponseGroupUtils {

	/**
	 * Returns a single response from this ResponseGroup
	 */
	public static Response getRandomResponse(ResponseGroup responseGroup){

		List<Response> responses = responseGroup.getResponses();
		if(CollectionUtils.isEmpty(responses)){return null;}

		// TODO: This will be controllable through a config
		Random rand = new Random();

		int index = rand.nextInt(responses.size());
		return responses.get(index);
	}

	/**
	 * Re-populates the runtime mapOfResponsesById
	 * @param responseGroup
	 */
	public static void mapResponsesFromResponseGroup(ResponseGroup responseGroup){
		if(responseGroup == null || CollectionUtils.isEmpty(responseGroup.getResponses())){return;}

		// Reset the runtime data structure
		responseGroup.setMapOfResponsesById(new HashMap<Integer,Response>());
		Map<Integer,Response> mapOfResponsesById = responseGroup.getMapOfResponsesById();

		// Now populate it with fresh up-to-date data
		for(Response response : responseGroup.getResponses()){
			Integer id = Integer.valueOf(response.getId());
			mapOfResponsesById.put(id, response);
		}
	}

	public static int getAndIncrementResponseId(ResponseGroup responseGroup){
		int responseId = responseGroup.getNextResponseId();
		responseGroup.setNextResponseId(responseId+1);
		return responseId;
	}

	public static Response getResponse(ResponseGroup responseGroup, int responseId){
		if(responseGroup == null){return null;}

		Map<Integer, Response> mapOfResponsesById = responseGroup.getMapOfResponsesById();
		if(MapUtils.isEmpty(mapOfResponsesById)){
			return null;
		}
		return mapOfResponsesById.get(Integer.valueOf(responseId));
	}

	/**
	 * Constructs a ResponseGroup summary object for display to the user
	 * @param user
	 * @param responseGroup
	 * @return
	 */
	public static ResponseGroupSummary buildResponseGroupSummary(User user, ResponseGroup responseGroup){

		ResponseGroupSummary summary = new ResponseGroupSummary();

		String url = "/responder/users/"+user.getId().toString()+"/responses/"+responseGroup.getId();
		summary.setUrl(url);

		List<String> responseIds = new ArrayList<>();
		List<Response> responses = responseGroup.getResponses();
		if(CollectionUtils.isNotEmpty(responses)){
			for(Response response : responses){
				Integer id = Integer.valueOf(response.getId());
				responseIds.add(url+"/"+id);
			}
		}
		summary.setParameters(responseGroup.getKey());
		summary.setResponses(responseIds);
		return summary;
	}
}
