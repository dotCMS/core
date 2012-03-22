package com.dotcms.autoupdater;



public class UpdateException extends Exception {
	
	public static final String ERROR = "ERROR";
	public static final String SUCCESS = "SUCCESS";
	public static final String CANCEL = "CANCEL";

	public static enum UpdateExceptionType {
		
	}

	String type;
	
	public String getType() {
		return type;
	}



	public UpdateException(String string, String type) {
		super(string);
		this.type=type;
	}
	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
