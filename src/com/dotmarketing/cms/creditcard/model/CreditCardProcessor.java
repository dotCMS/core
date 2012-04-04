package com.dotmarketing.cms.creditcard.model;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Config;

public abstract class CreditCardProcessor {
	
	final String creditCardDriver = "";
	
	//CreditCardInfo
	protected String creditCardNumber;	
	protected String creditCardName;
	protected String creditCardTypeName;
	protected String crditCardType;
	protected String creditCardCVV;
	protected Date creditCardExpirationDate;
	
	//Amounts
	protected float amount;
	protected float subtotal;
	protected float shipping;
	protected float tax;	
	protected float discount;
	
	//Billing Address	
	protected String billingFirstName;
	protected String billingLastName;
	protected String billingEmailAdress;
	protected String billingCompany;
	protected String billingStreet;
	protected String billingStreet2;
	protected String billingCity;
	protected String billingState;
	protected String billingZip;
	protected String billingCountry;
	protected String billingPhone;
	protected String billingFax;
	protected String billingMobile;
	protected boolean shipToBilling;
	
	//Shipping Address
	protected String shippingFirstName;
	protected String shippingLastName;
	protected String shippingEmailAdress;
	protected String shippingCompany;
	protected String shippingStreet;
	protected String shippingStreet2;
	protected String shippingCity;
	protected String shippingState;
	protected String shippingZip;
	protected String shippingCountry;
	protected String shippingPhone;
	protected String shippingFax;
	protected String shippingMobile;
	
	//Come from LinkPoint
	private HttpServletRequest request;
	private HttpServletResponse response;
	private String orderId;
	private boolean taxExempt;
	private String clientIPAddress;
	private String orderComments;
	
	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	//### GETTERS AND SETTERS ###
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

	public String getCreditCardDriver() {
		return creditCardDriver;
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

	public String getCreditCardCVV() {
		return creditCardCVV;
	}

	public void setCreditCardCVV(String cvv) {
		creditCardCVV = cvv;
	}

	public Date getCreditCardExpirationDate() {
		return creditCardExpirationDate;
	}

	public void setCreditCardSExpirationDate(Date expirationDate) {
		this.creditCardExpirationDate = expirationDate;
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
	//### END START GETTERS AND SETTERS ###

	public CreditCardProcessor()
	{	
	}
	
	public static CreditCardProcessor getInstance() throws Exception
	{
		String CCProcessorClass = Config.getStringProperty("CCProcessorClass");
		CreditCardProcessor ccProcesor = null;
		

		try{
			Class proc =  Class.forName(CCProcessorClass);
			ccProcesor=	(CreditCardProcessor) proc.newInstance();
		
		}
		catch(IllegalAccessException iae){
			throw new Exception("there is no processor selected in the properties file");
		}
		catch (InstantiationException ie) {
			
			throw new Exception("there is no processor selected in the properties file");
		}

			

		return ccProcesor;
	}
	
	

	public float getShipping() {
		return shipping;
	}

	public void setShipping(float shipping) {
		this.shipping = shipping;
	}

	public float getSubtotal() {
		return subtotal;
	}

	public void setSubtotal(float subtotal) {
		this.subtotal = subtotal;
	}

	public float getTax() {
		return tax;
	}

	public void setTax(float tax) {
		this.tax = tax;
	}

	public void setCreditCardExpirationDate(Date creditCardExpirationDate) {
		this.creditCardExpirationDate = creditCardExpirationDate;
	}
	
	public abstract CreditCardProcessorResponse process() throws CreditCardProcessorException;	

	public String getShippingStreet2() {
		return shippingStreet2;
	}

	public void setShippingStreet2(String shippingStreet2) {
		this.shippingStreet2 = shippingStreet2;
	}

	public String getBillingFax() {
		return billingFax;
	}

	public void setBillingFax(String billingFax) {
		this.billingFax = billingFax;
	}

	public String getBillingMobile() {
		return billingMobile;
	}

	public void setBillingMobile(String billingMobile) {
		this.billingMobile = billingMobile;
	}

	public String getBillingStreet2() {
		return billingStreet2;
	}

	public void setBillingStreet2(String billingStreet2) {
		this.billingStreet2 = billingStreet2;
	}

	public String getShippingFax() {
		return shippingFax;
	}

	public void setShippingFax(String shippingFax) {
		this.shippingFax = shippingFax;
	}

	public String getShippingMobile() {
		return shippingMobile;
	}

	public void setShippingMobile(String shippingMobile) {
		this.shippingMobile = shippingMobile;
	}

	public float getDiscount() {
		return discount;
	}

	public void setDiscount(float discount) {
		this.discount = discount;
	}

	public String getClientIPAddress() {
		return clientIPAddress;
	}

	public void setClientIPAddress(String clientIPAddress) {
		this.clientIPAddress = clientIPAddress;
	}

	public String getOrderComments() {
		return orderComments;
	}

	public void setOrderComments(String orderComments) {
		this.orderComments = orderComments;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public boolean isTaxExempt() {
		return taxExempt;
	}

	public void setTaxExempt(boolean taxExempt) {
		this.taxExempt = taxExempt;
	}
}
