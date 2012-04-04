package com.dotmarketing.cms.creditcard.model;

public class CreditCardDeniedException extends Exception 
{

	private CreditCardProcessorResponse response;
	
	public CreditCardDeniedException(String message, CreditCardProcessorResponse response) {
		super(message);		
		this.response = response;
	}

	public CreditCardProcessorResponse getResponse() {
		return response;
	}


}
