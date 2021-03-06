package com.derpgroup.echodebugger.model;

import java.util.HashMap;
import java.util.Map;

public class Response {

	private String id;
	private Map<String, Object> data = new HashMap<>();

	public Response(){
	}
	public Response(String id, Map<String, Object> data){
		this.id = id;
		this.data = data;
	}

	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	public Map<String, Object> getData() {return data;}
	public void setData(Map<String, Object> data) {this.data = data;}

	@Override
	public String toString(){
		if(data==null){return id + "= No data";}
		return id + "=" + data.toString();
	}
}
