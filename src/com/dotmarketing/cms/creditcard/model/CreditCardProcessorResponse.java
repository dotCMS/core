package com.dotmarketing.cms.creditcard.model;

public abstract class CreditCardProcessorResponse 
{	
	public static final String APPROVED = "1";
	public static final String DENIED = "2";
	public static final String ERROR = "3";
	
	private String code;
	private String message;
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	abstract public boolean orderApproved();
	abstract public String getApproved();
	abstract public String getOrdernum();
	abstract public String getError();
}