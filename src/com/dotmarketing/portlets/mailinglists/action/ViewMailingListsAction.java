/**
 * Copyright (c) 2000-2004 Liferay, LLC. All rights reserved.
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

package com.dotmarketing.portlets.mailinglists.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.jsp.PageContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.userfilter.factories.UserFilterFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class ViewMailingListsAction extends PortletAction {

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

	    Logger.debug(this, "Running ViewMailingListsAction");
	    
		try {
			//get the user, order, direction
			User user = com.liferay.portal.util.PortalUtil.getUser(req);
			String orderBy = req.getParameter("orderby");
			String direction  = req.getParameter("direction");
			String condition = req.getParameter("query");

			//get their lists
			List list = null;
			List roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
			boolean isMarketingAdmin = false;
			
			Iterator rolesIt = roles.iterator();
			while (rolesIt.hasNext()) {
			    Role role = (Role) rolesIt.next();
			    if (UtilMethods.isSet(role.getRoleKey()) && role.getRoleKey().equals(Config.getStringProperty("MAILINGLISTS_ADMIN_ROLE"))) {
			        isMarketingAdmin = true;
			        break;
			    }
			}
			
			if (isMarketingAdmin) {
				if (UtilMethods.isSet(orderBy) && UtilMethods.isSet(direction)) {
					//list = MailingListFactory.getAllMailingLists(orderBy, direction);
					list = MailingListFactory.getAllMailingLists();
					list.addAll(UserFilterFactory.getAllUserFilter());
					
					if (orderBy.equals("title")) {
						if (direction.equals(" asc"))
							Collections.sort(list, new ComparatorTitleAsc());
						else
							Collections.sort(list, new ComparatorTitleDesc());
					}
				} else if(UtilMethods.isSet(condition)) {
					list = MailingListFactory.getAllMailingListsCondition(condition);
					list.addAll(UserFilterFactory.getUserFilterByTitle(condition));
					Collections.sort(list, new ComparatorTitleAsc());
				} else {
					list = MailingListFactory.getAllMailingLists();
					list.addAll(UserFilterFactory.getAllUserFilter());
					Collections.sort(list, new ComparatorTitleAsc());
				}
			} else {
				if (UtilMethods.isSet(orderBy) && UtilMethods.isSet(direction)) {
					//list = MailingListFactory.getMailingListsByUser(user, orderBy, direction);
					list = MailingListFactory.getMailingListsByUser(user);
					list.add(MailingListFactory.getUnsubscribersMailingList());
					list.addAll(UserFilterFactory.getAllUserFilterByUser(user));
					
					if (orderBy.equals("title")) {
						if (direction.equals(" asc"))
							Collections.sort(list, new ComparatorTitleAsc());
						else
							Collections.sort(list, new ComparatorTitleDesc());
					}
				} else if(UtilMethods.isSet(condition)) {
					list = MailingListFactory.getMailingListsByUserCondition(user, condition);
					list.add(MailingListFactory.getUnsubscribersMailingList());
					list.addAll(UserFilterFactory.getUserFilterByTitleAndUser(condition, user));
					Collections.sort(list, new ComparatorTitleAsc());
				} else {
					list = MailingListFactory.getMailingListsByUser(user);
					list.add(MailingListFactory.getUnsubscribersMailingList());
					list.addAll(UserFilterFactory.getAllUserFilterByUser(user));
					Collections.sort(list, new ComparatorTitleAsc());
				}
			}
			
			if (req.getWindowState().equals(WindowState.NORMAL)) {
//				if (list != null)
//					list = orderMailingListByDescDate(list);
				req.setAttribute(WebKeys.MAILING_LIST_VIEW_PORTLET, list);
				return mapping.findForward("portlet.ext.mailinglists.view");
			}
			else {
				req.setAttribute(WebKeys.MAILING_LIST_VIEW, list);
				return mapping.findForward("portlet.ext.mailinglists.view_mailinglists");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
	
	private List<MailingList> orderMailingListByDescDate(List<MailingList> list) {
		List<MailingList> result = new ArrayList<MailingList>(list.size());
		
		int i;
		boolean added;
		MailingList mailingList2;
		
		for (MailingList mailingList1: list) {
			if (result.size() == 0) {
				result.add(mailingList1);
			} else {
				added = false;
				for (i = 0; i < result.size(); ++i) {
					mailingList2 = result.get(i);
					if (mailingList2.getIDate().before(mailingList1.getIDate())) {
						result.add(i, mailingList1);
						added = true;
						break;
					}
				}
				
				if (!added)
					result.add(mailingList1);
			}
		}
		
		return result;
	}
	
	private class ComparatorTitleAsc implements Comparator {

		public int compare(Object o1, Object o2) {
			String title1, title2;
			
			try {
				if (o1 instanceof MailingList)
					title1 = BeanUtils.getProperty(o1, "title");
				else
					title1 = BeanUtils.getProperty(o1, "userFilterTitle");
			} catch (Exception e) {
				title1 = "";
			}
			
			try {
				if (o2 instanceof MailingList)
					title2 = BeanUtils.getProperty(o2, "title");
				else
					title2 = BeanUtils.getProperty(o2, "userFilterTitle");
			} catch (Exception e) {
				title2 = "";
			}
			
			return title1.compareToIgnoreCase(title2);
		}
	}
	
	private class ComparatorTitleDesc implements Comparator {

		public int compare(Object o1, Object o2) {
			String title1, title2;
			
			try {
				if (o1 instanceof MailingList)
					title1 = BeanUtils.getProperty(o1, "title");
				else
					title1 = BeanUtils.getProperty(o1, "userFilterTitle");
			} catch (Exception e) {
				title1 = "";
			}
			
			try {
				if (o2 instanceof MailingList)
					title2 = BeanUtils.getProperty(o2, "title");
				else
					title2 = BeanUtils.getProperty(o2, "userFilterTitle");
			} catch (Exception e) {
				title2 = "";
			}
			
			return title2.compareToIgnoreCase(title1);
		}
	}
}