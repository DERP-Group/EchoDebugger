package com.derpgroup.echodebugger.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.SpeechletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponderUtils {

	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Serializes an object into a String
	 */
	public static String serialize(Object object){
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	/**
	 * Measures the length of an object after it's serialized
	 */
	public static int getLengthOfContent(Object content){
		if(content == null){return 0;}
		String serializedBody = serialize(content);
		if(serializedBody == null){return 0;}
		return serializedBody.length();
	}

	public static Map<String, String> getMessageAsMap(SpeechletRequest request) {
		if (!(request instanceof IntentRequest)) {
			return Collections.emptyMap();
		}

		IntentRequest intentRequest = (IntentRequest) request;
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (Entry<String, Slot> entry : intentRequest.getIntent().getSlots().entrySet()) {
			Slot slot = entry.getValue();
			if (slot != null) {
				result.put(slot.getName(), slot.getValue());
			}
		}

		return result;
	}
}
