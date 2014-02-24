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

package com.liferay.portlet.myaccount.action;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.liferay.portal.AddressCellException;
import com.liferay.portal.AddressCityException;
import com.liferay.portal.AddressCountryException;
import com.liferay.portal.AddressDescriptionException;
import com.liferay.portal.AddressFaxException;
import com.liferay.portal.AddressPhoneException;
import com.liferay.portal.AddressStateException;
import com.liferay.portal.AddressStreetException;
import com.liferay.portal.AddressZipException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.AddressManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.ParamUtil;
import com.liferay.util.Validator;
import com.liferay.util.servlet.SessionErrors;

/**
 * <a href="EditAddressAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class EditAddressAction extends PortletAction {

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		String priority = req.getParameter("address_priority");

		if ((cmd != null) &&
			(cmd.equals(Constants.ADD) || cmd.equals(Constants.UPDATE)) &&
			(priority == null)) {

			try {
				_updateAddress(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof AddressCellException ||
					e instanceof AddressCityException ||
					e instanceof AddressCountryException ||
					e instanceof AddressDescriptionException ||
					e instanceof AddressFaxException ||
					e instanceof AddressPhoneException ||
					e instanceof AddressStateException ||
					e instanceof AddressStreetException ||
					e instanceof AddressZipException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.edit_address");
				}
				else if (e != null &&
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else if ((cmd != null) && (cmd.equals(Constants.DELETE))) {
			try {
				_deleteAddress(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else if ((cmd != null) && (cmd.equals(Constants.UPDATE)) &&
				 (priority != null)) {

			try {
				_updateAddressPriority(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else {
			setForward(req, "portlet.my_account.edit_address");
		}
	}

	private void _deleteAddress(ActionRequest req, ActionResponse res)
		throws Exception {

		String addressId = ParamUtil.getString(req, "address_id");

		AddressManagerUtil.deleteAddress(addressId);

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(ParamUtil.getString(req, "redirect")));
	}

	private void _updateAddress(ActionRequest req, ActionResponse res)
		throws Exception {

		String addressId = ParamUtil.getString(req, "address_id");

		String description = ParamUtil.getString(req, "address_desc");

		String street1 = ParamUtil.getString(req, "address_street_1");
		String street2 = ParamUtil.getString(req, "address_street_2");
		String city = ParamUtil.getString(req, "address_city");
		String state = ParamUtil.getString(req, "address_state");
		String zip = ParamUtil.getString(req, "address_zip");
		String country = ParamUtil.getString(req, "address_country");

		String phone = ParamUtil.getString(req, "address_phone");
		String fax = ParamUtil.getString(req, "address_fax");
		String cell = ParamUtil.getString(req, "address_cell");

		User user = PortalUtil.getSelectedUser(req);

		if (Validator.isNull(addressId)) {

			// Add address

			AddressManagerUtil.addAddress(
				user.getUserId(), User.class.getName(), user.getUserId(),
				description, street1, street2, city, state, zip, country, phone,
				fax, cell);
		}
		else {

			// Update address

			AddressManagerUtil.updateAddress(
				addressId, description, street1, street2, city, state, zip,
				country, phone, fax, cell);
		}

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(ParamUtil.getString(req, "redirect")));
	}

	private void _updateAddressPriority(ActionRequest req, ActionResponse res)
		throws Exception {

		String addressId = ParamUtil.getString(req, "address_id");
		boolean priority = ParamUtil.getBoolean(req, "address_priority");

		User user = PortalUtil.getSelectedUser(req);

		AddressManagerUtil.updateAddressPriority(
			User.class.getName(), user.getUserId(), addressId, priority);

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(ParamUtil.getString(req, "redirect")));
	}

}