package com.dotmarketing.cms.creditcard.model.verisign;

import com.dotmarketing.cms.creditcard.model.CreditCardProcessorResponse;

public class VerisignCreditCardProcessorResponse extends CreditCardProcessorResponse {
	
//	User and Transaction Information
	private String RESULT = Integer.toString(Integer.MIN_VALUE);
	private String PNREF;
	private String RESPMSG;
	private String AUTHCODE;
	private String AVSADDR;
	private String AVSZIP;
	private String CVV2MATCH;
	private String PREFPSMSG;
	private String POSTFPSMSG;
	private String response;
	
	public String getAUTHCODE() {
		return AUTHCODE;
	}
	public void setAUTHCODE(String authcode) {
		AUTHCODE = authcode;
	}
	public String getAVSADDR() {
		return AVSADDR;
	}
	public void setAVSADDR(String avsaddr) {
		AVSADDR = avsaddr;
	}
	public String getAVSZIP() {
		return AVSZIP;
	}
	public void setAVSZIP(String avszip) {
		AVSZIP = avszip;
	}
	public String getCVV2MATCH() {
		return CVV2MATCH;
	}
	public void setCVV2MATCH(String cvv2match) {
		CVV2MATCH = cvv2match;
	}
	public String getPNREF() {
		return PNREF;
	}
	public void setPNREF(String pnref) {
		PNREF = pnref;
	}
	public String getPOSTFPSMSG() {
		return POSTFPSMSG;
	}
	public void setPOSTFPSMSG(String postfpsmsg) {
		POSTFPSMSG = postfpsmsg;
	}
	public String getPREFPSMSG() {
		return PREFPSMSG;
	}
	public void setPREFPSMSG(String prefpsmsg) {
		PREFPSMSG = prefpsmsg;
	}
	public String getRESPMSG() {
		return RESPMSG;
	}
	public void setRESPMSG(String respmsg) {
		RESPMSG = respmsg;
	}
	public String getRESULT() {
		return RESULT;
	}
	public void setRESULT(String result) {
		RESULT = result;
	}
	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
	}	
	
	public boolean orderApproved()
	{
		boolean returnValue = false;
		if(this.getRESULT().equals("0") || this.getRESULT().equals("126"))
		{
			returnValue = true;
		}
		return returnValue;
	}
	
	public String getApproved()
	{
		String returnValue = DENIED;
		if(this.getRESULT().equals("0") || this.getRESULT().equals("126"))
		{
			returnValue = APPROVED;
		}
		return returnValue;
	}
	public String getOrdernum()
	{
		return AUTHCODE;
	}
	public String getError()
	{
		return RESPMSG;
	}
}
