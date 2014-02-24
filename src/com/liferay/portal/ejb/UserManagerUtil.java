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

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.model.User;

/**
 * <a href="UserManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.109 $
 *
 */
public class UserManagerUtil {
	
	public static com.liferay.portal.model.User addUser(
		java.lang.String companyId, boolean autoUserId,
		java.lang.String userId, boolean autoPassword,
		java.lang.String password1, java.lang.String password2,
		boolean passwordReset, java.lang.String firstName,
		java.lang.String middleName, java.lang.String lastName,
		java.lang.String nickName, boolean male, java.util.Date birthday,
		java.lang.String emailAddress, java.util.Locale locale)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.addUser(companyId, autoUserId, userId,
				autoPassword, password1, password2, passwordReset, firstName,
				middleName, lastName, nickName, male, birthday, emailAddress,
				locale);
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

    public static int authenticateByEmailAddress ( java.lang.String companyId, java.lang.String emailAddress, java.lang.String password ) throws com.liferay.portal.PortalException, com.liferay.portal.SystemException {

        try {

            //Search for the system user
            User systemUser = APILocator.getUserAPI().getSystemUser();

            //Verify that the System User is not been use to log in inside the system
            if ( systemUser.getEmailAddress().equalsIgnoreCase( emailAddress ) ) {
                SecurityLogger.logInfo( UserManagerUtil.class, "An invalid attempt to login as a System User has been made  - you cannot login as the System User" );
                throw new AuthException( "Unable to login as System User - you cannot login as the System User." );
            }

            UserManager userManager = UserManagerFactory.getManager();
            return userManager.authenticateByEmailAddress( companyId, emailAddress, password );
        } catch ( com.liferay.portal.PortalException pe ) {
            throw pe;
        } catch ( com.liferay.portal.SystemException se ) {
            throw se;
        } catch ( Exception e ) {
            throw new com.liferay.portal.SystemException( e );
        }
    }

    public static int authenticateByUserId ( java.lang.String companyId, java.lang.String userId, java.lang.String password ) throws com.liferay.portal.PortalException, com.liferay.portal.SystemException {

        try {

            //Search for the system user
            User systemUser = APILocator.getUserAPI().getSystemUser();

            //Verify that the System User is not been use to log in inside the system
            if ( systemUser.getUserId().equalsIgnoreCase( userId ) ) {
                SecurityLogger.logInfo( UserManagerUtil.class, "An invalid attempt to login as a System User has been made  - you cannot login as the System User" );
                throw new AuthException( "Unable to login as System User - you cannot login as the System User." );
            }

            UserManager userManager = UserManagerFactory.getManager();
            return userManager.authenticateByUserId( companyId, userId, password );
        } catch ( com.liferay.portal.PortalException pe ) {
            throw pe;
        } catch ( com.liferay.portal.SystemException se ) {
            throw se;
        } catch ( Exception e ) {
            throw new com.liferay.portal.SystemException( e );
        }
    }

	public static com.liferay.util.KeyValuePair decryptUserId(
		java.lang.String companyId, java.lang.String userId,
		java.lang.String password)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.decryptUserId(companyId, userId, password);
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

	public static void deleteUser(java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();
			userManager.deleteUser(userId);
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

	public static java.lang.String encryptUserId(java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.encryptUserId(userId);
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

	public static java.util.List findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(
		java.lang.String firstName, java.lang.String middleName,
		java.lang.String lastName, java.lang.String emailAddress,
		java.lang.Boolean male, java.util.Date age1, java.util.Date age2,
		java.lang.String im, java.lang.String street1,
		java.lang.String street2, java.lang.String city,
		java.lang.String state, java.lang.String zip, java.lang.String phone,
		java.lang.String fax, java.lang.String cell)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(firstName,
				middleName, lastName, emailAddress, male, age1, age2, im,
				street1, street2, city, state, zip, phone, fax, cell);
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

	public static java.util.List findByC_SMS()
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.findByC_SMS();
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

	public static java.util.List findByOr_C_FN_MN_LN_EA_M_BD_IM_A(
		java.lang.String firstName, java.lang.String middleName,
		java.lang.String lastName, java.lang.String emailAddress,
		java.lang.Boolean male, java.util.Date age1, java.util.Date age2,
		java.lang.String im, java.lang.String street1,
		java.lang.String street2, java.lang.String city,
		java.lang.String state, java.lang.String zip, java.lang.String phone,
		java.lang.String fax, java.lang.String cell)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.findByOr_C_FN_MN_LN_EA_M_BD_IM_A(firstName,
				middleName, lastName, emailAddress, male, age1, age2, im,
				street1, street2, city, state, zip, phone, fax, cell);
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

	public static java.lang.String getCompanyId(java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.getCompanyId(userId);
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

	public static com.liferay.portal.model.User getDefaultUser(
		java.lang.String companyId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.getDefaultUser(companyId);
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

	public static com.liferay.portal.model.User getUserByEmailAddress(
		java.lang.String emailAddress)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.getUserByEmailAddress(emailAddress);
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

	public static com.liferay.portal.model.User getUserById(
		java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.getUserById(userId);
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

	public static com.liferay.portal.model.User getUserById(
		java.lang.String companyId, java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.getUserById(companyId, userId);
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

	public static java.lang.String getUserId(java.lang.String companyId,
		java.lang.String emailAddress)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.getUserId(companyId, emailAddress);
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

	public static int notifyNewUsers()
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.notifyNewUsers();
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

	
	public static void sendPassword(java.lang.String companyId,
		java.lang.String emailAddress)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();
			userManager.sendPassword(companyId, emailAddress);
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

	public static void test() throws com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();
			userManager.test();
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}


	public static com.liferay.portal.model.User updateActive(
		java.lang.String userId, boolean active)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.updateActive(userId, active);
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

	public static com.liferay.portal.model.User updateAgreedToTermsOfUse(
		boolean agreedToTermsOfUse)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.updateAgreedToTermsOfUse(agreedToTermsOfUse);
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

	public static com.liferay.portal.model.User updateLastLogin(
		java.lang.String loginIP)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.updateLastLogin(loginIP);
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

	public static void updatePortrait(java.lang.String userId, byte[] bytes)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();
			userManager.updatePortrait(userId, bytes);
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

	public static com.liferay.portal.model.User updateUser(
		java.lang.String userId, java.lang.String password1,
		java.lang.String password2, boolean passwordReset)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.updateUser(userId, password1, password2,
				passwordReset);
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

	public static com.liferay.portal.model.User updateUser(
		java.lang.String userId, java.lang.String password,
		java.lang.String firstName, java.lang.String middleName,
		java.lang.String lastName, java.lang.String nickName, boolean male,
		java.util.Date birthday, java.lang.String emailAddress,
		java.lang.String smsId, java.lang.String aimId, java.lang.String icqId,
		java.lang.String msnId, java.lang.String ymId,
		java.lang.String favoriteActivity, java.lang.String favoriteBibleVerse,
		java.lang.String favoriteFood, java.lang.String favoriteMovie,
		java.lang.String favoriteMusic, java.lang.String languageId,
		java.lang.String timeZoneId, java.lang.String skinId,
		boolean dottedSkins, boolean roundedSkins, java.lang.String greeting,
		java.lang.String resolution, java.lang.String refreshRate,
		java.lang.String comments)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.updateUser(userId, password, firstName,
				middleName, lastName, nickName, male, birthday, emailAddress,
				smsId, aimId, icqId, msnId, ymId, favoriteActivity,
				favoriteBibleVerse, favoriteFood, favoriteMovie, favoriteMusic,
				languageId, timeZoneId, skinId, dottedSkins, roundedSkins,
				greeting, resolution, refreshRate, comments);
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

	public static boolean hasAdmin(java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserManager userManager = UserManagerFactory.getManager();

			return userManager.hasAdmin(userId);
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
}