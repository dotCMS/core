package com.dotcms.rest.api.v1.authentication;

import com.dotcms.datagen.CompanyDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.InitDataObject;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
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

        final InitDataObject initData = new InitDataObject();
        initData.setUser(limitedUser);
        when(webResource.init(any(), anyBoolean(), any(), anyBoolean(), any())).thenReturn(initData);

        // Attempt to create a token for a different user
        final ApiTokenForm form = new ApiTokenForm.Builder()
                .userId(anotherUser.getUserId())
                .expirationSeconds(3600)
                .build();

        final DotDataException e = assertThrows(DotDataException.class,
                () -> resource.issueApiToken(request, response, form));
        assertTrue("Exception message should indicate admin-only restriction",
                e.getMessage().contains("Only Admin user"));
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
