package com.dotcms.company;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * Encapsulates company useful functionality such as:
 * <ul>
 *     <li>Get default company</li>
 *     <li>Get a company</li>
 *     <li>Get a company users</li>
 *     <li>Update company info</li>
 *     <li>so on...</li>
 * </ul>
 * @author jsanca
 */
public interface CompanyAPI extends Serializable {

    /**
     * Get the company from the {@link HttpServletRequest}, {@link com.liferay.portal.util.WebKeys}.COMPANY
     * @param req {@link HttpServletRequest}
     * @return Company
     */
    public Company getCompany(HttpServletRequest req) throws SystemException, PortalException;

    /**
     * Try to get the company id from the from the {@link javax.servlet.http.HttpSession} or {@link HttpServletRequest},
     * {@link com.liferay.portal.util.WebKeys}.COMPANY_ID.
     * @param req {@link HttpServletRequest}
     * @return String
     */
    public String getCompanyId(HttpServletRequest req);

    /**
     * Get the default company based on the context app config.
     * @return Company
     */
    public Company getDefaultCompany();

    /**
     * Get the user company. The user is based on the id in the Current Thread, see {@link com.liferay.portal.auth.PrincipalThreadLocal}
     * @return Company
     * @throws com.liferay.portal.PortalException
     * @throws com.liferay.portal.SystemException
     */
    public Company getCompany() throws SystemException, PortalException;

    /**
     * Get the company based on a company id
     * @param companyId String
     * @return Company
     * @throws com.liferay.portal.PortalException
     * @throws com.liferay.portal.SystemException
     */
    public  com.liferay.portal.model.Company getCompany(
            java.lang.String companyId)
            throws com.liferay.portal.PortalException,
            com.liferay.portal.SystemException;

    /**
     * Get the company users based on the current thread user company id see {@link com.liferay.portal.auth.PrincipalThreadLocal}
     * @return List of User
     * @throws com.liferay.portal.PortalException
     * @throws com.liferay.portal.SystemException
     */
    public java.util.List<User> getUsers() throws com.liferay.portal.PortalException,
            com.liferay.portal.SystemException;

    /**
     * Update the company based on the current thread user company id see {@link com.liferay.portal.auth.PrincipalThreadLocal}
     * pre: the user has to be an administrator, otherwise will throw a {@link com.liferay.portal.auth.PrincipalException}
     * @param portalURL
     * @param homeURL
     * @param mx
     * @param name
     * @param shortName
     * @param type
     * @param size
     * @param street
     * @param city
     * @param state
     * @param zip
     * @param phone
     * @param fax
     * @param emailAddress
     * @param authType
     * @param autoLogin
     * @param strangers
     * @return Company
     * @throws com.liferay.portal.PortalException
     * @throws com.liferay.portal.SystemException
     */
    public Company updateCompany(
            java.lang.String portalURL, java.lang.String homeURL,
            java.lang.String mx, java.lang.String name, java.lang.String shortName,
            java.lang.String type, java.lang.String size, java.lang.String street,
            java.lang.String city, java.lang.String state, java.lang.String zip,
            java.lang.String phone, java.lang.String fax,
            java.lang.String emailAddress, java.lang.String authType,
            boolean autoLogin, boolean strangers)
            throws com.liferay.portal.PortalException,
            com.liferay.portal.SystemException;

    /**
     * Just update the company
     * Listener are called before and after create it.
     * @param company Company
     * @return Company
     * @throws com.liferay.portal.SystemException
     */
    public Company updateCompany(Company company)throws com.liferay.portal.SystemException;

    /**
     * Update the default user for the current thread user company id see {@link com.liferay.portal.auth.PrincipalThreadLocal}
     * pre: the user has to be an administrator, otherwise will throw a {@link com.liferay.portal.auth.PrincipalException}
     * @param languageId
     * @param timeZoneId
     * @param skinId
     * @param dottedSkins
     * @param roundedSkins
     * @param resolution
     * @throws com.liferay.portal.PortalException
     * @throws com.liferay.portal.SystemException
     */
    public void updateDefaultUser(java.lang.String languageId,
                                         java.lang.String timeZoneId, java.lang.String skinId,
                                         boolean dottedSkins, boolean roundedSkins, java.lang.String resolution)
            throws com.liferay.portal.PortalException,
            com.liferay.portal.SystemException;

    /**
     * Stores the logo file in the assets folders, associated to the current thread user company id see {@link com.liferay.portal.auth.PrincipalThreadLocal}
     * pre: the user has to be an administrator, otherwise will throw a {@link com.liferay.portal.auth.PrincipalException}
     * @param file File
     * @throws com.liferay.portal.PortalException
     * @throws com.liferay.portal.SystemException
     */
    public void updateLogo(java.io.File file)
            throws com.liferay.portal.PortalException,
            com.liferay.portal.SystemException;

    /**
     * Gets the default logo path.
     * @return String
     */
    public String getDefaultLogoPath ();

    /**
     * Gets the logo path based on the company parameter
     * @param company Company
     * @return String
     */
    public String getLogoPath (Company company);

    /**
     * Update the default user settings, based on the current thread user company id see {@link com.liferay.portal.auth.PrincipalThreadLocal}
     * pre: the user has to be an administrator, otherwise will throw a {@link com.liferay.portal.auth.PrincipalException}
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
    public void updateDefaultUserSettings(java.lang.String languageId,
                                   java.lang.String timeZoneId, java.lang.String skinId,
                                   boolean dottedSkins, boolean roundedSkins, java.lang.String resolution)
            throws PortalException, SystemException,com.dotmarketing.exception.DotRuntimeException;
} // E:O:F:CompanyAPI.
