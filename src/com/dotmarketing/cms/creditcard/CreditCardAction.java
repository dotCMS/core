package com.dotmarketing.cms.creditcard;

import java.util.GregorianCalendar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.cms.creditcard.form.CreditCardForm;
import com.dotmarketing.cms.creditcard.model.CreditCardProcessor;

public class CreditCardAction extends DispatchAction {
    public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
            HttpServletResponse response) throws Exception {    	
        ActionForward af = mapping.findForward("processCreditCard");
        return af;
    }

    public ActionForward processCreditCard(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        
    	CreditCardForm ccForm = (CreditCardForm) lf;
    	
    	CreditCardProcessor accp = CreditCardProcessor.getInstance(); 	    	
    	accp.setRequest(request);
    	accp.setResponse(response);
    	
    	GregorianCalendar gc = new GregorianCalendar();
    	int month = Integer.parseInt(ccForm.getCreditCardExpirationMonth());
    	int year = Integer.parseInt(ccForm.getCreditCardExpirationYear());    	
    	gc.set(GregorianCalendar.MONTH,month);
    	gc.set(GregorianCalendar.YEAR,year);
    	gc.set(GregorianCalendar.DATE,1);
    	    	
    	accp.setCreditCardNumber(ccForm.getCreditCardNumber());
    	accp.setCreditCardName(ccForm.getCreditCardName());
    	accp.setCreditCardSExpirationDate(gc.getTime());
    	accp.setAmount(ccForm.getAmount());
    	accp.setCreditCardCVV(ccForm.getCreditCardCVV());
    	accp.process();
            
        return unspecified(mapping, lf, request, response);
    }
}
