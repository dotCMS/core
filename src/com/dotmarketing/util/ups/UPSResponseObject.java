package com.dotmarketing.util.ups;

import java.io.Serializable;

/**
 * 
 * @author Oswaldo Gallango
 *
 */

public class UPSResponseObject implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String statusCode;
	private String statusDescription;
	private String errorCode;
	private String errorDescription;
	
	private String currencyCode;
	private String totalValue;
	
	public UPSResponseObject(){
		
		statusCode = "";
		statusDescription = "";
		currencyCode="";
		totalValue="0";
		errorCode="";
		errorDescription="";
		
	}
	
	public String getCurrencyCode() {
		return currencyCode;
	}
	
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}
	
	public String getTotalValue() {
		return totalValue;
	}
	
	public void setTotalValue(String totalValue) {
		this.totalValue = totalValue;
	}
	
	public String getStatusCode() {
		return statusCode;
	}
	
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	
	public String getStatusDescription() {
		return statusDescription;
	}
	
	public void setStatusDescription(String statusDescription) {
		this.statusDescription = statusDescription;
	}
	
	public String getErrorCode() {
		return errorCode;
	}
	
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	
	public String getErrorDescription() {
		return errorDescription;
	}
	
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}
	
}
