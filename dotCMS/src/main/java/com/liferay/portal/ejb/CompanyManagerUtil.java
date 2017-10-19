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

package com.liferay.portal.ejb;

import java.util.TimeZone;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.CacheLocator;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Company;

/**
 * <a href="CompanyManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.98 $
 *
 */
public class CompanyManagerUtil {

	@CloseDBIfOpened
	public static com.liferay.portal.model.Company getCompany()
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			CompanyManager companyManager = CompanyManagerFactory.getManager();

			return companyManager.getCompany();
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	@CloseDBIfOpened
	public static com.liferay.portal.model.Company getCompany(
		java.lang.String companyId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			CompanyManager companyManager = CompanyManagerFactory.getManager();

			return companyManager.getCompany(companyId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	@CloseDBIfOpened
	public static java.util.List getUsers()
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			CompanyManager companyManager = CompanyManagerFactory.getManager();

			return companyManager.getUsers();
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Company updateCompany(
		java.lang.String portalURL, java.lang.String homeURL,
		java.lang.String mx, java.lang.String name, java.lang.String shortName,
		java.lang.String type, java.lang.String size, java.lang.String street,
		java.lang.String city, java.lang.String state, java.lang.String zip,
		java.lang.String phone, java.lang.String fax,
		java.lang.String emailAddress, java.lang.String authType,
		boolean autoLogin, boolean strangers)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			CompanyManager companyManager = CompanyManagerFactory.getManager();

			return companyManager.updateCompany(portalURL, homeURL, mx, name,
				shortName, type, size, street, city, state, zip, phone, fax,
				emailAddress, authType, autoLogin, strangers);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Company updateCompany(Company c)throws com.liferay.portal.SystemException{
		try {
			CompanyManager companyManager = CompanyManagerFactory.getManager();

			return companyManager.updateCompany(c);
		}catch (com.liferay.portal.SystemException se) {
			throw se;
		}
	}

	public static void updateDefaultUser(java.lang.String languageId,
		java.lang.String timeZoneId, java.lang.String skinId,
		boolean dottedSkins, boolean roundedSkins, java.lang.String resolution)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			CompanyManager companyManager = CompanyManagerFactory.getManager();
			companyManager.updateDefaultUser(languageId, timeZoneId, skinId,
				dottedSkins, roundedSkins, resolution);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void updateLogo(java.io.File file)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			CompanyManager companyManager = CompanyManagerFactory.getManager();
			companyManager.updateLogo(file);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
	
    /**
     * 
     * @param languageId
     * @param timeZoneId
     * @param skinId
     * @param dottedSkins
     * @param roundedSkins
     * @param resolution
     * @throws PortalException
     * @throws SystemException
     * @throws com.dotmarketing.exception.DotRuntimeException
     */
	public static void updateUsers(java.lang.String languageId,
			java.lang.String timeZoneId, java.lang.String skinId,
			boolean dottedSkins, boolean roundedSkins, java.lang.String resolution)
	throws PortalException, SystemException,com.dotmarketing.exception.DotRuntimeException {
		try{		
			CompanyManager companyManager = CompanyManagerFactory.getManager();
			//companyManager.updateUsers(languageId, timeZoneId, skinId, dottedSkins, roundedSkins, resolution);
			companyManager.updateDefaultUser(languageId, timeZoneId, skinId, dottedSkins, roundedSkins, resolution);
			
			
			
			TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
			CacheLocator.getCacheAdministrator().flushGroup(CacheLocator.getUserCache().getPrimaryGroup());
			
		}catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
	

}