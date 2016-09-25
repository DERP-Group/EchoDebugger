package com.derpgroup.echodebugger.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class ResponseGroup {

	private ResponseKey key;
	private List<Response> responses = new ArrayList<Response>();

	public ResponseGroup(){}
	public ResponseGroup(ResponseKey key, Response response){
		this.key = key;
		responses.add(response);
	}

	public ResponseKey getKey() {return key;}
	public void setKey(ResponseKey key) {this.key = key;}
	public List<Response> getResponses() {return responses;}
	public void setResponses(List<Response> responses) {this.responses = responses;}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder("Key=[");
		builder.append(key.toString());
		builder.append("], Responses=[");

		if(CollectionUtils.isEmpty(responses)){
			builder.append("]");
			return builder.toString();
		}
		for(int i=0; i<responses.size(); i++){
			if(i!=0){builder.append(", ");}
			builder.append(responses.toString());
		}
		builder.append(")");
		return builder.toString();
	}
}
