package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.logConsole.model.LogMapper;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by freddyrodriguez on 7/29/16.
 */
public abstract class RestUtilTest {

    public static final String DEFAULT_USER_ID = "userId";
    public static final String DEFAULT_COMPANY = "dotcms.org";

    public static HttpServletRequest getMockHttpRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        final HttpSession session = mock(HttpSession.class);
        when(request.getSession( anyBoolean() )).thenReturn(session);
        when(request.getSession(  )).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault());
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(DEFAULT_USER_ID);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        return request;
    }

    public static void verifyErrorResponse(final Response response, final int statusExpected, final String errorCodeExpected) {
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

    public static void verifySuccessResponse(final Response response) {
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof ResponseEntityView);
    }

    public static void initMockContext(){

        final ServletContext context = mock(ServletContext.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {
                return Company.AUTH_TYPE_ID;
            }
        };

        Config.CONTEXT=context;
        when(context.getInitParameter("company_id")).thenReturn(DEFAULT_COMPANY);

        LogMapper mockLogMapper = mock(LogMapper.class);
        when ( mockLogMapper.isLogEnabled( any() ) ).thenReturn( false );

        LogMapper.setLogMapper( mockLogMapper );
    }

    public static WebResource getMockWebResource(final User user, final HttpServletRequest req) {
        WebResource webResource  = mock(WebResource.class);

        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, req, true, null)).thenReturn(initDataObject);

        return webResource;
    }
}
