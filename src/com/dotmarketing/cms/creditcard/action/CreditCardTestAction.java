package com.dotmarketing.cms.creditcard.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.cms.creditcard.struts.CreditCardTestForm;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class CreditCardTestAction extends DispatchAction
{
	public ActionForward processTestCC(ActionMapping mapping, ActionForm lf, HttpServletRequest request,HttpServletResponse response) throws Exception 
	{
		try
		{
		ActionForward af = null;
		CreditCardTestForm creditCardTestForm = (CreditCardTestForm) lf;
		ActionErrors ae = creditCardTestForm.validate(mapping,request);
		if(ae != null && ae.size() == 0)
		{		
			String typeOfPayment = creditCardTestForm.getTypeOfPayment();
			if(UtilMethods.isSet(typeOfPayment))
			{			
				if(typeOfPayment.equals("Authorize"))
				{
					Logger.warn(this,"Authorize");
					AuthorizeCreditCardTestAction action = new AuthorizeCreditCardTestAction();
					af = action.processTestCC(mapping, lf, request, response);
					Logger.warn(this,"Authorize: " + af.getName() + " - " + af.getPath());
				}
				else if(typeOfPayment.equals("LinkPoint"))
				{
					Logger.warn(this,"LinkPoint");
					LinkPointCreditCardTestAction action = new LinkPointCreditCardTestAction();
					af = action.processTestCC(mapping, lf, request, response);
					Logger.warn(this,"LinkPoint: " + af.getName() + " - " + af.getPath());
				}
				else if(typeOfPayment.equals("Verisign"))
				{
					Logger.warn(this,"Verisign");
					VerisignCreditCardTestAction action = new VerisignCreditCardTestAction();
					af = action.processTestCC(mapping, lf, request, response);
					Logger.warn(this,"Verisign: " + af.getName() + " - " + af.getPath());
				}	
			}
		}
		else
		{
			saveMessages(request, ae);
			af = mapping.findForward("failure");			
		}
		return af;
		}
		catch(Exception ex)
		{
			Logger.warn(this,ex.toString());
			throw ex;
		}
	}
}
