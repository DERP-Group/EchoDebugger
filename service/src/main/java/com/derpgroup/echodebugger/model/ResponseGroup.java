package com.derpgroup.echodebugger.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ResponseGroup {

	private int id;
	private ResponseKey key;
	private List<Response> responses = new ArrayList<Response>();
	private int nextResponseId = 0;
	@JsonIgnore
	private Map<Integer, Response> mapOfResponsesById = new HashMap<>();

	public ResponseGroup(){
	}
	public ResponseGroup(ResponseKey key, Response response){
		this.key = key;
		responses.add(response);
	}

	public int getId() {return id;}
	public void setId(int id) {this.id = id;}
	public ResponseKey getKey() {return key;}
	public void setKey(ResponseKey key) {this.key = key;}
	public List<Response> getResponses() {return responses;}
	public void setResponses(List<Response> responses) {this.responses = responses;}
	public int getNextResponseId() {return nextResponseId;}
	public void setNextResponseId(int nextResponseId) {this.nextResponseId = nextResponseId;}
	public Map<Integer, Response> getMapOfResponsesById() {return mapOfResponsesById;}
	public void setMapOfResponsesById(Map<Integer, Response> mapOfResponsesById) {this.mapOfResponsesById = mapOfResponsesById;}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder("id=");
		builder.append(id);
		builder.append(" Key=[");
		builder.append(key.toString());
		builder.append("], Responses=");
		builder.append(responses);
		return builder.toString();
	}
}
