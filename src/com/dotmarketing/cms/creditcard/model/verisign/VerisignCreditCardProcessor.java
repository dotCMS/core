package com.dotmarketing.cms.creditcard.model.verisign;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.Verisign.payment.PFProAPI;
import com.dotmarketing.cms.creditcard.model.CreditCardProcessor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;

public class VerisignCreditCardProcessor extends CreditCardProcessor{
	
	//Place the default values
	static String HostAddress = "test-payflow.verisign.com";
	static Integer HostPort = Integer.decode("443");
	static String ParmList = "";
	static Integer Timeout = Integer.decode("30");
	static String ProxyAddress = "";
	static Integer ProxyPort = Integer.decode("0");
	static String ProxyLogon = "";
	static String ProxyPassword = "";
	
	//Verisign Account Information
	String partner = "";		
	String user = "";
	String vendor = "";
	String password = "";
	
	//Type Transaction
	private String tender = "C";
	private String trxtype = "S";
	
	public String getPartner() {
		return partner;
	}
	public void setPartner(String partner) {
		this.partner = partner;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getTender() {
		return tender;
	}
	public void setTender(String tender) {
		this.tender = tender;
	}
	public String getTrxtype() {
		return trxtype;
	}
	public void setTrxtype(String trxtype) {
		this.trxtype = trxtype;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getVendor() {
		return vendor;
	}
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	
	public VerisignCreditCardProcessor()
	{
		partner = Config.getStringProperty("verisign.partner");		
		user = Config.getStringProperty("verisign.user");
		vendor = Config.getStringProperty("verisign.vendor");
		password = Config.getStringProperty("verisign.password");
	}
	
	public VerisignCreditCardProcessorResponse process()
	{			
		PFProAPI pn = new PFProAPI();		

		//Set the certificate path
		//pn.SetCertPath("/wwwroot/dotcms.org/");
		pn.SetCertPath("C:\\svnroot\\wwwroot\\dotcms.org\\");

		//Create the Context
		pn.CreateContext(HostAddress,
				HostPort.intValue(),
				Timeout.intValue(),
				ProxyAddress,
				ProxyPort.intValue(),
				ProxyLogon,
				ProxyPassword);

		//Add the transaction Values
		String paramList = getParmList();
		String rc = pn.SubmitTransaction(paramList);
		
		//Logger.info("rc='" + rc + "'\n\n");

		//Procces the Response
		VerisignCreditCardProcessorResponse vsccrb = new VerisignCreditCardProcessorResponse();
		vsccrb.setResponse(rc);
		String[] splitRC = rc.split("&");
		int size = splitRC.length;
		for(int i = 0; i < size; i++)
		{
			String variable = splitRC[i];
			String variableName = variable.split("=")[0]; 
			String variableValue = variable.split("=")[1];
			if(variableName.equals("RESULT"))
			{
				vsccrb.setRESULT(variableValue);
			}
			else if(variableName.equals("PNREF"))
			{
				vsccrb.setPNREF(variableValue);
			}
			else if(variableName.equals("RESPMSG"))
			{
				vsccrb.setRESPMSG(variableValue);
			}
			else if(variableName.equals("AUTHCODE"))
			{
				vsccrb.setAUTHCODE(variableValue);
			}
			else if(variableName.equals("AVSADDR"))
			{
				vsccrb.setAVSADDR(variableValue);
			}
			else if(variableName.equals("AVSZIP"))
			{
				vsccrb.setAVSZIP(variableValue);
			}
			else if(variableName.equals("CVV2MATCH"))
			{
				vsccrb.setCVV2MATCH(variableValue);
			}
			else if(variableName.equals("PREFPSMSG"))
			{
				vsccrb.setPREFPSMSG(variableValue);
			}
			else if(variableName.equals("POSTFPSMSG"))
			{
				vsccrb.setPOSTFPSMSG(variableValue);
			}
		}	
		return vsccrb;
	}
	
	private String getParmList()
	{
		StringBuffer sb = new StringBuffer();
		
		//AMT		
		NumberFormat decimalFormat = NumberFormat.getNumberInstance(Locale.US);		
		decimalFormat.setGroupingUsed(false);
		decimalFormat.setMaximumFractionDigits(2);		
		decimalFormat.setMinimumFractionDigits(2);
		String amountString = decimalFormat.format(amount);
		sb.append("AMT=" + amountString);
		
		//ACCT
		if (UtilMethods.isSet(getCreditCardNumber()))
			sb.append("&ACCT=" + getCreditCardNumber());
		
		//CVV2
		if (UtilMethods.isSet(getCreditCardCVV()))
			sb.append("&CVV2=" + getCreditCardCVV());
		
		//EXPDATE
		if (getCreditCardExpirationDate() != null) 
		{
			Date expirationdate = getCreditCardExpirationDate();
			SimpleDateFormat sdf = new SimpleDateFormat("MMyyyy");
			String expirationDateString = sdf.format(expirationdate);		
			sb.append("&EXPDATE=" + expirationDateString);
		}
		
		//NAME
		if (UtilMethods.isSet(getCreditCardName()))
			sb.append("&NAME=" + getCreditCardName());
		
		//PARTNER
		if (UtilMethods.isSet(partner))
			sb.append("&PARTNER=" + partner);
		
		//PWD
		if (UtilMethods.isSet(password))
			sb.append("&PWD=" + password);
		
		//STREET
		if (UtilMethods.isSet(billingStreet))
			sb.append("&STREET=" + billingStreet);
		
		//TENDER
		if (UtilMethods.isSet(tender))
			sb.append("&TENDER=" + tender);
		
		//TRXTYPE
		if (UtilMethods.isSet(trxtype))
			sb.append("&TRXTYPE=" + trxtype);
		
		//USER
		if (UtilMethods.isSet(user))
			sb.append("&USER=" + user);
		
		//VENDOR
		if (UtilMethods.isSet(vendor))
			sb.append("&VENDOR=" + vendor);
		
		//ZIP
		if (UtilMethods.isSet(billingZip))
			sb.append("&ZIP=" + billingZip);
		
		//ORIGID
		if (InodeUtils.isSet(getOrderId()))
			sb.append("&ORIGID=" + getOrderId());

		String paramList = sb.toString();
		
		//Logger.info("\n\nparamList='" + paramList + "'");
		
		//paramList = "TRXTYPE=S&TENDER=C&PARTNER=PayPal&VENDOR=SuperMerchant&USER=SuperMerchant&PWD=x1y2z3&ACCT=5555555555554444&EXPDATE=0308&AMT=123.00";
		return paramList;
	}

}
