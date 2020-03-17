package com.dotcms.filters.interceptor.jwt;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.factories.KeyFactoryUtils;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.security.Encryptor;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyLocalManager;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.EncryptorException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * The goal of this unit test is to try some scenarios for the json web token interceptor.
 * @author jsanca
 */
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
@PrepareForTest({ClusterFactory.class})
@RunWith(PowerMockRunner.class)
public class JsonWebTokenInterceptorTest extends UnitTestBase {

    final String clusterId = "CLUSTER-123";

    /**
     * Test the scenario when the user is already logged in, means does not need any process
     * @throws IOException
     */
    @Test
    public void interceptLoggedUserTest() throws IOException {

        final HttpServletRequest   request  = mock(HttpServletRequest.class);
        final HttpServletResponse  response = mock(HttpServletResponse.class);
        final HttpSession          session  = mock(HttpSession.class);
        final LoginServiceAPI loginService  = mock(LoginServiceAPI.class);

        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(null, null, null, loginService, null);

        when(request.getSession(false)).thenReturn(session); //
        when(request.isSecure()).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                fail("On Logged user, should not call the is Secure");
                return true;
            }
        });
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn("userId"); // user logged
        when(loginService.isLoggedIn(request)).thenReturn(true);

        jsonWebTokenInterceptor.init();

        jsonWebTokenInterceptor.intercept
                (request, response);

        jsonWebTokenInterceptor.destroy();
    }

    /**
     * Test the scenario when the protocol is not https
     * @throws IOException
     */
    @Test
    public void interceptOnNonHttpsTest() throws IOException {

        final HttpServletRequest   request  = mock(HttpServletRequest.class);
        final HttpServletResponse  response = mock(HttpServletResponse.class);
        final HttpSession          session  = mock(HttpSession.class);
        final LoginServiceAPI loginService  = mock(LoginServiceAPI.class);

        boolean allowHttp = Config.getBooleanProperty(JsonWebTokenInterceptor.JSON_WEB_TOKEN_ALLOW_HTTP, false);
        if (allowHttp) {
            Config.setProperty(JsonWebTokenInterceptor.JSON_WEB_TOKEN_ALLOW_HTTP, false);
        }

        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(null, null, null, loginService, null);

        when(loginService.isLoggedIn(request)).thenReturn(false);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(null); // non-logged
        when(request.isSecure()).thenReturn(false); // no https
        when(request.getCookies()).thenAnswer(new Answer<Cookie[]>() {

            @Override
            public Cookie[] answer(InvocationOnMock invocation) throws Throwable {

                fail("On non https, should not call the method getCookies");
                return new Cookie[0];
            }
        });

        jsonWebTokenInterceptor.init();

        jsonWebTokenInterceptor.intercept
                (request, response);

        jsonWebTokenInterceptor.destroy();
    }

    /**
     * Test the scenario when there is not any token in the cookies
     * @throws IOException
     */
    @Test
    public void interceptNonAccessTokenTest() throws IOException {

        final HttpServletRequest   request  = mock(HttpServletRequest.class);
        final HttpServletResponse  response = mock(HttpServletResponse.class);
        final HttpSession          session  = mock(HttpSession.class);
        final JsonWebTokenService  jsonWebTokenService =
                mock(JsonWebTokenService.class);
        final LoginServiceAPI loginService  = mock(LoginServiceAPI.class);

        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(null, null, null, loginService, null);

        when(loginService.isLoggedIn(request)).thenReturn(false);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(null); // non-logged
        when(request.isSecure()).thenReturn(true); // https
        when(request.getCookies()).thenAnswer(new Answer<Cookie[]>() {

            @Override
            public Cookie[] answer(InvocationOnMock invocation) throws Throwable {

                System.out.println("Gettings cookies");
                return new Cookie[0];
            }
        });
        when(jsonWebTokenService.parseToken(anyString())).thenAnswer(new Answer<UserToken>() {

            @Override
            public UserToken answer(InvocationOnMock invocation) throws Throwable {

                fail("On no access token should not call the method parseToken");
                return new UserToken("jwtId", "subject", new Date(), 0l);
            }
        });

        jsonWebTokenInterceptor.setJsonWebTokenService(jsonWebTokenService);
        jsonWebTokenInterceptor.init();

        jsonWebTokenInterceptor.intercept(request, response);

        jsonWebTokenInterceptor.destroy();
    }

}
