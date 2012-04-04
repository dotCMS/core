package com.dotmarketing.cms.creditcard.model.linkpoint;

import com.dotmarketing.cms.creditcard.model.CreditCardProcessorException;

/**
 * This class is used by the CreditCardProcessor to anounce data and comunication errors in a credit card process. 
 * @author David
 *
 */
public class LinkPointCreditCardProcessorException extends CreditCardProcessorException 
{	
	private int code;	
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public LinkPointCreditCardProcessorException(int code, String message) 
	{
		super(message);
		setCode(code);
	}
	
	LinkPointCreditCardProcessorException (int code, String message, Throwable ex)
	{
		super(message,ex);
		setCode(code);
	}	
}
