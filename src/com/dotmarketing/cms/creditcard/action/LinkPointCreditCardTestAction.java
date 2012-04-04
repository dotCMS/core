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

import com.dotmarketing.cms.creditcard.model.linkpoint.LinkPointCreditCardProcessor;
import com.dotmarketing.cms.creditcard.model.linkpoint.LinkPointCreditCardProcessorResponse;
import com.dotmarketing.cms.creditcard.struts.CreditCardTestForm;
import com.dotmarketing.util.Config;

public class LinkPointCreditCardTestAction  extends DispatchAction {

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

			LinkPointCreditCardProcessor lpccp = new LinkPointCreditCardProcessor();
			lpccp.setOrderId(orderId);
			lpccp.setTaxExempt((orderForm.getOrderTax() != 0 ? false : true));
			lpccp.setClientIPAddress(request.getRemoteUser());
			lpccp.setDiscount(orderForm.getOrderDiscount());
			lpccp.setTax(orderForm.getOrderTax());
			lpccp.setShipping(orderForm.getOrderShipping());
			lpccp.setAmount(orderForm.getOrderTotal());
			lpccp.setCreditCardNumber(orderForm.getCardNumber());
			lpccp.setCreditCardExpirationDate(expirationDate);
			lpccp.setCreditCardCVV(orderForm.getCardVerificationValue());
			lpccp.setBillingFirstName(orderForm.getBillingFirstName());
			lpccp.setBillingLastName(orderForm.getBillingLastName());
			lpccp.setBillingStreet(orderForm.getBillingAddress1());
			lpccp.setBillingStreet2(orderForm.getBillingAddress2());
			lpccp.setBillingCity(orderForm.getBillingCity());
			lpccp.setBillingState(orderForm.getBillingState());
			lpccp.setBillingZip(orderForm.getBillingZip());
			lpccp.setBillingCountry(Config.getStringProperty("US_COUNTRY_CODE"));
			lpccp.setBillingPhone(orderForm.getBillingPhone());
			lpccp.setBillingEmailAdress(orderForm.getBillingEmail());
			lpccp.setOrderComments(Config.getStringProperty("WEB_EVENT_REGISTRATION_COMMENTS"));
			String storeID = Config.getStringProperty("LP_ECOMM_STORE_ID"); 			
			lpccp.setStoreId(storeID);
			LinkPointCreditCardProcessorResponse ccResponse = (LinkPointCreditCardProcessorResponse) lpccp.process();

			ActionMessages ae = new ActionMessages();
			if (ccResponse.orderApproved()) 
			{			
				ae.add(Globals.ERROR_KEY, new ActionMessage("error.cc_processing.card.approved"));
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
