package com.dotmarketing.cms.creditcard.model.authorize;

import com.dotmarketing.cms.creditcard.model.CreditCardProcessorException;

public class AuthorizeCreditCardProcessorException extends CreditCardProcessorException
{
	private int code;	
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	public AuthorizeCreditCardProcessorException(String message, Throwable ex) 
	{
		super(message,ex);
	}
	
	public AuthorizeCreditCardProcessorException(String message) 
	{
		super(message);
	}
}
