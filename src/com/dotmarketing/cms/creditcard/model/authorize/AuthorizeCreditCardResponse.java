package com.dotmarketing.cms.creditcard.model.authorize;

import com.dotmarketing.cms.creditcard.model.CreditCardProcessorResponse;

public class AuthorizeCreditCardResponse extends CreditCardProcessorResponse
{
	private String ordernum;
	
	public boolean orderApproved() {
		return getCode().equals(APPROVED);
	}
	
	public String getApproved() {
		return getCode();
	}
	
	public void setOrdernum(String ordernum) {
		this.ordernum = ordernum;
	}
	
	public String getOrdernum() {
		return ordernum;
	}
	
	public String getError() {
		if ((getMessage() != null) && (!getMessage().trim().equals(""))) {
			int beginIndex = getMessage().indexOf("message=") + 8;
			
			if (-1 < beginIndex) {
				int endIndex = getMessage().indexOf("&", beginIndex);
				
				if (-1 < endIndex) {
					return ("\"" + getMessage().substring(beginIndex, endIndex) + "\"");
				} else {
					return ("\"" + getMessage().substring(beginIndex) + "\"");
				}
			} else {
				return "";
			}
		} else {
			return "";
		}
	}
}