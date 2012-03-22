package com.dotcms.content.business;

public class DotMappingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5613964763936171392L;
	String message;
	public DotMappingException(String message){
		this.message = message;
	}
	public DotMappingException(String message, Throwable cause){
	    super(message,cause);
	}
	@Override
	public String getLocalizedMessage() {
		// TODO Auto-generated method stub
		return  getMessage();
	}
	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return message;
	}
	
}
