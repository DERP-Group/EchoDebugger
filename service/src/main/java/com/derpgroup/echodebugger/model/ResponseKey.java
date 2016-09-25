package com.derpgroup.echodebugger.model;

import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class is used as a key to determine what mock response should be returned
 */
public class ResponseKey {

	public static final String DEFAULT_RESPONDER_INTENT = "GETRESPONSE";

	private String intentName;
	private Map<String, String> variables;	// Key=CustomSlot name, Value=CustomSlot value
	private String state;

	public ResponseKey(){
		this(DEFAULT_RESPONDER_INTENT, null, null);
	}
	public ResponseKey(String intentName){
		this(intentName, null, null);
	}
	public ResponseKey(String intentName, Map<String, String> variables){
		this(intentName, variables, null);
	}
	public ResponseKey(String intentName, Map<String, String> variables, String state){
		this.intentName = intentName;
		this.variables = variables;
		this.state = state;
	}

	public String getIntentName() {return intentName;}
	public void setIntentName(String intentName) {this.intentName = intentName;}
	public Map<String, String> getVariables() {return variables;}
	public void setVariables(Map<String, String> variables) {this.variables = variables;}
	public String getState() {return state;}
	public void setState(String state) {this.state = state;}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder(intentName);

		if(MapUtils.isEmpty(variables)){
			builder.append(" variables={}");
		}
		else{
			builder.append(" variables=");
			builder.append(variables.toString());
		}

		if(StringUtils.isEmpty(state)){
			builder.append(" state=[]");
		}
		else{
			builder.append(" state=[");
			builder.append(state);
			builder.append("]");
		}
		return builder.toString();
	}
}
