package com.derpgroup.echodebugger.providers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;

import com.derpgroup.echodebugger.exceptions.ResponderException;

@Provider
public class ResponderExceptionMapper implements ExceptionMapper<Exception> {

	@Override
	public Response toResponse(Exception exception) {

		Status status = Status.INTERNAL_SERVER_ERROR;
		String errorMessage = exception.getMessage();
		if(StringUtils.isEmpty(errorMessage)){
			errorMessage = "There was an error";
		}

		if(exception instanceof ResponderException){
			ResponderException e = (ResponderException) exception;
			switch(e.getType()){
			case NO_INTENT_SPECIFIED:
			case REPEAT_QUERY_PARAMETER:
			case RESPONSE_TOO_LONG:
			case UNRECOGNIZED_ID:
			case NO_SAVED_RESPONSE:
				status = Status.BAD_REQUEST;
			default:
			}
		}
		Response response = Response
				.status(status)
				.type(MediaType.APPLICATION_JSON)
				.entity("{\"message\":\""+errorMessage+"\"}")
				.build();

		return response;
	}
}