package com.dotcms.rest.api.v1.company;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.company.CompanyAPI;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.rest.api.v1.system.CompanyEmailForm;
import com.dotcms.rest.api.v1.system.ConfigurationResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.rest.exception.BadRequestException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link CompanyResource}.
 * Tests the v1 company configuration REST API endpoints.
 *
 * @author hassandotcms
 */
public class CompanyResourceIntegrationTest extends IntegrationTestBase {

    private static CompanyResource resource;
    private static HttpServletResponse mockResponse;
    private static User adminUser;
    private static User nonAdminUser;
    private static CompanyAPI companyAPI;
    private static Layout configurationLayout;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new CompanyResource();
        mockResponse = new MockHttpResponse().response();
        adminUser = TestUserUtils.getAdminUser();
        companyAPI = APILocator.getCompanyAPI();

        // Create a non-admin backend user for permission tests
        nonAdminUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), nonAdminUser);

        // Give the non-admin the Configuration portlet, to show the role is still required
        configurationLayout = new LayoutDataGen()
                .name("configuration-" + System.currentTimeMillis())
                .portletIds(PortletID.CONFIGURATION.toString())
                .nextPersisted();
        APILocator.getRoleAPI().addLayoutToRole(configurationLayout,
                APILocator.getRoleAPI().getUserRole(nonAdminUser));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (configurationLayout != null) {
            APILocator.getLayoutAPI().removeLayout(configurationLayout);
        }
    }

    // ==================== GET /v1/configuration/branding ====================

    @Test
    public void test_getCompanyConfig_asAdmin_returnsMappedConfig() {
        final HttpServletRequest request = createAdminRequest();
        final Company company = companyAPI.getDefaultCompany();

        final ResponseEntityCompanyConfigView response =
                resource.getCompanyConfig(request, mockResponse);

        assertNotNull(response);
        final CompanyConfigView config = response.getEntity();
        assertNotNull(config);

        // Verify the view maps correctly from the Company model
        assertEquals(company.getCompanyId(), config.companyId());
        assertEquals(company.getName(), config.companyName());
        assertEquals(company.getPortalURL(), config.portalURL());
        assertEquals(company.getEmailAddress(), config.emailAddress());
        assertEquals(company.getMx(), config.mx());
        assertEquals(AuthType.fromString(company.getAuthType()), config.authType());
        assertEquals(company.getType(), config.primaryColor());
        assertEquals(company.getStreet(), config.secondaryColor());
    }

    @Test(expected = SecurityException.class)
    public void test_getCompanyConfig_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.getCompanyConfig(request, mockResponse);
    }

    // ==================== PUT /v1/configuration/branding ====================

    @Test
    public void test_saveBasicInfo_asAdmin_persistsAndReturnsUpdatedConfig() {
        final HttpServletRequest request = createAdminRequest();
        final Company original = companyAPI.getDefaultCompany();

        // Snapshot ALL fields we'll modify for restore
        final String origPortalURL = original.getPortalURL();
        final String origEmail = original.getEmailAddress();
        final String origMx = original.getMx();
        final String origType = original.getType();
        final String origStreet = original.getStreet();
        final String origSize = original.getSize();
        final String origHomeURL = original.getHomeURL();
        final String origCity = original.getCity();
        final String origState = original.getState();

        try {
            final CompanyBasicInfoForm form = basicInfoForm(
                    "http://localhost:9090", "test@dotcms.com", null,
                    "#FF0000", "#00FF00", "#0000FF",
                    null, null, null);

            final ResponseEntityCompanyConfigView result =
                    resource.saveBasicInfo(request, mockResponse, form);

            // Verify the returned view
            final CompanyConfigView config = result.getEntity();
            assertEquals("http://localhost:9090", config.portalURL());
            assertEquals("test@dotcms.com", config.emailAddress());
            assertEquals("dotcms.com", config.mx());
            assertEquals("#FF0000", config.primaryColor());
            assertEquals("#00FF00", config.secondaryColor());
            assertEquals("#0000FF", config.backgroundColor());
            assertNull("backgroundImage should be null when not provided", config.backgroundImage());
            assertNull("loginScreenLogo should be null when not provided", config.loginScreenLogo());

            // Verify persistence — re-read from database
            final Company persisted = companyAPI.getDefaultCompany();
            assertEquals("http://localhost:9090", persisted.getPortalURL());
            assertEquals("test@dotcms.com", persisted.getEmailAddress());
            assertEquals("dotcms.com", persisted.getMx());
            assertEquals("#FF0000", persisted.getType());
            assertEquals("#00FF00", persisted.getStreet());
            assertEquals("#0000FF", persisted.getSize());
        } finally {
            restoreCompany(origPortalURL, origEmail, origMx, origType,
                    origStreet, origSize, origHomeURL, origCity, origState);
        }
    }

    @Test
    public void test_saveBasicInfo_mxDerivedFromEmail_whenNotProvided() {
        final HttpServletRequest request = createAdminRequest();
        final Company original = companyAPI.getDefaultCompany();
        final String origMx = original.getMx();
        final String origPortalURL = original.getPortalURL();
        final String origEmail = original.getEmailAddress();
        final String origType = original.getType();
        final String origStreet = original.getStreet();
        final String origSize = original.getSize();
        final String origHomeURL = original.getHomeURL();
        final String origCity = original.getCity();
        final String origState = original.getState();

        try {
            final CompanyBasicInfoForm form = basicInfoForm(
                    "http://localhost:8080", "admin@example.org", null,
                    "#000", "#111", null, null, null, null);

            final ResponseEntityCompanyConfigView result =
                    resource.saveBasicInfo(request, mockResponse, form);

            assertEquals("example.org", result.getEntity().mx());
        } finally {
            restoreCompany(origPortalURL, origEmail, origMx, origType,
                    origStreet, origSize, origHomeURL, origCity, origState);
        }
    }

    @Test(expected = BadRequestException.class)
    public void test_saveBasicInfo_invalidEmail_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyBasicInfoForm form = basicInfoForm(
                "http://localhost:8080", "not-an-email", null,
                "#FF0000", "#00FF00", null, null, null, null);
        resource.saveBasicInfo(request, mockResponse, form);
    }

    @Test(expected = ValidationException.class)
    public void test_saveBasicInfo_missingPortalURL_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyBasicInfoForm form = basicInfoForm(
                null, "test@dotcms.com", null,
                "#FF0000", "#00FF00", null, null, null, null);
        resource.saveBasicInfo(request, mockResponse, form);
    }

    @Test(expected = ValidationException.class)
    public void test_saveBasicInfo_missingPrimaryColor_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyBasicInfoForm form = basicInfoForm(
                "http://localhost:8080", "test@dotcms.com", null,
                null, "#00FF00", null, null, null, null);
        resource.saveBasicInfo(request, mockResponse, form);
    }

    @Test(expected = SecurityException.class)
    public void test_saveBasicInfo_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        final CompanyBasicInfoForm form = basicInfoForm(
                "http://localhost:8080", "test@dotcms.com", null,
                "#FF0000", "#00FF00", null, null, null, null);
        resource.saveBasicInfo(request, mockResponse, form);
    }

    // ==================== backgroundImage (#37873) ====================

    /**
     * The bundled login backgrounds are accepted on save, persisted to {@code homeURL} and
     * returned unchanged on read.
     */
    @Test
    public void test_saveBasicInfo_presetBackground_isAcceptedAndReturned() {
        final CompanySnapshot snapshot = CompanySnapshot.take(companyAPI);
        try {
            for (final String preset : new String[]{
                    "/html/images/backgrounds/bg-1.jpg",
                    "/html/images/backgrounds/bg-5.jpg",
                    "/html/images/backgrounds/bg-11.jpg"}) {

                final CompanyConfigView saved = resource.saveBasicInfo(
                        createAdminRequest(), mockResponse, brandingForm(preset)).getEntity();

                assertEquals(preset, saved.backgroundImage());
                assertEquals(preset, companyAPI.getDefaultCompany().getHomeURL());
                assertEquals(preset, resource.getCompanyConfig(createAdminRequest(), mockResponse)
                        .getEntity().backgroundImage());
            }
        } finally {
            snapshot.restore(companyAPI);
        }
    }

    /**
     * A stored background that is not a dotAsset path is returned as stored; the fresh-install
     * {@code localhost} value is returned as {@code null}.
     */
    @Test
    public void test_getCompanyConfig_storedNonDotAssetBackground_isReturned() throws Exception {
        final CompanySnapshot snapshot = CompanySnapshot.take(companyAPI);
        try {
            storeHomeURL("https://example.com/bg.jpg");
            assertEquals("https://example.com/bg.jpg",
                    resource.getCompanyConfig(createAdminRequest(), mockResponse)
                            .getEntity().backgroundImage());

            storeHomeURL("localhost");
            assertNull(resource.getCompanyConfig(createAdminRequest(), mockResponse)
                    .getEntity().backgroundImage());
        } finally {
            snapshot.restore(companyAPI);
        }
    }

    /**
     * Reading the branding and saving it back with another field changed keeps the stored
     * background, whether it is a preset or a legacy URL that is only accepted because it is
     * the value already stored.
     */
    @Test
    public void test_saveBasicInfo_roundTrip_keepsStoredBackground() throws Exception {
        final CompanySnapshot snapshot = CompanySnapshot.take(companyAPI);
        try {
            for (final String stored : new String[]{
                    "/html/images/backgrounds/bg-5.jpg", "https://example.com/bg.jpg"}) {

                storeHomeURL(stored);
                final CompanyConfigView read = resource.getCompanyConfig(
                        createAdminRequest(), mockResponse).getEntity();

                resource.saveBasicInfo(createAdminRequest(), mockResponse,
                        brandingForm(read.backgroundImage()));

                assertEquals(stored, companyAPI.getDefaultCompany().getHomeURL());
            }
        } finally {
            snapshot.restore(companyAPI);
        }
    }

    /**
     * A new background that is neither a dotAsset path, a bundled preset nor the stored value
     * is rejected with a message naming the field, and nothing is saved.
     */
    @Test
    public void test_saveBasicInfo_invalidBackground_isRejected() throws Exception {
        final CompanySnapshot snapshot = CompanySnapshot.take(companyAPI);
        try {
            storeHomeURL("/dA/stored/bg.png");
            for (final String invalid : new String[]{
                    "/html/images/backgrounds/bg-12.jpg",
                    "/html/images/backgrounds/bg-1-sm.jpg",
                    "/html/images/backgrounds/BG-1.JPG",
                    "/html/images/backgrounds/bg-1.jpg?x",
                    "/html/images/../bg-1.jpg",
                    "https://example.com/html/images/backgrounds/bg-1.jpg",
                    "/dAnything/bg.png",
                    "/dA/../html/bg.png"}) {
                try {
                    resource.saveBasicInfo(createAdminRequest(), mockResponse, brandingForm(invalid));
                    fail("Expected BadRequestException for " + invalid);
                } catch (final BadRequestException e) {
                    final String body = String.valueOf(e.getResponse().getEntity());
                    assertTrue(body, body.contains("backgroundImage"));
                }
                assertEquals("/dA/stored/bg.png", companyAPI.getDefaultCompany().getHomeURL());
            }
        } finally {
            snapshot.restore(companyAPI);
        }
    }

    /**
     * dotAsset and empty backgrounds behave exactly as before.
     */
    @Test
    public void test_saveBasicInfo_dotAssetAndEmptyBackground_unchanged() {
        final CompanySnapshot snapshot = CompanySnapshot.take(companyAPI);
        try {
            final CompanyConfigView withAsset = resource.saveBasicInfo(createAdminRequest(),
                    mockResponse, brandingForm("/dA/abc/bg.png")).getEntity();
            assertEquals("/dA/abc/bg.png", withAsset.backgroundImage());
            assertEquals("/dA/abc/bg.png", companyAPI.getDefaultCompany().getHomeURL());

            final CompanyConfigView cleared = resource.saveBasicInfo(createAdminRequest(),
                    mockResponse, brandingForm(null)).getEntity();
            assertNull(cleared.backgroundImage());
            assertTrue(!com.dotmarketing.util.UtilMethods.isSet(
                    companyAPI.getDefaultCompany().getHomeURL()));
        } finally {
            snapshot.restore(companyAPI);
        }
    }

    // ==================== GET /v1/configuration/license (#37874) ====================

    /**
     * Any backend user gets the license of the running build. The test container can't resolve
     * the license file, so this also proves the endpoint answers 200 with the fallback text
     * instead of failing; header parsing is covered by {@code CompanyConfigHelperTest}.
     */
    @Test
    public void test_getLicense_asBackendUser_returnsLicenseInfo() {
        final LicenseInfoView info = resource.getLicense(
                createRequestForUser(nonAdminUser), mockResponse).getEntity();

        assertEquals(LicenseUtil.getLicenseText(), info.text());
        assertTrue(info.title(), info.title().startsWith("dotCMS Business Source License"));
    }

    /**
     * Front-end-only users and anonymous requests are rejected with 401.
     */
    @Test
    public void test_getLicense_frontEndOrAnonymous_throws401() throws Exception {
        final User frontEndUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadFrontEndUserRole(), frontEndUser);

        for (final HttpServletRequest request : new HttpServletRequest[]{
                createRequestForUser(frontEndUser), createRequestForUser(null)}) {
            try {
                resource.getLicense(request, mockResponse);
                fail("Expected 401");
            } catch (final SecurityException e) {
                assertEquals(401, e.getResponse().getStatus());
            }
        }
    }

    // ==================== Portlet gate (#37872) ====================

    /**
     * A non-admin with the Configuration portlet in their layout, and an anonymous request, are
     * rejected with 401 by every configuration endpoint.
     */
    @Test
    public void test_endpoints_nonAdminWithConfigurationPortlet_or_anonymous_throw401() {
        final ConfigurationResource configurationResource = new ConfigurationResource();
        for (final User user : new User[]{nonAdminUser, null}) {
            final java.util.List<Runnable> calls = java.util.List.of(
                    () -> resource.getCompanyConfig(createRequestForUser(user), mockResponse),
                    () -> resource.saveBasicInfo(createRequestForUser(user), mockResponse,
                            brandingForm(null)),
                    () -> resource.saveAuthType(createRequestForUser(user), mockResponse,
                            new CompanyAuthTypeForm(AuthType.EMAIL_ADDRESS)),
                    () -> resource.saveLocaleInfo(createRequestForUser(user), mockResponse,
                            new CompanyLocaleForm("en_US", "America/New_York")),
                    () -> {
                        try {
                            resource.regenerateKey(createRequestForUser(user), mockResponse);
                        } catch (final com.dotmarketing.exception.DotDataException
                                | com.dotmarketing.exception.DotSecurityException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    () -> {
                        try {
                            configurationResource.validateEmail(createRequestForUser(user),
                                    mockResponse, new CompanyEmailForm("test@dotcms.com"));
                        } catch (final java.util.concurrent.ExecutionException
                                | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
            for (final Runnable call : calls) {
                try {
                    call.run();
                    fail("Expected 401 for " + (user == null ? "anonymous" : "non-admin"));
                } catch (final SecurityException e) {
                    assertEquals(401, e.getResponse().getStatus());
                }
            }
        }
    }

    /**
     * Sending no body to the e-mail validation endpoint is a client error, not a server error.
     */
    @Test(expected = BadRequestException.class)
    public void test_validateEmail_nullBody_returns400() throws Exception {
        new ConfigurationResource().validateEmail(createAdminRequest(), mockResponse, null);
    }

    // ==================== PUT /v1/configuration/authentication ====================

    @Test
    public void test_saveAuthType_asAdmin_persistsAndReturnsUpdatedConfig() {
        final HttpServletRequest request = createAdminRequest();
        final Company original = companyAPI.getDefaultCompany();
        final String originalAuthType = original.getAuthType();

        try {
            final CompanyAuthTypeForm form = new CompanyAuthTypeForm(AuthType.EMAIL_ADDRESS);

            final ResponseEntityCompanyConfigView result =
                    resource.saveAuthType(request, mockResponse, form);

            assertNotNull(result);
            assertEquals(AuthType.EMAIL_ADDRESS, result.getEntity().authType());

            // Verify persistence
            final Company persisted = companyAPI.getDefaultCompany();
            assertEquals("emailAddress", persisted.getAuthType());
        } finally {
            try {
                final Company company = companyAPI.getDefaultCompany();
                company.setAuthType(originalAuthType);
                com.liferay.portal.ejb.CompanyManagerUtil.updateCompany(company);
            } catch (Exception e) {
                // best effort restore
            }
        }
    }

    @Test(expected = BadRequestException.class)
    public void test_saveAuthType_invalidType_throwsBadRequest() {
        // AuthType.fromString returns null for invalid values;
        // form validation catches it and throws BadRequestException
        final HttpServletRequest request = createAdminRequest();
        final CompanyAuthTypeForm form = new CompanyAuthTypeForm(AuthType.fromString("invalidType"));
        resource.saveAuthType(request, mockResponse, form);
    }

    @Test(expected = BadRequestException.class)
    public void test_saveAuthType_nullType_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyAuthTypeForm form = new CompanyAuthTypeForm(null);
        resource.saveAuthType(request, mockResponse, form);
    }

    @Test(expected = SecurityException.class)
    public void test_saveAuthType_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        final CompanyAuthTypeForm form = new CompanyAuthTypeForm(AuthType.EMAIL_ADDRESS);
        resource.saveAuthType(request, mockResponse, form);
    }

    // ==================== PUT /v1/configuration/locale ====================

    @Test
    public void test_saveLocaleInfo_asAdmin_succeeds() throws Exception {
        final HttpServletRequest request = createAdminRequest();

        // Snapshot locale state for restore
        final User origDefaultUser = APILocator.getUserAPI().getDefaultUser();
        final String origLanguageId = origDefaultUser.getLanguageId();
        final String origTimeZoneId = origDefaultUser.getTimeZoneId();
        final java.util.TimeZone origJvmTimeZone = java.util.TimeZone.getDefault();

        try {
            final CompanyLocaleForm form = new CompanyLocaleForm("en_US", "America/New_York");

            final ResponseEntityCompanyConfigView result =
                    resource.saveLocaleInfo(request, mockResponse, form);

            assertNotNull(result);
            final CompanyConfigView config = result.getEntity();
            assertNotNull(config);
            assertNotNull(config.companyId());
            assertEquals("en_US", config.languageId());
            assertEquals("America/New_York", config.timeZoneId());
        } finally {
            try {
                // Restore default user locale
                com.liferay.portal.ejb.CompanyManagerUtil.updateUsers(
                        origLanguageId, origTimeZoneId, null, false, false, null);
                java.util.TimeZone.setDefault(origJvmTimeZone);
            } catch (Exception e) {
                // best effort restore
            }
        }
    }

    @Test(expected = ValidationException.class)
    public void test_saveLocaleInfo_missingLanguage_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyLocaleForm form = new CompanyLocaleForm(null, "America/New_York");
        resource.saveLocaleInfo(request, mockResponse, form);
    }

    @Test(expected = ValidationException.class)
    public void test_saveLocaleInfo_missingTimezone_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyLocaleForm form = new CompanyLocaleForm("en_US", null);
        resource.saveLocaleInfo(request, mockResponse, form);
    }

    @Test(expected = SecurityException.class)
    public void test_saveLocaleInfo_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        final CompanyLocaleForm form = new CompanyLocaleForm("en_US", "America/New_York");
        resource.saveLocaleInfo(request, mockResponse, form);
    }

    // ==================== POST /v1/configuration/_regenerateKey ====================

    @Test
    public void test_regenerateKey_asAdmin_returnsNewDigest() throws Exception {
        final HttpServletRequest request = createAdminRequest();

        final String originalKey = companyAPI.getDefaultCompany().getKey();
        final String originalDigest = companyAPI.getDefaultCompany().getKeyDigest();

        try {
            final ResponseEntityStringView result =
                    resource.regenerateKey(request, mockResponse);

            assertNotNull(result);
            final String newDigest = result.getEntity();
            assertNotNull(newDigest);
            assertTrue("Key digest should not be empty", newDigest.length() > 0);
            assertNotEquals("Key digest should change after regeneration",
                    originalDigest, newDigest);

            // Verify persistence
            final String persistedDigest = companyAPI.getDefaultCompany().getKeyDigest();
            assertEquals(newDigest, persistedDigest);
        } finally {
            try {
                final Company company = companyAPI.getDefaultCompany();
                company.setKey(originalKey);
                com.liferay.portal.ejb.CompanyManagerUtil.updateCompany(company);
            } catch (Exception e) {
                // best effort restore
            }
        }
    }

    @Test(expected = SecurityException.class)
    public void test_regenerateKey_asNonAdmin_throwsSecurity() throws Exception {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.regenerateKey(request, mockResponse);
    }

    // ==================== Helpers ====================

    private HttpServletRequest createAdminRequest() {
        return createRequestForUser(adminUser);
    }

    private static HttpServletRequest createRequestForUser(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()
        ).request();

        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private static CompanyBasicInfoForm basicInfoForm(
            final String portalURL, final String email, final String mx,
            final String primaryColor, final String secondaryColor,
            final String bgColor, final String bgImage,
            final String loginLogo, final String navLogo) {
        return new CompanyBasicInfoForm(
                portalURL, email, mx,
                primaryColor, secondaryColor,
                bgColor, bgImage, loginLogo, navLogo);
    }

    /**
     * A valid branding form that differs only in its background image.
     */
    private static CompanyBasicInfoForm brandingForm(final String backgroundImage) {
        return basicInfoForm("http://localhost:8080", "test@dotcms.com", null,
                "#FF0000", "#00FF00", null, backgroundImage, null, null);
    }

    /**
     * Writes {@code homeURL} straight to the company row, the way the legacy screen stores it.
     */
    private static void storeHomeURL(final String homeURL) throws Exception {
        final Company company = companyAPI.getDefaultCompany();
        company.setHomeURL(homeURL);
        com.liferay.portal.ejb.CompanyManagerUtil.updateCompany(company);
    }

    /**
     * The branding fields of the default company, captured so a test can put them back.
     */
    private static final class CompanySnapshot {

        private final String portalURL, email, mx, type, street, size, homeURL, city, state;

        private CompanySnapshot(final Company company) {
            portalURL = company.getPortalURL();
            email = company.getEmailAddress();
            mx = company.getMx();
            type = company.getType();
            street = company.getStreet();
            size = company.getSize();
            homeURL = company.getHomeURL();
            city = company.getCity();
            state = company.getState();
        }

        static CompanySnapshot take(final CompanyAPI companyAPI) {
            return new CompanySnapshot(companyAPI.getDefaultCompany());
        }

        void restore(final CompanyAPI companyAPI) {
            try {
                final Company company = companyAPI.getDefaultCompany();
                company.setPortalURL(portalURL);
                company.setEmailAddress(email);
                company.setMx(mx);
                company.setType(type);
                company.setStreet(street);
                company.setSize(size);
                company.setHomeURL(homeURL);
                company.setCity(city);
                company.setState(state);
                com.liferay.portal.ejb.CompanyManagerUtil.updateCompany(company);
            } catch (Exception e) {
                // best effort restore
            }
        }
    }

    private void restoreCompany(
            final String portalURL, final String email, final String mx,
            final String type, final String street, final String size,
            final String homeURL, final String city, final String state) {
        try {
            final Company company = companyAPI.getDefaultCompany();
            company.setPortalURL(portalURL);
            company.setEmailAddress(email);
            company.setMx(mx);
            company.setType(type);
            company.setStreet(street);
            company.setSize(size);
            company.setHomeURL(homeURL);
            company.setCity(city);
            company.setState(state);
            com.liferay.portal.ejb.CompanyManagerUtil.updateCompany(company);
        } catch (Exception e) {
            // best effort restore
        }
    }
}
