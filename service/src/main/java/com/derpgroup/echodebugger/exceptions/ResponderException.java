package com.derpgroup.echodebugger.exceptions;

public class ResponderException extends RuntimeException {

	private static final long serialVersionUID = 8409198483579933450L;

	private ExceptionType type;

	public ResponderException(String message, ExceptionType type, Exception e){
		super(message, e);
		this.type = type;
	}
	public ResponderException(String message, ExceptionType type){
		super(message);
		this.type = type;
	}

	public ExceptionType getType() {return type;}
	public void setType(ExceptionType type) {this.type = type;}
}
