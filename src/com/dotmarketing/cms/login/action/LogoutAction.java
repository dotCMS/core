package com.dotmarketing.cms.login.action;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.UtilMethods;


/**
 * <a href="LoginAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */


public class LogoutAction extends DispatchAction {
    public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

    		String referrer = request.getParameter("referrer");
    		
        	LoginFactory.doLogout(request, response);


            ActionMessages am = new ActionMessages();
            am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.Logout.Successful"));
            request.getSession().setAttribute(Globals.MESSAGE_KEY, am);

            ActionForward af = null;
            if(UtilMethods.isSet(referrer)) {
            	af = new ActionForward(SecurityUtils.stripReferer(referrer));
            	af.setRedirect(true);
            } else
            	af = mapping.findForward("afterLogoutPage");
            return af;
    }
}
