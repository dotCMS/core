package com.dotmarketing.portlets.calendar.action;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;

/**
 *
 * @author  David Torres
 * @version 1.0
 *
 */
public class ViewCalendarAction extends DotPortletAction {
	

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

		try {
			//gets the user
			User user = _getUser(req);
			
			String cmd = (req.getParameter(Constants.CMD)!=null)? req.getParameter(Constants.CMD) : Constants.EDIT;

			if ((cmd != null) /** ... && cmd.equals(Constants.DELETE) */) {
				/* ... */
			}

			_viewCalendar(req, user);

			if (req.getWindowState().equals(WindowState.NORMAL)) {
				return mapping.findForward("portlet.ext.calendar.view");
			}
			else {
				return mapping.findForward("portlet.ext.calendar.view_calendar");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	private void _viewCalendar(RenderRequest req, User user) throws PortalException, SystemException {


	}


}