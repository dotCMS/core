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

package com.liferay.portlet.polls.ejb;

import com.dotmarketing.business.APILocator;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.PrincipalBean;
import com.liferay.portlet.polls.NoSuchDisplayException;
import com.liferay.portlet.polls.model.PollsDisplay;

/**
 * <a href="PollsDisplayManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class PollsDisplayManagerImpl
	extends PrincipalBean implements PollsDisplayManager {

	// Business methods

	public PollsDisplay getDisplay(String portletId)
		throws PortalException, SystemException {

		PollsDisplayPK pk = new PollsDisplayPK(getUserId(), portletId);

		return PollsDisplayUtil.findByPrimaryKey(pk);
	}

	public PollsDisplay updateDisplay(String portletId, String questionId)
		throws PortalException, SystemException {

		PollsDisplayPK pk = new PollsDisplayPK(getUserId(), portletId);

		PollsDisplay display = null;

		try {
			display = PollsDisplayUtil.findByPrimaryKey(pk);
		}
		catch (NoSuchDisplayException nsde) {
			display = PollsDisplayUtil.create(pk);
		}

		display.setQuestionId(questionId);

		PollsDisplayUtil.update(display);

		return display;
	}

	// Permission methods

	public boolean hasAdmin() throws PortalException, SystemException {
		String userId = null;

		try {
			userId = getUserId();
		}
		catch (PrincipalException pe) {
			return false;
		}

		try {
			if (APILocator.getRoleAPI().doesUserHaveRole(APILocator.getUserAPI().loadUserById(userId, APILocator.getUserAPI().getSystemUser(), true), APILocator.getRoleAPI().loadRoleByKey("Polls Admin"))) {
				return true;
			}
			else {
				return false;
			}
		} catch (Exception e) {
			throw new PortalException(e);
		}
	}

}