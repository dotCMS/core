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

import com.dotmarketing.cms.creditcard.model.CreditCardProcessorResponse;
import com.dotmarketing.cms.creditcard.model.authorize.AuthorizeCreditCardProcessor;
import com.dotmarketing.cms.creditcard.model.authorize.AuthorizeCreditCardResponse;
import com.dotmarketing.cms.creditcard.struts.CreditCardTestForm;

public class AuthorizeCreditCardTestAction extends DispatchAction {

	public ActionForward processTestCC(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) { 
		{
			try
			{
				//### SEND THE CREDIT CARD INFORMATION TO AUTHORIZE.NET ###
				ActionForward af;
				String returnCode = "0";
				CreditCardTestForm makeGiftForm = (CreditCardTestForm) lf;
				AuthorizeCreditCardProcessor authorizeCCP = new AuthorizeCreditCardProcessor();
				
				authorizeCCP.setRequest(request);
				authorizeCCP.setResponse(response);

				//Fill the data
				authorizeCCP.setBillingFirstName(makeGiftForm.getBillingFirstName());
				authorizeCCP.setBillingLastName(makeGiftForm.getBillingLastName());
				String street = makeGiftForm.getBillingAddress1().trim() + " " + makeGiftForm.getBillingAddress2().trim();
				authorizeCCP.setBillingStreet(street);
				authorizeCCP.setBillingCity(makeGiftForm.getBillingCity());
				authorizeCCP.setBillingState(makeGiftForm.getBillingState());
				authorizeCCP.setBillingCountry(makeGiftForm.getBillingCountry());
				authorizeCCP.setBillingZip(makeGiftForm.getBillingZip());
				authorizeCCP.setBillingPhone(makeGiftForm.getBillingPhone());
				authorizeCCP.setBillingEmailAdress(makeGiftForm.getBillingEmail());		

				Date expirationDate = new Date();
				GregorianCalendar gc = new GregorianCalendar();
				gc.set(Calendar.YEAR,makeGiftForm.getCardExpYear());
				gc.set(Calendar.MONTH,makeGiftForm.getCardExpMonth());
				gc.set(Calendar.DATE,gc.getActualMaximum(Calendar.DATE));
				expirationDate = gc.getTime();

				authorizeCCP.setCreditCardNumber(makeGiftForm.getCardNumber());
				authorizeCCP.setCreditCardName(makeGiftForm.getNameOnCard());
				authorizeCCP.setCreditCardSExpirationDate(expirationDate);
				authorizeCCP.setAmount(makeGiftForm.getOrderTotal());
				authorizeCCP.setCreditCardCVV(makeGiftForm.getCardVerificationValue());
				AuthorizeCreditCardResponse accr = authorizeCCP.process();

				returnCode = accr.getCode();				
				String URL = accr.getMessage();
				
				ActionMessages ae = new ActionMessages();
				if(returnCode.equals(CreditCardProcessorResponse.APPROVED))
				{
					ae.add(Globals.ERROR_KEY, new ActionMessage("error.cc_processing.card.approved"));
				}
				else
				{
					ae.add(Globals.ERROR_KEY, new ActionMessage("error.cc_processing.card.denied"));			
				}
				
				saveMessages(request, ae);
				af = new ActionForward(URL);
				return af;
			}
			catch(Exception ex)
			{
				String message = ex.getMessage();
				ActionForward af = new ActionForward("failure");
				return af;
			}			
		}
	}
}
