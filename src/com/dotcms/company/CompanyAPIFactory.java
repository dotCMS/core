package com.dotcms.company;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.ImageKey;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

/**
 * Factory to get the CompanyAPI, this encapsulates the creation of it, however in order to
 * get the instance please see {@link com.dotmarketing.business.APILocator}
 * @author jsanca
 */
public class CompanyAPIFactory implements Serializable {

    private final CompanyAPI companyAPI = new CompanyAPIImpl();

    private CompanyAPIFactory () {
        // singleton
    }

    private static class SingletonHolder {
        private static final CompanyAPIFactory INSTANCE = new CompanyAPIFactory();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static CompanyAPIFactory getInstance() {

        return CompanyAPIFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    public CompanyAPI getCompanyAPI() {

        return this.companyAPI;
    }

    private class CompanyAPIImpl implements CompanyAPI {

        @Override
        public Company getCompany(final HttpServletRequest req) throws SystemException, PortalException {

            return PortalUtil.getCompany(req);
        }

        @Override
        public String getCompanyId(final HttpServletRequest req) {

            return PortalUtil.getCompanyId(req);
        }

        @Override
        public Company getDefaultCompany() {

            return CompanyUtils.getDefaultCompany();
        }

        @Override
        public Company getCompany() throws SystemException, PortalException {

            return CompanyManagerUtil.getCompany();
        }

        @Override
        public Company getCompany(final String companyId) throws PortalException, SystemException {

            return CompanyManagerUtil.getCompany(companyId);
        }

        @Override
        public List<User> getUsers() throws PortalException, SystemException {

            return CompanyManagerUtil.getUsers();
        }

        @Override
        public Company updateCompany(String portalURL, String homeURL, String mx, String name,
                                     String shortName, String type, String size, String street,
                                     String city, String state, String zip, String phone, String fax,
                                     String emailAddress, String authType, boolean autoLogin,
                                     boolean strangers) throws PortalException, SystemException {

            return CompanyManagerUtil.updateCompany(portalURL, homeURL, mx, name,
                    shortName, type, size, street,
                    city, state, zip, phone, fax,
                    emailAddress, authType, autoLogin,
                    strangers);
        }

        @Override
        public Company updateCompany(final Company company) throws SystemException {

            return CompanyManagerUtil.updateCompany(company);
        }

        @Override
        public void updateDefaultUser(String languageId, String timeZoneId, String skinId,
                                      boolean dottedSkins, boolean roundedSkins, String resolution) throws PortalException, SystemException {

            CompanyManagerUtil.updateDefaultUser(languageId,  timeZoneId,  skinId,
                                         dottedSkins,  roundedSkins,  resolution);
        }

        @Override
        public void updateLogo(final File file) throws PortalException, SystemException {

            CompanyManagerUtil.updateLogo(file);
        }

        @Override
        public String getDefaultLogoPath() {

            return this.getLogoPath(this.getDefaultCompany());
        }

        @Override
        public String getLogoPath(final Company company) {

            String logoPath = null;

            try {
                final ServletContext c = Config.CONTEXT;
                final String imagePath = (String)c.getAttribute(WebKeys.IMAGE_PATH);

                logoPath = MessageFormat.format("{0}/company_logo?img_id={1}&key={2}",
                        imagePath, company.getCompanyId(), ImageKey.get(company.getCompanyId()));
            } catch (Exception e) {

                throw new DotRuntimeException("No Default Company Id!");
            }

            return logoPath;
        }

        @Override
        public void updateDefaultUserSettings(String languageId, String timeZoneId,
                                              String skinId, boolean dottedSkins, boolean roundedSkins,
                                              String resolution) throws PortalException, SystemException, DotRuntimeException {

            CompanyManagerUtil.updateUsers(languageId, timeZoneId,
                    skinId, dottedSkins, roundedSkins,
                    resolution);
        }
    }
} // E:O:F:CompanyAPI.
