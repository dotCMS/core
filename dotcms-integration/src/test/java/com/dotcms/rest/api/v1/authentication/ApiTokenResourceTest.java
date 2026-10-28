package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.CompanyDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.NoSuchCompanyException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiTokenResourceTest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Builds a request coming from localhost: a persisted token requires the requesting IP.
     */
    private static HttpServletRequest requestFromLocalhost() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        return request;
    }

    /**
     * Builds the resource for a request made by {@code user}, the way the container does.
     */
    private static ApiTokenResource resourceFor(final User user) throws DotDataException, DotSecurityException {
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initData = new InitDataObject();
        initData.setUser(user);
        // issueApiToken uses the 5-param init overload
        when(webResource.init(any(), anyBoolean(), any(), anyBoolean(), any())).thenReturn(initData);
        return new ApiTokenResource(APILocator.getApiTokenAPI(), webResource);
    }

    /**
     * Method to test: {@link ApiTokenResource#issueApiToken}
     * Given Scenario: the caller asks for a token restricted to a network, but the value is not
     *                 CIDR notation (a bare address, or a mask out of range).
     * ExpectedResult: HTTP 400 and no token is issued. A network the server cannot parse is never
     *                 silently dropped, so the caller cannot end up with a token that is less
     *                 restricted than the one they asked for.
     */
    @Test
    public void test_issueApiToken_invalidNetwork_isRejected() throws DotDataException, DotSecurityException {

        final User limitedUser = new UserDataGen().active(true).nextPersisted();
        final ApiTokenResource resource = resourceFor(limitedUser);
        final HttpServletRequest  request  = requestFromLocalhost();
        final HttpServletResponse response = mock(HttpServletResponse.class);

        for (final String badNetwork : new String[] {"203.0.113.5", "10.0.0.0/33", "not-a-network"}) {
            final ApiTokenForm form = new ApiTokenForm.Builder()
                    .expirationSeconds(3600)
                    .netmask(badNetwork)
                    .build();

            final Response restResponse = resource.issueApiToken(request, response, form);
            assertEquals("network '" + badNetwork + "' must be rejected",
                    Response.Status.BAD_REQUEST.getStatusCode(), restResponse.getStatus());
        }
        assertTrue("no token must be issued for a rejected network",
                APILocator.getApiTokenAPI().findApiTokensByUserId(limitedUser.getUserId(), false,
                        APILocator.systemUser()).isEmpty());
    }

    /**
     * Method to test: {@link ApiTokenResource#issueApiToken}
     * Given Scenario: the caller passes a valid CIDR, the "any network" wildcard, or no network.
     * ExpectedResult: HTTP 200; the CIDR is kept on the token and the other two leave it unrestricted.
     */
    @Test
    public void test_issueApiToken_validNetwork_isKept() throws DotDataException, DotSecurityException {

        final User limitedUser = new UserDataGen().active(true).nextPersisted();
        final ApiTokenResource resource = resourceFor(limitedUser);
        final HttpServletRequest  request  = requestFromLocalhost();
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final Map<String, String> expectedNetworkByInput = new LinkedHashMap<>();
        expectedNetworkByInput.put("10.0.0.0/8", "10.0.0.0/8");
        expectedNetworkByInput.put("0.0.0.0/0", null);
        expectedNetworkByInput.put(null, null);

        for (final Map.Entry<String, String> entry : expectedNetworkByInput.entrySet()) {
            final ApiTokenForm form = new ApiTokenForm.Builder()
                    .expirationSeconds(3600)
                    .netmask(entry.getKey())
                    .build();

            final Response restResponse = resource.issueApiToken(request, response, form);
            assertEquals("network '" + entry.getKey() + "' must be accepted",
                    Response.Status.OK.getStatusCode(), restResponse.getStatus());
            @SuppressWarnings("unchecked")
            final Map<String, Object> entity = (Map<String, Object>) ((ResponseEntityView<?>) restResponse.getEntity()).getEntity();
            final ApiToken issued = (ApiToken) entity.get("token");
            assertEquals("network stored for '" + entry.getKey() + "'", entry.getValue(), issued.allowNetwork);
        }
    }

    /**
     * Method to test: {@link ApiTokenResource#issueApiToken}
     * Given Scenario: A non-admin user explicitly passes their own userId in the form body.
     *                 Before the fix, getUserById() returned a new DB object (different reference),
     *                 causing the requestingUser != forUser reference check to fire incorrectly.
     * ExpectedResult: The token is issued successfully (HTTP 200).
     * @see <a href="https://github.com/dotCMS/core/issues/35094">Issue #35094</a>
     */
    @Test
    public void test_issueApiToken_nonAdminUser_canCreateOwnToken()
            throws DotDataException, DotSecurityException {

        final User limitedUser = new UserDataGen().active(true).nextPersisted();

        final HttpServletRequest  request     = requestFromLocalhost();
        final HttpServletResponse response    = mock(HttpServletResponse.class);
        final ApiTokenResource    resource    = resourceFor(limitedUser);

        // Pass the user's OWN userId explicitly — this is what the UI does and what triggered the bug
        final ApiTokenForm form = new ApiTokenForm.Builder()
                .userId(limitedUser.getUserId())
                .expirationSeconds(3600)
                .build();

        final Response restResponse = resource.issueApiToken(request, response, form);
        assertNotNull(restResponse);
        assertEquals("Non-admin user should be able to issue a token for themselves",
                Response.Status.OK.getStatusCode(), restResponse.getStatus());
    }

    /**
     * Method to test: {@link ApiTokenResource#issueApiToken}
     * Given Scenario: A non-admin user tries to create a token for a different user.
     * ExpectedResult: The request is rejected and no token is issued for the other user. A
     *                 non-admin cannot even load the other account, so the resource answers 404
     *                 ("No user found") before the admin-only check is reached.
     * @see <a href="https://github.com/dotCMS/core/issues/35094">Issue #35094</a>
     */
    @Test
    public void test_issueApiToken_nonAdminUser_cannotCreateTokenForOtherUser()
            throws DotDataException, DotSecurityException {

        final User limitedUser  = new UserDataGen().active(true).nextPersisted();
        final User anotherUser  = new UserDataGen().active(true).nextPersisted();

        final HttpServletRequest  request     = requestFromLocalhost();
        final HttpServletResponse response    = mock(HttpServletResponse.class);
        final ApiTokenResource    resource    = resourceFor(limitedUser);

        // Attempt to create a token for a different user
        final ApiTokenForm form = new ApiTokenForm.Builder()
                .userId(anotherUser.getUserId())
                .expirationSeconds(3600)
                .build();

        final Response restResponse = resource.issueApiToken(request, response, form);
        assertEquals("Non-admin user must not issue a token for another user",
                Response.Status.NOT_FOUND.getStatusCode(), restResponse.getStatus());
        assertTrue("no token must be issued for the other user",
                APILocator.getApiTokenAPI().findApiTokensByUserId(anotherUser.getUserId(), false,
                        APILocator.systemUser()).isEmpty());
    }

    @Test()
    public void test_revokeUserToken_non_admin_user() throws DotDataException, DotSecurityException {

        final String skinId    = UUIDGenerator.generateUuid();
        final User limitedUser = new UserDataGen().active(true)
                .skinId(skinId).nextPersisted();
        final HttpServletRequest  request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource      webResource = mock(WebResource.class);
        final ApiTokenResource apiTokenResource = new ApiTokenResource(APILocator.getApiTokenAPI(), webResource);

        when(request.getAttribute(WebKeys.USER)).thenReturn(limitedUser);
        final InitDataObject initDataObject = new InitDataObject();
        initDataObject.setUser(limitedUser);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);
        final Response restResponse = apiTokenResource.revokeUserToken(request, response, limitedUser.getUserId());
        Assert.assertNotNull(restResponse);
        RestUtilTest.verifyErrorResponse(restResponse,  Response.Status.UNAUTHORIZED.getStatusCode(), "unauthorized to remove the token");
    }

    @Test
    public void test_revokeUserToken() throws DotSecurityException, DotDataException, SystemException, NoSuchCompanyException {

        // 1) create an user with skinid
        // 2) call the revoke to reset the user
        // 3) check the user has the skinid reset

        final Company company = new CompanyDataGen()
                .name("TestCompany")
                .shortName("TC")
                .authType("email")
                .autoLogin(true)
                .emailAddress("lol@dotCMS.com")
                .homeURL("localhost")
                .city("NYC")
                .mx("MX")
                .type("test")
                .phone("5552368")
                .portalURL("/portalURL")
                .nextPersisted();
        assertNotNull(company.getCompanyId());
        final Company retrievedCompany =  CompanyUtil.findByPrimaryKey(company.getCompanyId());
        assertEquals(company.getCompanyId(), retrievedCompany.getCompanyId());

        final String skinId    = UUIDGenerator.generateUuid();
        final User limitedUser = new UserDataGen().active(true)
                .skinId(skinId).companyId(retrievedCompany.getCompanyId()).nextPersisted();

        final User adminUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadCMSAdminRole(), adminUser);
        assertTrue(APILocator.getUserAPI().isCMSAdmin(adminUser));

        final HttpServletRequest  request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource      webResource = mock(WebResource.class);
        final ApiTokenResource apiTokenResource = new ApiTokenResource(APILocator.getApiTokenAPI(), webResource);

        when(request.getAttribute(WebKeys.USER)).thenReturn(adminUser);
        final InitDataObject initDataObject = new InitDataObject();
        initDataObject.setUser(adminUser);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);
        final Response restResponse = apiTokenResource.revokeUserToken(request, response, limitedUser.getUserId());
        Assert.assertNotNull(restResponse);
        assertEquals(restResponse.getStatus(), Response.Status.OK.getStatusCode());

        final User modifiedUser = APILocator.getUserAPI().loadUserById(limitedUser.getUserId());
        assertNotNull(modifiedUser);
        assertNotEquals(skinId, modifiedUser.getRememberMeToken());
    }
}
