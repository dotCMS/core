package com.dotmarketing.cms.login.action;


import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.action.ActionMessage;
import com.dotcms.repackage.org.apache.struts.action.ActionMessages;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.cms.SecureAction;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.UtilMethods;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * <a href="LoginAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
@Deprecated
public class LogoutAction extends SecureAction {
    public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {

    		String referrer = request.getParameter("referrer");
    		
        	LoginFactory.doLogout(request, response);


            ActionMessages am = new ActionMessages();
            am.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.Logout.Successful"));
            request.getSession().setAttribute(Globals.MESSAGE_KEY, am);

            ActionForward af = null;
            if(UtilMethods.isSet(referrer)) {
            	af = new ActionForward(SecurityUtils.stripReferer(request, referrer));
            	af.setRedirect(true);
            } else
            	af = mapping.findForward("afterLogoutPage");
            return af;
    }
}
