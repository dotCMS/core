package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.company.CompanyAPI;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the logo fields of
 * {@link CMSConfigResource#saveCompanyBasicInfo}, called by the Basic Config tab of the
 * Configuration portlet.
 */
public class CMSConfigResourceIntegrationTest extends IntegrationTestBase {

    private static final String DOT_ASSET_LOGO =
            "/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/dotCMS-400x200.png";

    private static final String STORED_BACKGROUND = "/html/images/backgrounds/bg-11.jpg";

    private static CMSConfigResource resource;
    private static CompanyAPI companyAPI;
    private static User configurationUser;
    private static User adminUser;
    private static Layout configurationLayout;
    private static Company original;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new CMSConfigResource();
        companyAPI = APILocator.getCompanyAPI();
        original = (Company) companyAPI.getDefaultCompany().clone();

        adminUser = TestUserUtils.getAdminUser();
        configurationUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), configurationUser);
        configurationLayout = new LayoutDataGen()
                .name("configuration-" + System.currentTimeMillis())
                .portletIds(PortletID.CONFIGURATION.toString())
                .nextPersisted();
        APILocator.getRoleAPI().addLayoutToRole(configurationLayout,
                APILocator.getRoleAPI().getUserRole(configurationUser));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (original != null) {
            CompanyManagerUtil.updateCompany(original);
        }
        if (configurationLayout != null) {
            APILocator.getLayoutAPI().removeLayout(configurationLayout);
        }
    }

    @Before
    public void storeDotAssetLogo() throws Exception {
        final Company company = companyAPI.getDefaultCompany();
        company.setCity(DOT_ASSET_LOGO);
        company.setState("");
        company.setSize("#1b3359");
        company.setType("#4e65f1");
        company.setStreet("#233f9b");
        company.setHomeURL(STORED_BACKGROUND);
        CompanyManagerUtil.updateCompany(company);
    }

    /**
     * A login screen logo that is not a dotAsset path gets a 400 and the stored logo is kept.
     */
    @Test
    public void saveCompanyBasicInfo_loginLogoNotADotAssetPath_returns400() throws Exception {
        for (final String invalid : new String[]{
                "/dA/../html/logo.png",
                "/dA/%2e%2e/html/logo.png",
                "/dA/..;/html/logo.png",
                "/dAx/logo.png",
                "/dA/lo\"go.png"}) {
            final Response response = save(invalid, "");

            assertEquals(invalid, 400, response.getStatus());
            assertEquals(DOT_ASSET_LOGO, companyAPI.getDefaultCompany().getCity());
        }
    }

    /**
     * A navigation bar logo that is not a dotAsset path gets a 400 and nothing is stored.
     */
    @Test
    public void saveCompanyBasicInfo_navLogoNotADotAssetPath_returns400() throws Exception {
        final Response response = save(DOT_ASSET_LOGO, "/dA/../html/logo.png");

        assertEquals(400, response.getStatus());
        assertEquals("", companyAPI.getDefaultCompany().getState());
    }

    /**
     * What the Basic Config tab sends, a dotAsset login logo with a file name that has spaces
     * and no navigation bar logo, is saved.
     */
    @Test
    public void saveCompanyBasicInfo_dotAssetLogo_isSaved() throws Exception {
        final String logo = "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/my logo (1).png";

        final Response response = save(logo, "");

        assertEquals(200, response.getStatus());
        assertEquals(logo, companyAPI.getDefaultCompany().getCity());
    }

    /**
     * A login logo whose file name has an apostrophe and a number sign is saved.
     */
    @Test
    public void saveCompanyBasicInfo_logoNameWithApostropheAndNumberSign_isSaved() throws Exception {
        final String logo = "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/O'Neil logo#2.png";

        final Response response = save(logo, "");

        assertEquals(200, response.getStatus());
        assertEquals(logo, companyAPI.getDefaultCompany().getCity());
    }

    /**
     * A color that is not a hex color gets a 400 and the stored colors are kept.
     */
    @Test
    public void saveCompanyBasicInfo_colorNotHex_returns400() throws Exception {
        final Company before = companyAPI.getDefaultCompany();
        final String size = before.getSize();
        final String type = before.getType();
        final String street = before.getStreet();
        for (final String[] colors : new String[][]{
                {"#1b3359\" x", "#4e65f1", "#233f9b"},
                {"#1b3359", "red", "#233f9b"},
                {"#1b3359", "#4e65f1", "#233f9b;"}}) {
            final Response response = save(DOT_ASSET_LOGO, "", colors[0], colors[1], colors[2]);

            assertEquals(String.join(",", colors), 400, response.getStatus());
            final Company after = companyAPI.getDefaultCompany();
            assertEquals(size, after.getSize());
            assertEquals(type, after.getType());
            assertEquals(street, after.getStreet());
        }
    }

    /**
     * Colors already stored that are not hex colors, sent back unchanged (the Basic Config tab
     * resends the hidden background color as it is), are saved; a new value that is not a hex
     * color is still refused.
     */
    @Test
    public void saveCompanyBasicInfo_storedColorsNotHexSentUnchanged_areSaved() throws Exception {
        final Company company = companyAPI.getDefaultCompany();
        company.setSize("navy");
        company.setType("rgb(1,2,3)");
        company.setStreet("#233f9b ");
        CompanyManagerUtil.updateCompany(company);

        final Response unchanged = save(DOT_ASSET_LOGO, "", "navy", "rgb(1,2,3)", "#233f9b ");
        assertEquals(200, unchanged.getStatus());

        final Response changed = save(DOT_ASSET_LOGO, "", "navy", "teal", "#233f9b ");
        assertEquals(400, changed.getStatus());
        assertEquals("rgb(1,2,3)", companyAPI.getDefaultCompany().getType());
    }

    /**
     * A new background image that is neither a dotAsset path nor a bundled background gets a 400,
     * and the stored background is kept.
     */
    @Test
    public void saveCompanyBasicInfo_backgroundImageNotAccepted_returns400() throws Exception {
        for (final String invalid : new String[]{
                "/images/bg.jpg",
                "https://example.com/bg.jpg",
                "/dA/../html/bg.png",
                "/html/images/backgrounds/bg-12.jpg"}) {
            final Response response = save(DOT_ASSET_LOGO, "", "#1b3359", "#4e65f1", "#233f9b",
                    invalid);

            assertEquals(invalid, 400, response.getStatus());
            assertEquals(STORED_BACKGROUND, companyAPI.getDefaultCompany().getHomeURL());
        }
    }

    /**
     * A bundled background, a dotAsset background, an empty value and the background already
     * stored are saved.
     */
    @Test
    public void saveCompanyBasicInfo_acceptedOrStoredBackground_isSaved() throws Exception {
        for (final String accepted : new String[]{
                "/html/images/backgrounds/bg-3.jpg",
                "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/bg.png",
                ""}) {
            assertEquals(accepted, 200,
                    save(DOT_ASSET_LOGO, "", "#1b3359", "#4e65f1", "#233f9b", accepted).getStatus());
        }

        final Company company = companyAPI.getDefaultCompany();
        company.setHomeURL("/application/themes/old/bg.jpg");
        CompanyManagerUtil.updateCompany(company);
        assertEquals(200, save(DOT_ASSET_LOGO, "", "#1b3359", "#4e65f1", "#233f9b",
                "/application/themes/old/bg.jpg").getStatus());
    }

    /**
     * A back-end user who has the Configuration portlet but is not a CMS Administrator cannot
     * change the company basic information, locale, authentication type or logo, and nothing is
     * changed.
     */
    @Test
    public void writes_withConfigurationPortletButNotAdministrator_areRejected() throws Exception {
        final Company before = (Company) companyAPI.getDefaultCompany().clone();
        final java.util.List<java.util.concurrent.Callable<Response>> writes = java.util.List.of(
                () -> resource.saveCompanyBasicInfo(requestFor(configurationUser), response(), "", "",
                        "http://localhost:8080", "dotcms.com", "dotCMS Website <website@dotcms.com>",
                        "#000000", "#111111", "#222222", before.getHomeURL(), DOT_ASSET_LOGO, ""),
                () -> resource.saveCompanyLocaleInfo(requestFor(configurationUser), response(), "", "",
                        "en_US", "UTC"),
                () -> resource.saveCompanyAuthTypeInfo(requestFor(configurationUser), response(), "", "",
                        "userId"),
                () -> resource.saveCompanyLogo(requestFor(configurationUser), response(), "", "",
                        null, null));
        for (int i = 0; i < writes.size(); i++) {
            try {
                writes.get(i).call();
                fail("Expected SecurityException for write #" + i);
            } catch (final SecurityException expected) {
                // rejected before doing anything
            }
        }
        final Company after = companyAPI.getDefaultCompany();
        assertEquals(before.getSize(), after.getSize());
        assertEquals(before.getType(), after.getType());
        assertEquals(before.getStreet(), after.getStreet());
        assertEquals(before.getAuthType(), after.getAuthType());
    }

    /**
     * Deleting publishing environments and endpoints keeps the Configuration portlet rule, like
     * creating and editing them: a user with the portlet who is not a CMS Administrator is not
     * refused.
     */
    @Test
    public void publishingDeletes_withConfigurationPortlet_areNotRefused() throws Exception {
        resource.deleteEnvironment(requestFor(configurationUser), response(), "", "",
                "not-an-environment", null, null);
        resource.deleteEndpoint(requestFor(configurationUser), response(), "", "",
                "not-an-endpoint", null, null);
    }

    /**
     * A request authenticated as the given user.
     */
    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private static HttpServletResponse response() {
        return new MockHttpResponse().response();
    }

    /**
     * Calls the endpoint as a CMS Administrator, resending the stored values of every other
     * field.
     */
    private static Response save(final String loginLogo, final String navLogo) throws Exception {
        return save(loginLogo, navLogo, "#1b3359", "#4e65f1", "#233f9b");
    }

    /**
     * Same as {@link #save(String, String)}, with the three colors given.
     */
    private static Response save(final String loginLogo, final String navLogo,
            final String backgroundColor, final String primaryColor, final String secondaryColor)
            throws Exception {
        return save(loginLogo, navLogo, backgroundColor, primaryColor, secondaryColor,
                companyAPI.getDefaultCompany().getHomeURL());
    }

    /**
     * Same as {@link #save(String, String, String, String, String)}, with the background image
     * given.
     */
    private static Response save(final String loginLogo, final String navLogo,
            final String backgroundColor, final String primaryColor, final String secondaryColor,
            final String backgroundImage) throws Exception {
        return resource.saveCompanyBasicInfo(requestFor(adminUser), response(), "", "",
                "http://localhost:8080", "dotcms.com", "dotCMS Website <website@dotcms.com>",
                backgroundColor, primaryColor, secondaryColor, backgroundImage,
                loginLogo, navLogo);
    }
}
