package com.dotmarketing.cms.creditcard.model;

public abstract class CreditCardProcessorException extends Exception 
{
	/**
	 * Code used for gateway comunication errors
	 */
	public static final int COMMUNICATION_ERROR = 0;
	
	/**
	 * Code used for erroneous or missing data in a credit card process;
	 */
	public static final int DATA_MISSING = 1;
	
	abstract public int getCode();

	abstract public void setCode(int code);
	
	private String gatewayMessage;
		
	public String getGatewayMessage() 
	{
		return gatewayMessage;
	}
	public void setGatewayMessage(String gatewayMessage) 
	{
		this.gatewayMessage = gatewayMessage;
	}
	
	public CreditCardProcessorException(String message) {
		super(message);		
	}

	public CreditCardProcessorException(String message, Throwable ex) 
	{
		super(message,ex);		
	}
}
