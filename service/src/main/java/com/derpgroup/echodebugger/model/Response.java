package com.derpgroup.echodebugger.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Response {

	private UUID id;
	private Map<String, Object> data = new HashMap<>();

	public Response(){
		this.id = UUID.randomUUID();
	}
	public Response(Map<String, Object> data){
		this.id = UUID.randomUUID();
		this.data = data;
	}

	public UUID getId() {return id;}
	public void setId(UUID id) {this.id = id;}
	public Map<String, Object> getData() {return data;}
	public void setData(Map<String, Object> data) {this.data = data;}

	@Override
	public String toString(){
		if(data==null){return id.toString() + "= No data";}
		return id.toString() + "=" + data.toString();
	}
}
