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

package com.liferay.portal.upgrade;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyLocalManagerUtil;
import com.liferay.portal.ejb.ImageManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Image;
import com.liferay.portal.model.User;
import com.liferay.util.StringUtil;

/**
 * <a href="UpgradeProcess_Legacy.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class UpgradeProcess_Legacy implements UpgradeProcess {

	public void upgrade(int buildNumber) throws UpgradeException {

		// Version 3.1.0 has build number 1712

		if (buildNumber > 1712) {
			return;
		}

		_log.debug("Upgrading");

		try {

			// Counter names renamed from *com.liferay.ejb.* to
			// *com.liferay.model.* as of 1.9.0

			_updateCounter();

			// Default users depend on General Guest's group pages as of 2.1.0

			_updateDefaultUser();

			// Image names lengthened as of 2.2.0

			_updateImage();
		}
		catch (PortalException pe) {
			throw new UpgradeException(pe.getCause());
		}
		catch (SystemException se) {
			throw new UpgradeException(se.getCause());
		}
	}

	private void _updateCounter() throws SystemException {
		Iterator itr = CounterManagerUtil.getNames().iterator();

		while (itr.hasNext()) {
			String name = (String)itr.next();

			int pos = name.indexOf(".ejb.");

			if (name.startsWith("com.liferay.") && pos != -1) {
				CounterManagerUtil.rename(
					name,
					StringUtil.replace(name, ".ejb.", ".model."));
			}
		}
	}

	private void _updateDefaultUser() throws PortalException, SystemException {
		Iterator itr = CompanyLocalManagerUtil.getCompanies().iterator();

		while (itr.hasNext()) {
			Company company = (Company)itr.next();

//			try {
////				Group generalGroup = GroupLocalManagerUtil.getGroupByName(
////					company.getCompanyId(), "General");
////
////				GroupLocalManagerUtil.deleteGroup(generalGroup.getGroupId());
//			}
//			catch (NoSuchGroupException nsge) {
//			}
			User u = new User();
			try {
				 u = APILocator.getUserAPI().getSystemUser();
			} catch (DotDataException e) {
				Logger.debug(UpgradeProcess_Legacy.class,"DotDataException: " + e.getMessage(),e);
			}
//			UserLocalManagerUtil.setLayouts(u.getUserId(), new String[0]);
		}
	}

	private void _updateImage() throws PortalException, SystemException {

		// Image Gallery

		Iterator itr = ImageManagerUtil.getImageIds("%.ig.%").iterator();

		while (itr.hasNext()) {
			String oldImageId = (String)itr.next();

			String newImageId = StringUtil.replace(
				oldImageId, ".ig.", ".image_gallery.");

			if (newImageId.endsWith(".tn")) {
				newImageId = newImageId.substring(
					0, newImageId.length() - 2) + "small";
			}
			else {
				newImageId = newImageId + ".large";
			}

			Image image = ImageManagerUtil.getImage(oldImageId);

			ImageManagerUtil.updateImage(newImageId, image.getTextObj());
			ImageManagerUtil.deleteImage(oldImageId);
		}

		// Shopping Item

		itr = ImageManagerUtil.getImageIds("%.s.%").iterator();

		while (itr.hasNext()) {
			String oldImageId = (String)itr.next();

			String newImageId = StringUtil.replace(
				oldImageId, ".s.", ".shopping.item.");

			Image image = ImageManagerUtil.getImage(oldImageId);

			ImageManagerUtil.updateImage(newImageId, image.getTextObj());
			ImageManagerUtil.deleteImage(oldImageId);
		}
	}

	private static final Log _log =
		LogFactory.getLog(UpgradeProcess_Legacy.class);

}