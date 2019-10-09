/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.admin.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.admin.ejb.AdminConfigManagerUtil;
import com.liferay.portlet.admin.model.EmailConfig;
import com.liferay.portlet.admin.model.UserConfig;
import com.liferay.util.ParamUtil;
import com.liferay.util.StringUtil;
import com.liferay.util.servlet.SessionErrors;
import com.liferay.util.servlet.SessionMessages;
import javax.servlet.jsp.PageContext;

/**
 * <a href="UpdateUserConfigAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class UpdateUserConfigAction extends PortletAction {

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

		try {
			String cmd = ParamUtil.get(req, Constants.CMD, "dgar");

			String[] groupNames = StringUtil.split(
				ParamUtil.getString(req, "config_groups"));
			String[] roleNames = StringUtil.split(
				ParamUtil.getString(req, "config_roles"));
			String[] ruid = StringUtil.split(ParamUtil.getString(
				req, "config_ruid"), "\n");
			String[] ruea = StringUtil.split(ParamUtil.getString(
				req, "config_ruea"), "\n");
			String[] mailHostNames = StringUtil.split(ParamUtil.getString(
				req, "config_mhn"), "\n");

			boolean registrationEmailSend = ParamUtil.get(
				req, "config_re_send", true);
			String registrationEmailSubject = ParamUtil.getString(
				req, "config_re_subject");
			String registrationEmailBody = ParamUtil.getString(
				req, "config_re_body");

			UserConfig userConfig = AdminConfigManagerUtil.getUserConfig(
				PortalUtil.getCompanyId(req));

			if (cmd.equals("dgar")) {
				userConfig.setGroupNames(groupNames);
				userConfig.setRoleNames(roleNames);
			}
			else if (cmd.equals("den")) {
				userConfig.setRegistrationEmail(new EmailConfig(
					registrationEmailSend, registrationEmailSubject,
					registrationEmailBody));
			}
			else if (cmd.equals("ru")) {
				userConfig.setReservedUserIds(ruid);
				userConfig.setReservedUserEmailAddresses(ruea);
			}
			else if (cmd.equals("mhn")) {
				userConfig.setMailHostNames(mailHostNames);
			}

			// Update config

			AdminConfigManagerUtil.updateUserConfig(userConfig);

			// Session messages

			SessionMessages.add(req, UpdateUserConfigAction.class.getName());

			return mapping.findForward("portlet.admin.edit_user_config");
		}
		catch (Exception e) {
			if (e != null &&
				e instanceof PrincipalException) {

				SessionErrors.add(req, e.getClass().getName());

				return mapping.findForward("portlet.admin.error");
			}
			else {
				req.setAttribute(PageContext.EXCEPTION, e);

				return mapping.findForward(Constants.COMMON_ERROR);
			}
		}
	}

}