package com.dotcms.rest.api.v1.company;

import com.dotcms.company.CompanyAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rest.api.v1.system.ConfigurationHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidTimeZoneException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.BadRequestException;

/**
 * Business logic helper for company configuration operations.
 * Handles mapping between semantic field names and the Liferay Company model.
 *
 * @author hassandotcms
 */
@ApplicationScoped
public class CompanyConfigHelper {

    private final CompanyAPI companyAPI;

    public CompanyConfigHelper() {
        this(APILocator.getCompanyAPI());
    }

    @VisibleForTesting
    public CompanyConfigHelper(final CompanyAPI companyAPI) {
        this.companyAPI = companyAPI;
    }

    /**
     * Reads the default company and maps it to a CompanyConfigView with semantic field names.
     *
     * @param user the requesting admin user
     * @return the company configuration view
     */
    public CompanyConfigView getCompanyConfig(final User user) {

        final Company company = companyAPI.getDefaultCompany();
        return toView(company);
    }

    /**
     * Saves company basic information and branding settings.
     * Maps semantic field names from the form to the Liferay Company model fields.
     *
     * @param form the basic info form with semantic field names
     * @param user the admin user performing the operation
     * @return the updated company configuration view
     */
    public CompanyConfigView saveBasicInfo(final CompanyBasicInfoForm form, final User user) {

        // Validate email format using the existing ConfigurationHelper utility
        try {
            ConfigurationHelper.INSTANCE.parseMailAndSender(form.getEmailAddress());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid email address: " + e.getMessage());
        }

        // NavLogo feature is Enterprise only
        String navBarLogo = form.getNavBarLogo();
        if (LicenseUtil.getLevel() == LicenseLevel.COMMUNITY.level
                && UtilMethods.isSet(navBarLogo)) {
            Logger.warn(this, "NavLogo feature is only for Enterprise Edition, ignoring value");
            navBarLogo = StringPool.BLANK;
        }

        try {
            PrincipalThreadLocal.setName(user.getUserId());

            final Company company = companyAPI.getDefaultCompany();

            // Map semantic names → Liferay Company model fields
            company.setPortalURL(form.getPortalURL());
            company.setEmailAddress(form.getEmailAddress());
            company.setMx(UtilMethods.isSet(form.getMx())
                    ? form.getMx()
                    : extractDomain(form.getEmailAddress()));
            company.setType(form.getPrimaryColor());       // type = primaryColor
            company.setStreet(form.getSecondaryColor());   // street = secondaryColor
            company.setSize(UtilMethods.isSet(form.getBackgroundColor())
                    ? form.getBackgroundColor()
                    : StringPool.BLANK);                   // size = backgroundColor
            company.setHomeURL(UtilMethods.isSet(form.getBackgroundImage())
                    ? form.getBackgroundImage()
                    : StringPool.BLANK);                   // homeURL = backgroundImage
            company.setCity(UtilMethods.isSet(form.getLoginScreenLogo())
                    ? form.getLoginScreenLogo()
                    : StringPool.BLANK);                   // city = loginScreenLogo
            company.setState(UtilMethods.isSet(navBarLogo)
                    ? navBarLogo
                    : StringPool.BLANK);                   // state = navBarLogo

            CompanyManagerUtil.updateCompany(company);

            return toView(company);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            Logger.error(this, "Error saving basic info for company: " + e.getMessage(), e);
            throw new RuntimeException("Error saving company basic info", e);
        } finally {
            PrincipalThreadLocal.setName(null);
        }
    }

    /**
     * Saves the company authentication type.
     *
     * @param form the auth type form
     * @param user the admin user performing the operation
     * @return the updated company configuration view
     */
    public CompanyConfigView saveAuthType(final CompanyAuthTypeForm form, final User user) {

        try {
            PrincipalThreadLocal.setName(user.getUserId());

            final Company company = companyAPI.getDefaultCompany();
            company.setAuthType(form.getAuthType());
            CompanyManagerUtil.updateCompany(company);

            return toView(company);
        } catch (Exception e) {
            Logger.error(this, "Error saving auth type for company: " + e.getMessage(), e);
            throw new RuntimeException("Error saving company auth type", e);
        } finally {
            PrincipalThreadLocal.setName(null);
        }
    }

    /**
     * Saves company locale information (language and timezone).
     * Updates the default company user's locale, sets the JVM-wide default timezone,
     * and flushes the user cache.
     *
     * @param form the locale form containing languageId and timeZoneId
     * @param user the admin user performing the operation
     */
    public void saveLocaleInfo(final CompanyLocaleForm form, final User user) {

        try {
            PrincipalThreadLocal.setName(user.getUserId());
            CompanyManagerUtil.updateUsers(
                    form.getLanguageId(), form.getTimeZoneId(),
                    null, false, false, null);
        } catch (InvalidTimeZoneException e) {
            throw e;
        } catch (Exception e) {
            Logger.error(this, "Error saving locale information for company: " + e.getMessage(), e);
            throw new RuntimeException("Error saving locale information", e);
        } finally {
            PrincipalThreadLocal.setName(null);
        }
    }

    /**
     * Regenerates the company security key.
     *
     * @param user the admin user performing the operation
     * @return the SHA-256 digest of the new key
     * @throws DotDataException     if a data error occurs
     * @throws DotSecurityException if the user lacks permission
     */
    public String regenerateKey(final User user) throws DotDataException, DotSecurityException {

        final Company company = companyAPI.getDefaultCompany();
        final Company updated = companyAPI.regenerateKey(company, user);
        return updated.getKeyDigest();
    }

    /**
     * Maps a Liferay Company model to a CompanyConfigView with semantic field names.
     */
    private CompanyConfigView toView(final Company company) {

        final String loginLogo = company.getCity();
        final String navLogo = company.getState();

        return CompanyConfigView.builder()
                .companyId(company.getCompanyId())
                .companyName(company.getName())
                .portalURL(company.getPortalURL())
                .emailAddress(company.getEmailAddress())
                .mx(company.getMx())
                .primaryColor(company.getType())
                .secondaryColor(company.getStreet())
                .backgroundColor(company.getSize())
                .backgroundImage(
                        UtilMethods.isSet(company.getHomeURL()) ? company.getHomeURL() : null)
                .loginScreenLogo(
                        UtilMethods.isSet(loginLogo) && loginLogo.startsWith("/dA")
                                ? loginLogo : null)
                .navBarLogo(
                        LicenseUtil.getLevel() > LicenseLevel.COMMUNITY.level
                                && UtilMethods.isSet(navLogo) && navLogo.startsWith("/dA")
                                ? navLogo : null)
                .authType(company.getAuthType())
                .keyDigest(company.getKeyDigest())
                .build();
    }

    /**
     * Extracts the domain from an email address.
     */
    private String extractDomain(final String email) {

        if (UtilMethods.isSet(email) && email.contains("@")) {
            return email.substring(email.indexOf('@') + 1);
        }
        return StringPool.BLANK;
    }
}
