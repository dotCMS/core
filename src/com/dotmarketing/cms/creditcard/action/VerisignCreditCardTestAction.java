package com.dotmarketing.cms.creditcard.action;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.cms.creditcard.model.verisign.VerisignCreditCardProcessor;
import com.dotmarketing.cms.creditcard.model.verisign.VerisignCreditCardProcessorResponse;
import com.dotmarketing.cms.creditcard.struts.CreditCardTestForm;
import com.dotmarketing.util.Config;

public class VerisignCreditCardTestAction extends DispatchAction {
	
	public ActionForward processTestCC(ActionMapping mapping, ActionForm lf, HttpServletRequest request,HttpServletResponse response)  
	{
		try
		{
			CreditCardTestForm orderForm = (CreditCardTestForm) lf;
			Date expirationDate = new Date();
			GregorianCalendar gc = new GregorianCalendar();
			gc.set(Calendar.YEAR,orderForm.getCardExpYear());
			gc.set(Calendar.MONTH,orderForm.getCardExpMonth());
			gc.set(Calendar.DATE,gc.getActualMaximum(Calendar.DATE));
			expirationDate = gc.getTime();

			String orderId = "1"; //for testing purpose

			VerisignCreditCardProcessor vsccp = new VerisignCreditCardProcessor();
			vsccp.setOrderId(orderId);			
			vsccp.setClientIPAddress(request.getRemoteUser());
			vsccp.setAmount(orderForm.getOrderTotal());
			vsccp.setCreditCardNumber(orderForm.getCardNumber());
			vsccp.setCreditCardExpirationDate(expirationDate);
			vsccp.setCreditCardCVV(orderForm.getCardVerificationValue());
			vsccp.setBillingFirstName(orderForm.getBillingFirstName());
			vsccp.setBillingLastName(orderForm.getBillingLastName());
			vsccp.setBillingStreet(orderForm.getBillingAddress1());
			vsccp.setBillingStreet2(orderForm.getBillingAddress2());
			vsccp.setBillingCity(orderForm.getBillingCity());
			vsccp.setBillingState(orderForm.getBillingState());
			vsccp.setBillingZip(orderForm.getBillingZip());
			vsccp.setBillingCountry(Config.getStringProperty("US_COUNTRY_CODE"));
			vsccp.setBillingPhone(orderForm.getBillingPhone());
			vsccp.setBillingEmailAdress(orderForm.getBillingEmail());
			vsccp.setOrderComments(Config.getStringProperty("WEB_EVENT_REGISTRATION_COMMENTS"));
			 					
			VerisignCreditCardProcessorResponse ccResponse = (VerisignCreditCardProcessorResponse) vsccp.process();
			String ccResponseString = ccResponse.getResponse(); 
			
			
			ActionMessages ae = new ActionMessages();
			if (ccResponse.orderApproved()) 
			{			
				ae.add(Globals.ERROR_KEY, new ActionMessage("error.cc_processing.card.approved",ccResponseString));
			}		
			else
			{
				ae.add(Globals.ERROR_KEY, new ActionMessage("error.cc_processing.card.denied", ccResponse.getError()));
			}
			saveMessages(request,ae);
			ActionForward af = mapping.findForward("success");
			return af;
		}
		catch(Exception ex)
		{
			String message = ex.toString();
			ActionForward af = mapping.findForward("failure");
			return af;
		}
	}

}
