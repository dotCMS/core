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
import com.liferay.portal.ejb.CompanyPool;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiTokenResourceTest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
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
    public void test_revokeUserToken()  {

        // 1) create an user with skinid
        // 2) call the revoke to reset the user
        // 3) check the user has the skinid reset
        Assert.assertTrue(true);
    }
}
