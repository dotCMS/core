/*
 * Created on Jun 24, 2004
 *
 */
package com.dotmarketing.util;

import javax.portlet.ActionRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.liferay.portlet.ActionRequestImpl;


/**
 * @author Maria
 *
 */
public class Validator {
	public static boolean validate(ActionRequest request, ActionForm form, ActionMapping mapping) {
        if ((request == null) || (form == null) || (mapping == null)) {
            return false;
        }

        ActionRequestImpl reqImpl = (ActionRequestImpl) request;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
        org.apache.struts.action.ActionErrors errors = form.validate(mapping, httpReq);

        if ((errors != null) && !errors.isEmpty()) {
            
            request.setAttribute(Globals.ERROR_KEY, errors);
            return false;
        }

        return true;
    }

    public static boolean validate(HttpServletRequest httpReq, ActionForm form, ActionMapping mapping) {
        if ((httpReq == null) || (form == null) || (mapping == null)) {
            return false;
        }

        org.apache.struts.action.ActionErrors errors = form.validate(mapping, httpReq);

        if ((errors != null) && !errors.isEmpty()) {
            
        	httpReq.setAttribute(Globals.ERROR_KEY, errors);
            return false;
        }

        return true;
    }
}
