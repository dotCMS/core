package com.dotcms.rest.api.v1.company;

import com.dotcms.company.CompanyAPI;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rest.api.v1.system.ConfigurationHelper;
import com.dotcms.storage.model.Metadata;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidTimeZoneException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dotcms.rest.exception.BadRequestException;

/**
 * Business logic helper for company configuration operations.
 * Handles mapping between semantic field names and the Liferay Company model.
 *
 * @author hassandotcms
 */
public class CompanyConfigHelper {

    private static final String LICENSE_TITLE = "dotCMS Business Source License";
    private static final Pattern LICENSE_VERSION =
            Pattern.compile("^Business Source License\\s+(\\S+)\\s*$", Pattern.MULTILINE);
    private static final Pattern LICENSE_LICENSOR =
            Pattern.compile("^Licensor:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern LICENSE_CHANGE_DATE =
            Pattern.compile("^Change Date:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern LICENSE_CHANGE_LICENSE =
            Pattern.compile("^Change License:\\s*(.+)$", Pattern.MULTILINE);

    /**
     * Identifier of the stock dotCMS login logo dotAsset created by the starter data.
     */
    static final String STOCK_LOGO_IDENTIFIER = "a083e942-5fb0-47ef-9619-6f8f40593474";

    /**
     * sha256 of the stock dotCMS login logo file.
     */
    static final String STOCK_LOGO_SHA256 =
            "6f59e8d34e7b67b14948f228110f14ef13bf2f19d949dc19ea3da08cd7ef74d0";

    private final CompanyAPI companyAPI;
    private final ContentletAPI contentletAPI;
    private final VersionableAPI versionableAPI;
    private final PermissionAPI permissionAPI;
    private final UserAPI userAPI;

    public CompanyConfigHelper() {
        this(APILocator.getCompanyAPI(), APILocator.getContentletAPI(),
                APILocator.getVersionableAPI(), APILocator.getPermissionAPI(),
                APILocator.getUserAPI());
    }

    /**
     * Builds the helper with the APIs it reads from, so tests can pass their own.
     *
     * @param companyAPI     reads and saves the default company
     * @param contentletAPI  loads the published stock login logo dotAsset
     * @param versionableAPI finds which language versions of it are published
     * @param permissionAPI  checks the login page can read it
     * @param userAPI        gives the system and anonymous users for those lookups
     */
    @VisibleForTesting
    public CompanyConfigHelper(final CompanyAPI companyAPI, final ContentletAPI contentletAPI,
                               final VersionableAPI versionableAPI,
                               final PermissionAPI permissionAPI, final UserAPI userAPI) {
        this.companyAPI = companyAPI;
        this.contentletAPI = contentletAPI;
        this.versionableAPI = versionableAPI;
        this.permissionAPI = permissionAPI;
        this.userAPI = userAPI;
    }

    /**
     * Returns the {@code /dA/...} path of the stock dotCMS login logo, so a client can save it
     * back as the login screen logo. The path is returned only when the starter's stock logo
     * dotAsset is published in some language, the anonymous user can read it, and its file is
     * still the stock dotCMS image. No content and no company setting is created or changed.
     *
     * @return the path, or empty when this instance doesn't have the stock logo
     * @throws DotDataException     if the dotAsset can't be read
     * @throws DotSecurityException if the system user can't read the dotAsset
     */
    public Optional<String> findStockLoginLogoPath() throws DotDataException, DotSecurityException {

        // Any language may hold the published logo; the lowest language id wins, so repeated
        // calls always pick the same version.
        final Optional<ContentletVersionInfo> published = versionableAPI
                .findContentletVersionInfos(STOCK_LOGO_IDENTIFIER, VariantAPI.DEFAULT_VARIANT.name())
                .stream()
                .filter(info -> !info.isDeleted() && UtilMethods.isSet(info.getLiveInode()))
                .min(Comparator.comparingLong(ContentletVersionInfo::getLang));
        if (published.isEmpty()) {
            Logger.debug(this, "Stock login logo not available: missing, archived or not published");
            return Optional.empty();
        }

        final Contentlet live = contentletAPI.find(published.get().getLiveInode(),
                userAPI.getSystemUser(), false);
        if (live == null) {
            Logger.debug(this, "Stock login logo not available: published version not found");
            return Optional.empty();
        }

        if (!permissionAPI.doesUserHavePermission(live, PermissionAPI.PERMISSION_READ,
                userAPI.getAnonymousUser(), true)) {
            Logger.debug(this, "Stock login logo not available: anonymous users can't read it");
            return Optional.empty();
        }

        final Metadata metadata = live.getBinaryMetadata(DotAssetContentType.ASSET_FIELD_VAR);
        if (metadata == null || !STOCK_LOGO_SHA256.equals(metadata.getSha256())) {
            Logger.debug(this, "Stock login logo not available: its file is not the stock logo");
            return Optional.empty();
        }

        return Optional.of("/dA/" + STOCK_LOGO_IDENTIFIER + "/asset/" + metadata.getName());
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
            validateBackgroundImage(form.getBackgroundImage(), company.getHomeURL());

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
            throw new DotRuntimeException("Error saving company basic info", e);
        } finally {
            PrincipalThreadLocal.setName(null);
        }
    }

    /**
     * Checks a new login background. Besides the values {@link
     * CompanyBasicInfoForm#isAcceptedBackgroundImage(String)} accepts, the value already stored
     * is accepted, so that saving back what was read never fails or clears a background set
     * before these rules existed.
     *
     * @param backgroundImage the value sent by the client
     * @param storedHomeURL   the value currently stored in {@code company.homeURL}
     * @throws BadRequestException if the value is none of the accepted forms
     */
    private void validateBackgroundImage(final String backgroundImage, final String storedHomeURL) {

        if (CompanyBasicInfoForm.isAcceptedBackgroundImage(backgroundImage)
                || backgroundImage.equals(storedHomeURL)) {
            return;
        }
        throw new BadRequestException("backgroundImage must be a dotAsset path starting with /dA/, "
                + "one of the bundled backgrounds " + CompanyBasicInfoForm.BACKGROUND_PRESETS_TEXT
                + ", or the background currently saved");
    }

    /**
     * Returns the license shipped with the running build.
     *
     * @return the license text and its header values
     */
    public LicenseInfoView getLicenseInfo() {
        return parseLicenseInfo(LicenseUtil.getLicenseText());
    }

    /**
     * Reads the header values of the Business Source License text. A value whose line is
     * missing, for example in the fallback text used when the file can't be read, is
     * {@code null}.
     *
     * @param text the license text
     * @return the license text with its title, licensor, change date and change license
     */
    static LicenseInfoView parseLicenseInfo(final String text) {

        final String version = licenseHeader(LICENSE_VERSION, text);
        return LicenseInfoView.builder()
                .title(version == null ? LICENSE_TITLE : LICENSE_TITLE + " " + version)
                .licensor(licenseHeader(LICENSE_LICENSOR, text))
                .changeDate(licenseHeader(LICENSE_CHANGE_DATE, text))
                .changeLicense(licenseHeader(LICENSE_CHANGE_LICENSE, text))
                .text(text)
                .build();
    }

    /**
     * Returns the trimmed first group of {@code pattern} in {@code text}, or {@code null}.
     */
    private static String licenseHeader(final Pattern pattern, final String text) {
        final Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
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
            company.setAuthType(form.getAuthType().getValue());
            CompanyManagerUtil.updateCompany(company);

            return toView(company);
        } catch (Exception e) {
            Logger.error(this, "Error saving auth type for company: " + e.getMessage(), e);
            throw new DotRuntimeException("Error saving company auth type", e);
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
            throw new BadRequestException("Invalid timeZoneId: '" + form.getTimeZoneId() + "'");
        } catch (Exception e) {
            Logger.error(this, "Error saving locale information for company: " + e.getMessage(), e);
            throw new DotRuntimeException("Error saving locale information", e);
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

        // Liferay Company repurposes address fields for branding:
        // city = loginScreenLogo, state = navBarLogo, type = primaryColor,
        // street = secondaryColor, size = backgroundColor, homeURL = backgroundImage
        final String loginLogo = company.getCity();
        final String navLogo = company.getState();

        // Locale is stored on the default User, not the Company entity
        String languageId = null;
        String timeZoneId = null;
        try {
            final User defaultUser = APILocator.getUserAPI().getDefaultUser();
            languageId = defaultUser.getLanguageId();
            timeZoneId = defaultUser.getTimeZoneId();
        } catch (Exception e) {
            Logger.debug(this, "Could not read default user locale: " + e.getMessage());
        }

        return CompanyConfigView.builder()
                .companyId(company.getCompanyId())
                .companyName(company.getName())
                .portalURL(company.getPortalURL())
                .emailAddress(company.getEmailAddress())
                .mx(company.getMx())
                .primaryColor(company.getType())
                .secondaryColor(company.getStreet())
                .backgroundColor(
                        UtilMethods.isSet(company.getSize()) ? company.getSize() : null)
                // Any stored background is returned so a client can save it back unchanged;
                // "localhost" is the fresh-install value that means no background.
                .backgroundImage(
                        UtilMethods.isSet(company.getHomeURL())
                                && !"localhost".equals(company.getHomeURL())
                                ? company.getHomeURL() : null)
                .loginScreenLogo(
                        UtilMethods.isSet(loginLogo) && loginLogo.startsWith("/dA")
                                ? loginLogo : null)
                .navBarLogo(
                        LicenseUtil.getLevel() > LicenseLevel.COMMUNITY.level
                                && UtilMethods.isSet(navLogo) && navLogo.startsWith("/dA")
                                ? navLogo : null)
                .authType(AuthType.fromStringOrDefault(company.getAuthType()))
                .keyDigest(company.getKeyDigest())
                .languageId(languageId)
                .timeZoneId(timeZoneId)
                .build();
    }

    /**
     * Extracts the domain from an email address.
     * Handles "Display Name &lt;user@domain&gt;" format via InternetAddress parsing.
     */
    private String extractDomain(final String email) {

        try {
            if (UtilMethods.isSet(email) && email.contains("@")) {
                final javax.mail.internet.InternetAddress[] addresses =
                        javax.mail.internet.InternetAddress.parse(email);
                if (addresses.length > 0) {
                    final String address = addresses[0].getAddress();
                    return address.substring(address.indexOf('@') + 1);
                }
            }
        } catch (Exception e) {
            Logger.debug(this, "Could not extract domain from email: " + e.getMessage());
        }
        return StringPool.BLANK;
    }
}
