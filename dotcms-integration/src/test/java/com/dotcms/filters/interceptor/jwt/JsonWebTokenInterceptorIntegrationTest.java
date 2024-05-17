package com.dotcms.filters.interceptor.jwt;

import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.security.Encryptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.ejb.CompanyLocalManager;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.WebKeys;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonWebTokenInterceptorIntegrationTest {

    private static JsonWebTokenService jsonWebTokenService;
    private LoginServiceAPI loginService;
    private static UserAPI userAPI;
    private static final String jwtId  = "jwt1";
    private static String userId;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static Date date;
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final HttpSession session = mock(HttpSession.class);
    private final CompanyLocalManager companyLocalManager = mock(CompanyLocalManager.class);
    private final Encryptor encryptor = mock(Encryptor.class);


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        userAPI = APILocator.getUserAPI();

        //Create User
        final User newUser = new UserDataGen().skinId(UUIDGenerator.generateUuid()).nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadCMSAdminRole(), newUser);
        assertTrue(userAPI.isCMSAdmin(newUser));
        userId = newUser.getUserId();

        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);
        date = dateFormat.parse("04/10/1981");
    }

    /**
     * Test the scenario when the ttl time is not valid for the token
     * @throws IOException
     */
    @Test
    public void interceptWithAccessTokenNonValidTest() throws IOException, DotSecurityException, DotDataException {
        loginService  = mock(LoginServiceAPI.class);

        jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();

        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(jsonWebTokenService, null, null, loginService, userAPI);

        when(loginService.isLoggedIn(request)).thenReturn(false);

        final User user = APILocator.getUserAPI().loadUserById(userId);

        final String jwtId = user.getRememberMeToken();
        final UserToken userToken = new UserToken.Builder().id(jwtId).subject(userId).modificationDate(date).expiresDate(date.getTime()).build();

        userAPI.loadUserById(userId).setModificationDate(new Date());

        final String jsonWebToken = jsonWebTokenService.generateUserToken(userToken);

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
     * Test the scenario when the user from the database has been modified.
     * @throws IOException
     */
    @Test
    public void interceptWithAccessTokenUserModifiedTest() throws IOException, DotSecurityException, DotDataException {
        loginService  = mock(LoginServiceAPI.class);

        jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();

        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(jsonWebTokenService, null, null, loginService, userAPI);

        when(loginService.isLoggedIn(request)).thenReturn(false);

        final User user = APILocator.getUserAPI().loadUserById(userId);
        final String jwtId = user.getRememberMeToken();
        final UserToken userToken = new UserToken.Builder().id(jwtId).subject(userId)
                        .modificationDate(date).expiresDate(date.getTime()).build();

        final String jsonWebToken = jsonWebTokenService.generateUserToken(userToken);

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

        userAPI.loadUserById(userId).setModificationDate(new Date());

        when(loginService.doCookieLogin(any(), any(), any())).thenAnswer(new Answer<Boolean>() {

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
     */
    @Test
    public void interceptWithAccessTokenTest() throws IOException, DotSecurityException, DotDataException {
        loginService  = mock(LoginServiceAPI.class);

        jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();

        final JsonWebTokenInterceptor jsonWebTokenInterceptor =
                new JsonWebTokenInterceptor(jsonWebTokenService, null, null, loginService, null);

        when(loginService.isLoggedIn(request)).thenReturn(false);

        final User user = APILocator.getUserAPI().loadUserById(userId);
        final String jwtId = user.getRememberMeToken();
        final UserToken userToken = new UserToken.Builder().id(jwtId).subject(userId).modificationDate(date)
                .expiresDate(date.getTime()).build();

        final String jsonWebToken = jsonWebTokenService.generateUserToken(userToken);

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

        when(loginService.doCookieLogin(any(), any(), any())).thenAnswer(new Answer<Boolean>() {

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
