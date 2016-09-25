package com.derpgroup.echodebugger.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

/**
 * This class is used as a key to determine what mock response should be returned
 */
public class ResponseKey {

	public static final String DEFAULT_RESPONDER_INTENT = "GETRESPONSE";
	private String intentName;

	/**
	 * Key=CustomSlot name, Value=CustomSlot value
	 */
	private Map<String, String> variables = new HashMap<>();

	public ResponseKey(){
		this(DEFAULT_RESPONDER_INTENT);
	}
	public ResponseKey(String intentName){
		this(intentName, null);
	}
	public ResponseKey(String intentName, Map<String, String> variables){
		this.intentName = intentName;
		this.variables = variables;
	}

	public String getIntentName() {return intentName;}
	public void setIntentName(String intentName) {this.intentName = intentName;}
	public Map<String, String> getVariables() {return variables;}
	public void setVariables(Map<String, String> variables) {this.variables = variables;}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder(intentName);

		if(MapUtils.isEmpty(variables)){
			builder.append(" {}");
			return builder.toString();
		}
		builder.append(variables.toString());
		return builder.toString();
	}
}
