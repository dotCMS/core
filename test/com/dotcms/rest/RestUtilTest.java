package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.api.v1.authentication.ForgotPasswordForm;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by freddyrodriguez on 7/29/16.
 */
public abstract class RestUtilTest {

    public static HttpServletRequest getMockHttpRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault());
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        return request;
    }

    public static void verifyErrorResponse(Response response, int statusExpected, String errorCodeExpected) {
        assertNotNull(response);
        assertEquals(response.getStatus(), statusExpected);
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response.getEntity()).getErrors().get(0).getErrorCode().equals
                (errorCodeExpected));

    }

    public static void verifySuccessResponse(Response response) {
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof ResponseEntityView);
    }

    public static ServletContext getMockContext(){

        final ServletContext context = mock(ServletContext.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {
                return Company.AUTH_TYPE_ID;
            }
        };

        Config.CONTEXT=context;
        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);

        return context;
    }
}
