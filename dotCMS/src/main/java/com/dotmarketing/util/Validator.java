/*
 * Created on Jun 24, 2004
 *
 */
package com.dotmarketing.util;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.liferay.portlet.ActionRequestImpl;
import javax.servlet.http.HttpServletRequest;

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
        ActionErrors errors = form.validate(mapping, httpReq);

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

        ActionErrors errors = form.validate(mapping, httpReq);

        if ((errors != null) && !errors.isEmpty()) {
            
        	httpReq.setAttribute(Globals.ERROR_KEY, errors);
            return false;
        }

        return true;
    }
}
