package com.dotmarketing.cms.creditcard.form;

import java.util.Date;

import org.apache.struts.validator.ValidatorForm;

public class CreditCardForm  extends ValidatorForm {
//	CreditCardInfo
	private String creditCardNumber;	
	private String creditCardName;
	private String creditCardTypeName;
	private String crditCardType;
	private String creditCardCVV;
	private Date creditCardExpirationDate;
	private String creditCardExpirationMonth;
	private String creditCardExpirationYear;
	//Amount
	private float amount;
	
	//Billing Address	
	private String billingFirstName;
	private String billingLastName;
	private String billingEmailAdress;
	private String billingCompany;
	private String billingStreet;
	private String billingCity;
	private String billingState;
	private String billingZip;
	private String billingCountry;
	private String billingPhone;
	private boolean shipToBilling;
	
	//Shipping Address
	private String shippingFirstName;
	private String shippingLastName;
	private String shippingEmailAdress;
	private String shippingCompany;
	private String shippingStreet;
	private String shippingCity;
	private String shippingState;
	private String shippingZip;
	private String shippingCountry;
	private String shippingPhone;
	
	public String getCreditCardExpirationYear() {
		return creditCardExpirationYear;
	}
	public void setCreditCardExpirationYear(String creditCardExpirationYear) {
		this.creditCardExpirationYear = creditCardExpirationYear;
	}
	public String getCreditCardExpirationMonth() {
		return creditCardExpirationMonth;
	}
	public void setCreditCardExpirationMonth(String creditCardExpirationMonth) {
		this.creditCardExpirationMonth = creditCardExpirationMonth;
	}
	public float getAmount() {
		return amount;
	}
	public void setAmount(float amount) {
		this.amount = amount;
	}
	public String getBillingCity() {
		return billingCity;
	}
	public void setBillingCity(String billingCity) {
		this.billingCity = billingCity;
	}
	public String getBillingCompany() {
		return billingCompany;
	}
	public void setBillingCompany(String billingCompany) {
		this.billingCompany = billingCompany;
	}
	public String getBillingCountry() {
		return billingCountry;
	}
	public void setBillingCountry(String billingCountry) {
		this.billingCountry = billingCountry;
	}
	public String getBillingEmailAdress() {
		return billingEmailAdress;
	}
	public void setBillingEmailAdress(String billingEmailAdress) {
		this.billingEmailAdress = billingEmailAdress;
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
	public String getBillingStreet() {
		return billingStreet;
	}
	public void setBillingStreet(String billingStreet) {
		this.billingStreet = billingStreet;
	}
	public String getBillingZip() {
		return billingZip;
	}
	public void setBillingZip(String billingZip) {
		this.billingZip = billingZip;
	}
	public String getCrditCardType() {
		return crditCardType;
	}
	public void setCrditCardType(String crditCardType) {
		this.crditCardType = crditCardType;
	}
	public String getCreditCardCVV() {
		return creditCardCVV;
	}
	public void setCreditCardCVV(String creditCardCVV) {
		this.creditCardCVV = creditCardCVV;
	}
	public Date getCreditCardExpirationDate() {
		return creditCardExpirationDate;
	}
	public void setCreditCardExpirationDate(Date creditCardExpirationDate) {
		this.creditCardExpirationDate = creditCardExpirationDate;
	}
	public String getCreditCardName() {
		return creditCardName;
	}
	public void setCreditCardName(String creditCardName) {
		this.creditCardName = creditCardName;
	}
	public String getCreditCardNumber() {
		return creditCardNumber;
	}
	public void setCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}
	public String getCreditCardTypeName() {
		return creditCardTypeName;
	}
	public void setCreditCardTypeName(String creditCardTypeName) {
		this.creditCardTypeName = creditCardTypeName;
	}
	public String getShippingCity() {
		return shippingCity;
	}
	public void setShippingCity(String shippingCity) {
		this.shippingCity = shippingCity;
	}
	public String getShippingCompany() {
		return shippingCompany;
	}
	public void setShippingCompany(String shippingCompany) {
		this.shippingCompany = shippingCompany;
	}
	public String getShippingCountry() {
		return shippingCountry;
	}
	public void setShippingCountry(String shippingCountry) {
		this.shippingCountry = shippingCountry;
	}
	public String getShippingEmailAdress() {
		return shippingEmailAdress;
	}
	public void setShippingEmailAdress(String shippingEmailAdress) {
		this.shippingEmailAdress = shippingEmailAdress;
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
	public String getShippingStreet() {
		return shippingStreet;
	}
	public void setShippingStreet(String shippingStreet) {
		this.shippingStreet = shippingStreet;
	}
	public String getShippingZip() {
		return shippingZip;
	}
	public void setShippingZip(String shippingZip) {
		this.shippingZip = shippingZip;
	}
	public boolean isShipToBilling() {
		return shipToBilling;
	}
	public void setShipToBilling(boolean shipToBilling) {
		this.shipToBilling = shipToBilling;
	}	
}
