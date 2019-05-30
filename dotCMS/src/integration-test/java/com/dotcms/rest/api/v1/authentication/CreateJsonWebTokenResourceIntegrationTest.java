package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;
import java.util.Locale;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;

public class CreateJsonWebTokenResourceIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testGetApiToken() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginServiceAPI loginService     = mock(LoginServiceAPI.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final String userId = "dotcms.org.1";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final User user = mock(User.class);
        final String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJpWEtweXU2QmtzcWI0MHZNa3VSUVF3PT0iLCJpYXQiOjE0NzEyODM4MjYsInN1YiI6IntcbiAgXCJ1c2VySWRcIjogXCJpWEtweXU2QmtzcWI0MHZNa3VSUVF3XFx1MDAzZFxcdTAwM2RcIixcbiAgXCJsYXN0TW9kaWZpZWRcIjogMTQ3MDg2NjM1NDAwMCxcbiAgXCJjb21wYW55SWRcIjogXCJkb3RjbXMub3JnXCJcbn0iLCJpc3MiOiJpWEtweXU2QmtzcWI0MHZNa3VSUVF3PT0iLCJleHAiOjE0NzI0OTM0MjZ9.YEtN28ENfpNRnugTFjZoiANlnnura5T5R0Pagi9wiC4";
        final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        LocaleUtil.setUserWebAPI(userWebAPI);
        Config.CONTEXT = context;

        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("CR").build();
        user.setLocale(locale);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getLocale()).thenReturn(locale); //
        when(request.getSession(false)).thenReturn(session); //
        when(request.getSession()).thenReturn(session); //
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(userId);
        when(userLocalManager.getUserById(userId)).thenReturn(user);
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenReturn(true);
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(false);
        when(user.getUserId()).thenReturn(userId);


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, ResponseUtil.INSTANCE, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).label("test").build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);

        System.out.println(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
    }
}
