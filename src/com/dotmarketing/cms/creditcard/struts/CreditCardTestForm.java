package com.dotmarketing.cms.creditcard.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.UtilMethods;

public class CreditCardTestForm  extends ValidatorForm {
		
	    //Billing Info
	    private String billingAddress1;
	    private String billingAddress2;
	    private String billingCity;
	    private String billingState;
	    private String billingStateOtherCountryText;
	    private String billingZip;
	    private String billingCountry;
	    private String billingPhone;
	    private String billingFax;	   
		private String billingFirstName;
		private String billingLastName;	    
	    private String billingEmail;
	    
	    //Shipping Info	    
	    private String shippingAddress1;
	    private String shippingAddress2;
	    private String shippingCity;
	    private String shippingState;
	    private String shippingStateOtherCountryText;
	    private String shippingZip;
	    private String shippingCountry;
	    private String shippingPhone;
	    private String shippingFax;	    
		private String shippingFirstName;
		private String shippingLastName;	    
	    private String shippingEmail;
	    
	    //Credit Card Info
	    private String nameOnCard;
	    private String cardType;
	    private String cardNumber; 
	    private int cardExpMonth;
	    private int cardExpYear;
	    private String cardVerificationValue;
	    
	    //Amounts
	    private float orderSubTotal;
	    private float orderDiscount;
	    private float orderShipping;
	    private float orderTax;
	    private float orderTotal;
	    
	    //Other
	    private String typeOfPayment;
	    
	    //Methods
		public String getBillingAddress1() {
			return billingAddress1;
		}
		public void setBillingAddress1(String billingAddress1) {
			this.billingAddress1 = billingAddress1;
		}
		public String getBillingAddress2() {
			return billingAddress2;
		}
		public void setBillingAddress2(String billingAddress2) {
			this.billingAddress2 = billingAddress2;
		}
		public String getBillingCity() {
			return billingCity;
		}
		public void setBillingCity(String billingCity) {
			this.billingCity = billingCity;
		}
		public String getBillingCountry() {
			return billingCountry;
		}
		public void setBillingCountry(String billingCountry) {
			this.billingCountry = billingCountry;
		}
		public String getBillingFax() {
			return billingFax;
		}
		public void setBillingFax(String billingFax) {
			this.billingFax = billingFax;
		}
		public String getBillingFirstName() {
			return billingFirstName;
		}
		public void setBillingFirstName(String billingFirstName) {
			this.billingFirstName = billingFirstName;
		}
		public String getBillingLastName() {
			return billingLastName;
		}
		public void setBillingLastName(String billingLastName) {
			this.billingLastName = billingLastName;
		}
		public String getBillingPhone() {
			return billingPhone;
		}
		public void setBillingPhone(String billingPhone) {
			this.billingPhone = billingPhone;
		}
		public String getBillingState() {
			return billingState;
		}
		public void setBillingState(String billingState) {
			this.billingState = billingState;
		}
		public String getBillingStateOtherCountryText() {
			return billingStateOtherCountryText;
		}
		public void setBillingStateOtherCountryText(String billingStateOtherCountryText) {
			this.billingStateOtherCountryText = billingStateOtherCountryText;
		}
		public String getBillingZip() {
			return billingZip;
		}
		public void setBillingZip(String billingZip) {
			this.billingZip = billingZip;
		}
		public int getCardExpMonth() {
			return cardExpMonth;
		}
		public void setCardExpMonth(int cardExpMonth) {
			this.cardExpMonth = cardExpMonth;
		}
		public int getCardExpYear() {
			return cardExpYear;
		}
		public void setCardExpYear(int cardExpYear) {
			this.cardExpYear = cardExpYear;
		}
		public String getCardNumber() {
			return cardNumber;
		}
		public void setCardNumber(String cardNumber) {
			this.cardNumber = cardNumber;
		}
		public String getCardType() {
			return cardType;
		}
		public void setCardType(String cardType) {
			this.cardType = cardType;
		}
		public String getCardVerificationValue() {
			return cardVerificationValue;
		}
		public void setCardVerificationValue(String cardVerificationValue) {
			this.cardVerificationValue = cardVerificationValue;
		}
		public String getNameOnCard() {
			return nameOnCard;
		}
		public void setNameOnCard(String nameOnCard) {
			this.nameOnCard = nameOnCard;
		}
		public float getOrderDiscount() {
			return orderDiscount;
		}
		public void setOrderDiscount(float orderDiscount) {
			this.orderDiscount = orderDiscount;
		}
		public float getOrderShipping() {
			return orderShipping;
		}
		public void setOrderShipping(float orderShipping) {
			this.orderShipping = orderShipping;
		}
		public float getOrderSubTotal() {
			return orderSubTotal;
		}
		public void setOrderSubTotal(float orderSubTotal) {
			this.orderSubTotal = orderSubTotal;
		}
		public float getOrderTax() {
			return orderTax;
		}
		public void setOrderTax(float orderTax) {
			this.orderTax = orderTax;
		}
		public float getOrderTotal() {
			return orderTotal;
		}
		public void setOrderTotal(float orderTotal) {
			this.orderTotal = orderTotal;
		}
		public String getShippingAddress1() {
			return shippingAddress1;
		}
		public void setShippingAddress1(String shippingAddress1) {
			this.shippingAddress1 = shippingAddress1;
		}
		public String getShippingAddress2() {
			return shippingAddress2;
		}
		public void setShippingAddress2(String shippingAddress2) {
			this.shippingAddress2 = shippingAddress2;
		}
		public String getShippingCity() {
			return shippingCity;
		}
		public void setShippingCity(String shippingCity) {
			this.shippingCity = shippingCity;
		}		
		public String getShippingCountry() {
			return shippingCountry;
		}
		public void setShippingCountry(String shippingCountry) {
			this.shippingCountry = shippingCountry;
		}
		public String getShippingFax() {
			return shippingFax;
		}
		public void setShippingFax(String shippingFax) {
			this.shippingFax = shippingFax;
		}
		public String getShippingFirstName() {
			return shippingFirstName;
		}
		public void setShippingFirstName(String shippingFirstName) {
			this.shippingFirstName = shippingFirstName;
		}
		public String getShippingLastName() {
			return shippingLastName;
		}
		public void setShippingLastName(String shippingLastName) {
			this.shippingLastName = shippingLastName;
		}
		public String getShippingPhone() {
			return shippingPhone;
		}
		public void setShippingPhone(String shippingPhone) {
			this.shippingPhone = shippingPhone;
		}
		public String getShippingState() {
			return shippingState;
		}
		public void setShippingState(String shippingState) {
			this.shippingState = shippingState;
		}
		public String getShippingStateOtherCountryText() {
			return shippingStateOtherCountryText;
		}
		public void setShippingStateOtherCountryText(
				String shippingStateOtherCountryText) {
			this.shippingStateOtherCountryText = shippingStateOtherCountryText;
		}
		public String getShippingZip() {
			return shippingZip;
		}
		public void setShippingZip(String shippingZip) {
			this.shippingZip = shippingZip;
		}
		public String getTypeOfPayment() {
			return typeOfPayment;
		}
		public void setTypeOfPayment(String typeOfPayment) {
			this.typeOfPayment = typeOfPayment;
		}		
		public String getBillingEmail() {
			return billingEmail;
		}
		public void setBillingEmail(String billingEmail) {
			this.billingEmail = billingEmail;
		}
		public String getShippingEmail() {
			return shippingEmail;
		}
		public void setShippingEmail(String shippingEmail) {
			this.shippingEmail = shippingEmail;
		}
		
		public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
			ActionErrors errors = new ActionErrors();
			//First Name
	    	if (!UtilMethods.isSet(billingFirstName)) 
	    	{    		
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","First Name"));    		
	    	}
	    	//Last Name
	    	if (!UtilMethods.isSet(billingLastName)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Last Name"));
	    	}
	    	//Address1
	    	if (!UtilMethods.isSet(billingAddress1)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Address 1"));
	    	}
	    	//City
	    	if (!UtilMethods.isSet(billingCity)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","City"));
	    	}
	    	//State
	    	if (!UtilMethods.isSet(billingState)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","State"));
	    	}
	    	//ZIP
	    	if (!UtilMethods.isSet(billingZip)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Zip"));
	    	}
	    	//Country
	    	if (!UtilMethods.isSet(billingCountry)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Country"));
	    	}
	    	//Phone
	    	if (!UtilMethods.isSet(billingPhone)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Phone"));
	    	}
	    	//Email
	    	if (!UtilMethods.isSet(billingEmail)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Email"));
	    	}
	    	//Amount
	    	if (!UtilMethods.isSet(orderTotal)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","State"));
	    	}
	    	//Card Type
	    	if (!UtilMethods.isSet(cardType)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Card Type"));
	    	}
	    	//Credit Card Number
	    	if (!UtilMethods.isSet(cardNumber)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Card Number"));
	    	}	
	    	//CVV
	    	if (!UtilMethods.isSet(cardVerificationValue)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","CVV"));
	    	}
	    	//Name on card
	    	if (!UtilMethods.isSet(nameOnCard)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Name on Card"));
	    	}	
	    	//Type API
	    	if (!UtilMethods.isSet(billingState)) 
	    	{
	    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","State"));
	    	}	
	    	return errors;
		}
	    
	    
}
