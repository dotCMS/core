package com.dotcms.filters.interceptor.jwt;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
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
        when(jsonWebTokenService.parseToken(anyString())).thenAnswer(new Answer<JWTBean>() {

            @Override
            public JWTBean answer(InvocationOnMock invocation) throws Throwable {

                fail("On no access token should not call the method parseToken");
                return new JWTBean("jwtId", "subject", new Date(), 0l);
            }
        });

        jsonWebTokenInterceptor.setJsonWebTokenService(jsonWebTokenService);
        jsonWebTokenInterceptor.init();

        jsonWebTokenInterceptor.intercept(request, response);

        jsonWebTokenInterceptor.destroy();
    }

    /**
     * Test the scenario when the ttl time is not valid for the token
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void interceptWithAccessTokenNonValidTest() throws IOException, ParseException {

        //Mocking data
        PowerMockito.mockStatic(ClusterFactory.class);
        PowerMockito.when(ClusterFactory.getClusterId()).thenReturn(clusterId);
        final HttpServletRequest   request          = mock(HttpServletRequest.class);
        final HttpServletResponse  response         = mock(HttpServletResponse.class);
        final HttpSession          session          = mock(HttpSession.class);
        final FileAssetAPI         fileAssetAPI     = mock(FileAssetAPI.class);
        Config.CONTEXT = mock(ServletContext.class);
        Config.CONTEXT_PATH = "/tmp";
        KeyFactoryUtils.getInstance(fileAssetAPI);
        when(fileAssetAPI.getRealAssetsRootPath()).thenReturn("/tmp/assets");

        final JsonWebTokenService jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        final LoginServiceAPI loginService  = mock(LoginServiceAPI.class);
        final UserAPI         userAPI       = mock(UserAPI.class);

        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(jsonWebTokenService, null, null, loginService, userAPI);

        when(loginService.isLoggedIn(request)).thenReturn(false);

        final String jwtId  = "jwt1";
        final String userId = "jsanca";
        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);
        final Date date = dateFormat.parse("04/10/1981");

        final String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                userId, date, date.getTime() - System.currentTimeMillis()
        ));

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(null); // non-logged
        when(request.isSecure()).thenReturn(true); // https
        when(request.getCookies()).thenAnswer(new Answer<Cookie[]>() {

            @Override
            public Cookie[] answer(InvocationOnMock invocation) throws Throwable {

                System.out.println("getting the access token:" + jsonWebToken);
                return new Cookie[] {
                        new Cookie(CookieKeys.JWT_ACCESS_TOKEN, jsonWebToken)
                };
            }
        });

        jsonWebTokenInterceptor.init();

        jsonWebTokenInterceptor.intercept(request, response);

        jsonWebTokenInterceptor.destroy();
    }

    /**
     * Test the scenario when the user from the data base (mocked) has been modified.
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void interceptWithAccessTokenUserModifiedTest() throws IOException, ParseException, SystemException, PortalException, EncryptorException, DotSecurityException, DotDataException {

        //Mocking data
        PowerMockito.mockStatic(ClusterFactory.class);
        PowerMockito.when(ClusterFactory.getClusterId()).thenReturn(clusterId);
        final HttpServletRequest   request          = mock(HttpServletRequest.class);
        final HttpServletResponse  response         = mock(HttpServletResponse.class);
        final HttpSession          session          = mock(HttpSession.class);
        final FileAssetAPI         fileAssetAPI     = mock(FileAssetAPI.class);
        Config.CONTEXT = mock(ServletContext.class);
        Config.CONTEXT_PATH = "/tmp";
        KeyFactoryUtils.getInstance(fileAssetAPI);
        when(fileAssetAPI.getRealAssetsRootPath()).thenReturn("/tmp/assets");

        final JsonWebTokenService jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        final CompanyLocalManager companyLocalManager =
                mock(CompanyLocalManager.class);
        final UserAPI userAPI     = mock(UserAPI.class);
        final Encryptor encryptor = mock(Encryptor.class);
        final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(jsonWebTokenService, null, null, loginService, userAPI);

        when(loginService.isLoggedIn(request)).thenReturn(false);

        final String jwtId  = "jwt1";
        final String userId = "jsanca";
        final String clusterId = "CLUSTER-123";
        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);

        final Date date = dateFormat.parse("04/10/1981");

        final String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                userId, date, date.getTime()
        ));

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(null); // non-logged
        when(request.isSecure()).thenReturn(true); // https
        when(request.getCookies()).thenAnswer(new Answer<Cookie[]>() {

            @Override
            public Cookie[] answer(InvocationOnMock invocation) throws Throwable {

                System.out.println("getting the access token:" + jsonWebToken);
                return new Cookie[] {
                        new Cookie(CookieKeys.JWT_ACCESS_TOKEN, jsonWebToken)
                };
            }
        });

        when(userAPI.loadUserById(userId)).thenAnswer(new Answer<User>() {

            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {

                final User user = new User();

                user.setModificationDate(new Date());

                return user;
            }
        });

        when(loginService.doCookieLogin(anyObject(), anyObject(), anyObject())).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                fail("Since the modification date of the token user and db user are diff, should not call to this method");
                return true;
            }
        });

        jsonWebTokenInterceptor.setCompanyLocalManager(companyLocalManager);
        jsonWebTokenInterceptor.setEncryptor(encryptor);
        jsonWebTokenInterceptor.setLoginService(loginService);

        jsonWebTokenInterceptor.init();

        jsonWebTokenInterceptor.intercept(request, response);

        jsonWebTokenInterceptor.destroy();
    }

    private boolean calledDoCookieLogin;

    /**
     * Test the scenario when the user from the data base (mocked) has been modified.
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void interceptWithAccessTokenTest() throws IOException, ParseException, SystemException, PortalException, EncryptorException, DotSecurityException, DotDataException {

        //Mocking data
        PowerMockito.mockStatic(ClusterFactory.class);
        PowerMockito.when(ClusterFactory.getClusterId()).thenReturn(clusterId);
        final HttpServletRequest   request          = mock(HttpServletRequest.class);
        final HttpServletResponse  response         = mock(HttpServletResponse.class);
        final HttpSession          session          = mock(HttpSession.class);
        final FileAssetAPI         fileAssetAPI     = mock(FileAssetAPI.class);
        Config.CONTEXT = mock(ServletContext.class);
        Config.CONTEXT_PATH = "/tmp";
        KeyFactoryUtils.getInstance(fileAssetAPI);
        when(fileAssetAPI.getRealAssetsRootPath()).thenReturn("/tmp/assets");

        final JsonWebTokenService jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        final CompanyLocalManager companyLocalManager =
                mock(CompanyLocalManager.class);
        final UserAPI userAPI     = mock(UserAPI.class);
        final Encryptor encryptor = mock(Encryptor.class);
        final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(jsonWebTokenService, null, null, loginService, null);

        when(loginService.isLoggedIn(request)).thenReturn(false);

        final String jwtId  = "jwt1";
        final String userId = "jsanca";
        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);
        final Date date = dateFormat.parse("04/10/1981");

        final String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                userId, date, date.getTime()
        ));

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(null); // non-logged
        when(request.isSecure()).thenReturn(true); // https
        when(request.getCookies()).thenAnswer(new Answer<Cookie[]>() {

            @Override
            public Cookie[] answer(InvocationOnMock invocation) throws Throwable {

                System.out.println("getting the access token:" + jsonWebToken);
                return new Cookie[] {
                        new Cookie(CookieKeys.JWT_ACCESS_TOKEN, jsonWebToken)
                };
            }
        });

        when(userAPI.loadUserById(userId)).thenAnswer(new Answer<User>() {

            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {

                final User user = new User();

                user.setModificationDate(date);

                return user;
            }
        });

        when(loginService.doCookieLogin(anyObject(), anyObject(), anyObject())).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                System.out.println("Success calling the do cookie for a valid scenario");
                calledDoCookieLogin = true;
                return true;
            }
        });

        jsonWebTokenInterceptor.setCompanyLocalManager(companyLocalManager);
        jsonWebTokenInterceptor.setEncryptor(encryptor);
        jsonWebTokenInterceptor.setLoginService(loginService);
        jsonWebTokenInterceptor.setUserAPI(userAPI);

        jsonWebTokenInterceptor.init();

        jsonWebTokenInterceptor.intercept(request, response);

        jsonWebTokenInterceptor.destroy();

        if (!calledDoCookieLogin) {

            fail("Failed. doCookieLogin must be called on successfully scenario...");
        }
    }

}