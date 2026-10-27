package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.CompanyDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.NoSuchCompanyException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiTokenResourceTest {

    private static final List<User> createdUsers = new ArrayList<>();
    private static final List<ApiToken> createdTokens = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Deletes the users and tokens created by the tests that go through {@link #newUser()} and
     * {@link #persistToken(User)}, tokens first so no token outlives its owner.
     */
    @AfterClass
    public static void cleanUp() {

        for (final ApiToken token : createdTokens) {
            try {
                APILocator.getApiTokenAPI().deleteToken(token, APILocator.systemUser());
            } catch (Exception e) {
                Logger.warn(ApiTokenResourceTest.class, "Unable to delete token " + token.getId(), e);
            }
        }
        createdUsers.forEach(UserDataGen::remove);
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

        final HttpServletRequest  request     = mock(HttpServletRequest.class);
        final HttpServletResponse response    = mock(HttpServletResponse.class);
        final WebResource         webResource = mock(WebResource.class);
        final ApiTokenResource    resource    = new ApiTokenResource(APILocator.getApiTokenAPI(), webResource);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final InitDataObject initData = new InitDataObject();
        initData.setUser(limitedUser);
        // issueApiToken uses the 5-param init overload
        when(webResource.init(any(), anyBoolean(), any(), anyBoolean(), any())).thenReturn(initData);

        // Pass the user's OWN userId explicitly — this is what the UI does and what triggered the bug
        final ApiTokenForm form = new ApiTokenForm.Builder()
                .userId(limitedUser.getUserId())
                .expirationSeconds(3600)
                .build();

        final Response restResponse = resource.issueApiToken(request, response, form);
        assertNotNull(restResponse);
        assertEquals("Non-admin user should be able to issue a token for themselves",
                Response.Status.OK.getStatusCode(), restResponse.getStatus());

        // Creation is the only time the JWT is returned, so it must be in this response.
        final Map<String, Object> entity = ((ResponseEntityMapView) restResponse.getEntity()).getEntity();
        assertTrue("The JWT should be returned when the token is created",
                entity.get("jwt") instanceof String && !((String) entity.get("jwt")).isEmpty());
    }

    /**
     * Method to test: {@link ApiTokenResource#issueApiToken}
     * Given Scenario: A non-admin user tries to create a token for a different user.
     * ExpectedResult: The request is rejected — only admins can issue tokens for other users.
     * @see <a href="https://github.com/dotCMS/core/issues/35094">Issue #35094</a>
     */
    @Test
    public void test_issueApiToken_nonAdminUser_cannotCreateTokenForOtherUser()
            throws DotDataException, DotSecurityException {

        final User limitedUser  = new UserDataGen().active(true).nextPersisted();
        final User anotherUser  = new UserDataGen().active(true).nextPersisted();

        final HttpServletRequest  request     = mock(HttpServletRequest.class);
        final HttpServletResponse response    = mock(HttpServletResponse.class);
        final WebResource         webResource = mock(WebResource.class);
        final ApiTokenResource    resource    = new ApiTokenResource(APILocator.getApiTokenAPI(), webResource);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final InitDataObject initData = new InitDataObject();
        initData.setUser(limitedUser);
        when(webResource.init(any(), anyBoolean(), any(), anyBoolean(), any())).thenReturn(initData);

        // Attempt to create a token for a different user
        final ApiTokenForm form = new ApiTokenForm.Builder()
                .userId(anotherUser.getUserId())
                .expirationSeconds(3600)
                .build();

        // The other user is not readable by the requester, so the resource answers 404 before the admin check
        final Response restResponse = resource.issueApiToken(request, response, form);
        assertEquals("A non-admin user should get 404 for a token request on another user",
                Response.Status.NOT_FOUND.getStatusCode(), restResponse.getStatus());
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

    /**
     * Method to test: {@link ApiTokenResource#getApiTokens}
     * Given Scenario: The owner of a token lists their own tokens.
     * ExpectedResult: The token is listed and the response does not contain its JWT.
     */
    @Test
    public void test_getApiTokens_owner_listsOwnTokenWithoutJwt() throws Exception {

        final User owner = newUser();
        final ApiToken token = persistToken(owner);

        final Response restResponse = resourceFor(owner).getApiTokens(
                mock(HttpServletRequest.class), mock(HttpServletResponse.class), owner.getUserId(), false);

        assertEquals(Response.Status.OK.getStatusCode(), restResponse.getStatus());
        assertTrue("The owner should see their own token", tokenIds(restResponse).contains(token.getId()));
        assertResponseHasNoJwt(restResponse);
    }

    /**
     * Method to test: {@link ApiTokenResource#getApiTokens}
     * Given Scenario: A CMS administrator lists the tokens of another user.
     * ExpectedResult: The token is listed so it can be revoked, but its JWT is not returned.
     */
    @Test
    public void test_getApiTokens_admin_listsOtherUsersTokenWithoutJwt() throws Exception {

        final User owner = newUser();
        final User admin = newCmsAdmin();
        final ApiToken token = persistToken(owner);

        final Response restResponse = resourceFor(admin).getApiTokens(
                mock(HttpServletRequest.class), mock(HttpServletResponse.class), owner.getUserId(), false);

        assertEquals(Response.Status.OK.getStatusCode(), restResponse.getStatus());
        assertTrue("An admin should see the other user's token", tokenIds(restResponse).contains(token.getId()));
        assertResponseHasNoJwt(restResponse);
    }

    /**
     * Method to test: {@link ApiTokenResource#getApiTokens}
     * Given Scenario: A non-admin user without user-management rights lists another user's tokens.
     * ExpectedResult: No tokens are returned.
     */
    @Test
    public void test_getApiTokens_otherNonAdminUser_seesNoTokens() throws Exception {

        final User owner = newUser();
        final User other = newUser();
        persistToken(owner);

        final Response restResponse = resourceFor(other).getApiTokens(
                mock(HttpServletRequest.class), mock(HttpServletResponse.class), owner.getUserId(), false);

        assertEquals(Response.Status.OK.getStatusCode(), restResponse.getStatus());
        assertTrue("A user without rights should not see another user's tokens", tokenIds(restResponse).isEmpty());
    }

    /**
     * Method to test: {@link ApiTokenResource#revokeApiToken}
     * Given Scenario: A non-admin user without user-management rights tries to revoke another user's token.
     * ExpectedResult: The request is rejected and the token stays active.
     */
    @Test
    public void test_revokeApiToken_otherNonAdminUser_isRejected() throws Exception {

        final User owner = newUser();
        final User other = newUser();
        final ApiToken token = persistToken(owner);

        assertThrows(DotStateException.class, () -> resourceFor(other).revokeApiToken(
                mock(HttpServletRequest.class), mock(HttpServletResponse.class), token.getId()));
        assertFalse("The token should not be revoked",
                APILocator.getApiTokenAPI().findApiToken(token.getId()).orElseThrow().isRevoked());
    }

    /**
     * Method to test: {@link ApiTokenResource#deleteApiToken}
     * Given Scenario: A non-admin user without user-management rights tries to delete another user's token.
     * ExpectedResult: The request is rejected and the token still exists.
     */
    @Test
    public void test_deleteApiToken_otherNonAdminUser_isRejected() throws Exception {

        final User owner = newUser();
        final User other = newUser();
        final ApiToken token = persistToken(owner);

        assertThrows(DotStateException.class, () -> resourceFor(other).deleteApiToken(
                mock(HttpServletRequest.class), mock(HttpServletResponse.class), token.getId()));
        assertTrue("The token should still exist",
                APILocator.getApiTokenAPI().findApiToken(token.getId()).isPresent());
    }

    /**
     * Method to test: {@link ApiTokenResource#revokeApiToken}
     * Given Scenario: A CMS administrator revokes another user's token.
     * ExpectedResult: The token is revoked.
     */
    @Test
    public void test_revokeApiToken_admin_canRevokeOtherUsersToken() throws Exception {

        final User owner = newUser();
        final User admin = newCmsAdmin();
        final ApiToken token = persistToken(owner);

        final Response restResponse = resourceFor(admin).revokeApiToken(
                mock(HttpServletRequest.class), mock(HttpServletResponse.class), token.getId());

        assertEquals(Response.Status.OK.getStatusCode(), restResponse.getStatus());
        assertTrue("The token should be revoked",
                APILocator.getApiTokenAPI().findApiToken(token.getId()).orElseThrow().isRevoked());
    }

    private static ApiTokenResource resourceFor(final User user) {

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initData = new InitDataObject();
        initData.setUser(user);
        when(webResource.init(any(), anyBoolean(), any(), anyBoolean(), any())).thenReturn(initData);
        return new ApiTokenResource(APILocator.getApiTokenAPI(), webResource);
    }

    private static User newUser() throws DotDataException {

        final User user = new UserDataGen().active(true).nextPersisted();
        createdUsers.add(user);
        return user;
    }

    private static ApiToken persistToken(final User owner) {

        final ApiToken token = APILocator.getApiTokenAPI().persistApiToken(owner.getUserId(),
                Date.from(Instant.now().plus(1, ChronoUnit.DAYS)), owner.getUserId(), "127.0.0.1");
        createdTokens.add(token);
        return token;
    }

    private static User newCmsAdmin() throws DotDataException {

        final User admin = newUser();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadCMSAdminRole(), admin);
        return admin;
    }

    @SuppressWarnings("unchecked")
    private static List<String> tokenIds(final Response restResponse) {

        final Map<String, Object> entity = ((ResponseEntityMapView) restResponse.getEntity()).getEntity();
        return ((List<ApiToken>) entity.get("tokens")).stream().map(ApiToken::getId)
                .collect(Collectors.toList());
    }

    /**
     * Asserts that a token listing carries no JWT. A JWT is signed with a new random id each time,
     * so there is no stored value to compare against; instead this checks for a {@code jwt} field
     * and for any value shaped like a JWT, whose header always encodes to a string starting with
     * {@code eyJ}.
     */
    private static void assertResponseHasNoJwt(final Response restResponse) throws Exception {

        final String json = DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                .writeValueAsString(restResponse.getEntity());
        assertFalse("The token list should not have a jwt field", json.contains("\"jwt\""));
        assertFalse("The token list should not contain a JWT", json.contains("eyJ"));
    }
}
