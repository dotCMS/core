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

package com.liferay.portal.action;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.jdom.Document;
import org.jdom.Element;

import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.CompanyLocalManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.oroad.stxx.action.Action;

/**
 * <a href="TestStxxAction.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 * 
 */
public class TestStxxAction extends Action {

	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest req, HttpServletResponse res) throws Exception {

		Document document = new Document(new Element("test"));

		Element usersEl = new Element("users");

		Iterator itr = CompanyLocalManagerUtil.getUsers(
				PortalUtil.getCompanyId(req)).iterator();

		while (itr.hasNext()) {
			User user = (User) itr.next();

			Element userEl = new Element("user");

			userEl.addContent(new Element("full-name").setText(user
					.getFullName()));

			userEl.addContent(new Element("email-address").setText(user
					.getEmailAddress()));

			usersEl.addContent(userEl);
		}

		document.getRootElement().addContent(usersEl);

		Logger.info(this, document.toString());

		saveDocument(req, document);

		return mapping.findForward("portal.test_stxx");
	}

}