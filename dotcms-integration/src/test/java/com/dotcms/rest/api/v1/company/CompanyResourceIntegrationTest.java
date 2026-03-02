package com.dotcms.rest.api.v1.company;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.company.CompanyAPI;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
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
    }

    // ==================== GET /v1/company ====================

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
        assertEquals(company.getAuthType(), config.authType());
        assertEquals(company.getType(), config.primaryColor());
        assertEquals(company.getStreet(), config.secondaryColor());
    }

    @Test(expected = SecurityException.class)
    public void test_getCompanyConfig_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.getCompanyConfig(request, mockResponse);
    }

    // ==================== PUT /v1/company/basic-info ====================

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

    @Test(expected = BadRequestException.class)
    public void test_saveBasicInfo_missingPortalURL_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyBasicInfoForm form = basicInfoForm(
                null, "test@dotcms.com", null,
                "#FF0000", "#00FF00", null, null, null, null);
        resource.saveBasicInfo(request, mockResponse, form);
    }

    @Test(expected = BadRequestException.class)
    public void test_saveBasicInfo_missingPrimaryColor_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyBasicInfoForm form = basicInfoForm(
                "http://localhost:8080", "test@dotcms.com", null,
                null, "#00FF00", null, null, null, null);
        resource.saveBasicInfo(request, mockResponse, form);
    }

    @Test(expected = BadRequestException.class)
    public void test_saveBasicInfo_invalidLoginLogoPath_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyBasicInfoForm form = basicInfoForm(
                "http://localhost:8080", "test@dotcms.com", null,
                "#FF0000", "#00FF00", null, null,
                "http://external.com/logo.png", null);
        resource.saveBasicInfo(request, mockResponse, form);
    }

    @Test(expected = BadRequestException.class)
    public void test_saveBasicInfo_invalidNavBarLogoPath_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyBasicInfoForm form = basicInfoForm(
                "http://localhost:8080", "test@dotcms.com", null,
                "#FF0000", "#00FF00", null, null, null,
                "http://external.com/nav.png");
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

    // ==================== PUT /v1/company/auth-type ====================

    @Test
    public void test_saveAuthType_asAdmin_persistsAndReturnsUpdatedConfig() {
        final HttpServletRequest request = createAdminRequest();
        final Company original = companyAPI.getDefaultCompany();
        final String originalAuthType = original.getAuthType();

        try {
            final CompanyAuthTypeForm form = new CompanyAuthTypeForm("emailAddress");

            final ResponseEntityCompanyConfigView result =
                    resource.saveAuthType(request, mockResponse, form);

            assertNotNull(result);
            assertEquals("emailAddress", result.getEntity().authType());

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
        final HttpServletRequest request = createAdminRequest();
        final CompanyAuthTypeForm form = new CompanyAuthTypeForm("invalidType");
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
        final CompanyAuthTypeForm form = new CompanyAuthTypeForm("emailAddress");
        resource.saveAuthType(request, mockResponse, form);
    }

    // ==================== PUT /v1/company/locale-info ====================

    @Test
    public void test_saveLocaleInfo_asAdmin_succeeds() {
        final HttpServletRequest request = createAdminRequest();

        final CompanyLocaleForm form = new CompanyLocaleForm("en_US", "America/New_York");

        final ResponseEntityStringView result =
                resource.saveLocaleInfo(request, mockResponse, form);

        assertNotNull(result);
        assertEquals("OK", result.getEntity());
    }

    @Test(expected = BadRequestException.class)
    public void test_saveLocaleInfo_missingLanguage_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final CompanyLocaleForm form = new CompanyLocaleForm(null, "America/New_York");
        resource.saveLocaleInfo(request, mockResponse, form);
    }

    @Test(expected = BadRequestException.class)
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

    // ==================== POST /v1/company/_regenerateKey ====================

    @Test
    public void test_regenerateKey_asAdmin_returnsNewDigest() throws Exception {
        final HttpServletRequest request = createAdminRequest();

        final String originalDigest = companyAPI.getDefaultCompany().getKeyDigest();

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
