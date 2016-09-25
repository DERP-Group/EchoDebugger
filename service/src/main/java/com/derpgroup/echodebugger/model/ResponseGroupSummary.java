package com.derpgroup.echodebugger.model;

import java.util.List;

public class ResponseGroupSummary {

	private ResponseKey parameters;
	private String url;
	private List<String> responses;

	public ResponseKey getParameters() {return parameters;}
	public void setParameters(ResponseKey parameters) {this.parameters = parameters;}
	public String getUrl() {return url;}
	public void setUrl(String url) {this.url = url;}
	public List<String> getResponses() {return responses;}
	public void setResponses(List<String> responses) {this.responses = responses;}
}
