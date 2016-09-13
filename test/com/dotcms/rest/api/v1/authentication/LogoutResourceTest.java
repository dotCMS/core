package com.dotcms.rest.api.v1.authentication;

import com.dotcms.cms.login.LoginService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotmarketing.util.Config;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogoutResourceTest {


    public LogoutResourceTest() {

	}

    @Test
    public void testLogout() throws Exception{


        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final WebResource webResource       = null;
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session); //

        Mockito.doNothing().when(loginService).doActionLogout(
                request,
                response);


        final LogoutResource logoutResource =
                new LogoutResource(loginService, webResource);

        final Response response1 = logoutResource.logout(request, response);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertTrue("Logout successfully".equals(ResponseEntityView.class.cast(response1.getEntity()).getEntity()));
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);

    }

}
