package com.derpgroup.echodebugger.model;

import java.util.Map;

/**
 * This object contains all the responses for an intent.
 */
public class IntentResponses {

	private String intentName;
	private Map<String, Object> data;

	public String getIntentName() {return intentName;}
	public void setIntentName(String intentName) {this.intentName = intentName;}
	public Map<String, Object> getData() {return data;}
	public void setData(Map<String, Object> data) {this.data = data;}
}
