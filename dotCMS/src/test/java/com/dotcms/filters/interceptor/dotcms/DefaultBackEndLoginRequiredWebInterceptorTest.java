package com.dotcms.filters.interceptor.dotcms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.SimpleWebInterceptorDelegateImpl;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.util.SecurityLogger;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * The goal of this unit test is to try some scenarios for the default backend login required
 * interceptor.
 *
 * @author Jonathan Gamba 9/25/18
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultBackEndLoginRequiredWebInterceptorTest extends UnitTestBase {

    /**
     * Test the scenario when the user is already logged in to the back end, that means the filter
     * chain must continue.
     */
    @Test
    public void intercept_isLoggedToBackend_true() throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);


        final DefaultBackEndLoginRequiredWebInterceptor loginRequiredWebInterceptor =
                new DefaultBackEndLoginRequiredWebInterceptor(userWebAPI);

        when(userWebAPI.isLoggedToBackend(request)).thenReturn(true);

        loginRequiredWebInterceptor.init();

        Result result = loginRequiredWebInterceptor.intercept(request, response);
        assertEquals(result, Result.NEXT);

        loginRequiredWebInterceptor.destroy();
    }

    /**
     * Test the scenario when the user is NOT logged in to the back end, that means the filter chain
     * must stop and redirect the user to the login page.
     */
    @Test
    public void intercept_isLoggedToBackend_false() throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final PrintWriter printWriter = mock(PrintWriter.class);
        final HttpSession session = mock(HttpSession.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);


        final DefaultBackEndLoginRequiredWebInterceptor loginRequiredWebInterceptor =
                new DefaultBackEndLoginRequiredWebInterceptor(userWebAPI);

        when(request.getSession(false)).thenReturn(session);
        when(response.getWriter()).thenReturn(printWriter);
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(false);

        loginRequiredWebInterceptor.init();

        Result result = loginRequiredWebInterceptor.intercept(request, response);
        assertEquals(result, Result.SKIP_NO_CHAIN);

        loginRequiredWebInterceptor.destroy();
    }

    /**
     * Test the scenario when an URI that should not be intercepted is called.
     */
    @Test
    public void test_not_match() throws Exception {

        final String URI_TO_TEST = "/blog/my-blog-page";

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final PrintWriter printWriter = mock(PrintWriter.class);
        final HttpSession session = mock(HttpSession.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final DefaultBackEndLoginRequiredWebInterceptor loginRequiredWebInterceptor = mock(DefaultBackEndLoginRequiredWebInterceptor.class);

        try (MockedStatic<SecurityLogger> mocked = Mockito.mockStatic(SecurityLogger.class)) {
            mocked.when(() -> SecurityLogger.logInfo(any(), anyString())).thenAnswer(invocation -> null);

            Mockito.verify(loginRequiredWebInterceptor, never()).intercept(request, response);


            //Create a new instance of an interceptor delegate
            SimpleWebInterceptorDelegateImpl webInterceptorDelegate = new SimpleWebInterceptorDelegateImpl();
            webInterceptorDelegate.add(loginRequiredWebInterceptor);
            webInterceptorDelegate.init();
            assertDoesNotThrow(() -> webInterceptorDelegate.intercept(request, response));
            assertDoesNotThrow(webInterceptorDelegate::destroy);
        }
    }

    /**
     * Test anyMatchFilter method in order to validate is applying correctly the interceptor filter
     */
    @Test
    public void test_anyMatchFilter_invalid() throws Exception {

        final String[] URIS_TO_TEST = {
                "/blog/my-blog-page",
                "/anyFolder/Html/something",
                "/anyFolder/html/something",
                "/anyhtmlFolder/something",
                "html/something",
                "HTML/something",
                "/htmlFolder/something",
                "/anyFolder/HTML/something",
                "/anyFolder/otherFolder/html/something"
        };

        final UserWebAPI userWebAPI = mock(UserWebAPI.class);

        final DefaultBackEndLoginRequiredWebInterceptor loginRequiredWebInterceptor =
                new DefaultBackEndLoginRequiredWebInterceptor(userWebAPI);

        //Create a new instance of an interceptor delegate
        SimpleWebInterceptorDelegateImpl webInterceptorDelegate = new SimpleWebInterceptorDelegateImpl();
        webInterceptorDelegate.add(loginRequiredWebInterceptor);
        webInterceptorDelegate.init();

        for (final String uriToTest : URIS_TO_TEST) {
            assertFalse(
                    webInterceptorDelegate.anyMatchFilter(loginRequiredWebInterceptor, uriToTest));
        }

        webInterceptorDelegate.destroy();
    }

    /**
     * Test anyMatchFilter method in order to validate is applying correctly the interceptor filter
     */
    @Test
    public void test_anyMatchFilter_valid() throws Exception {

        final String[] URIS_TO_TEST = {
                "/html/portlet/EXT/contentlet/image_tools/index.jsp",
                "/Html/portlet/ext/contentlet/image_tools/index.jsp",
                "/HTML/portlet/ext/contentlet/image_tools/index.jsp",
                "/htmL/portlet/ext/contentlet/image_tools/index.jsp",
                "/html/portlet/ext/contentlet/image_tools/index.jsp",
                "/Html/portlet/EXT/contentlet/image_tools/index.jsp",
                "/html/portlet/EXT/contentlet/image_tools/index.jsp",
                "/html/portlet/EXT/contentLET/image_tools/index.jsp",
                "/html/portlet/ext/contentlet/image_tools/index.jsp?fieldName=1%22%20%6f%6e%65%72%72%6f%72%3d%61%6c%65%72%74%28%27%31%27%29%20%3e&inode="
        };

        final UserWebAPI userWebAPI = mock(UserWebAPI.class);

        final DefaultBackEndLoginRequiredWebInterceptor loginRequiredWebInterceptor =
                new DefaultBackEndLoginRequiredWebInterceptor(userWebAPI);

        //Create a new instance of an interceptor delegate
        SimpleWebInterceptorDelegateImpl webInterceptorDelegate = new SimpleWebInterceptorDelegateImpl();
        webInterceptorDelegate.add(loginRequiredWebInterceptor);
        webInterceptorDelegate.init();

        for (final String uriToTest : URIS_TO_TEST) {
            assertTrue(
                    webInterceptorDelegate.anyMatchFilter(loginRequiredWebInterceptor, uriToTest));
        }

        webInterceptorDelegate.destroy();
    }

    /**
     * Test the allowed urls we have in the DefaultBackEndLoginRequiredWebInterceptor
     */
    @Test
    public void intercept_allowed_url_no_user() throws Exception {

        final String[] URIS_TO_TEST = {
                "/html/js/dojo",
                "/html/js/dojo/test/test",
                "/html/js/dojo/test",
                "/html/js/dojo/test/",
                "/html/js/dojo/test/test/file.png",
                "/html/images/backgrounds,/html/images/persona",
                "/html/images/backgrounds,/html/images/persona/test",
                "/html/images/backgrounds,/html/images/persona/test/",
                "/html/images/backgrounds,/html/images/persona/test/test",
                "/html/images/backgrounds,/html/images/persona/test/file.png"
        };

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);


        final DefaultBackEndLoginRequiredWebInterceptor loginRequiredWebInterceptor =
                new DefaultBackEndLoginRequiredWebInterceptor(userWebAPI);

        loginRequiredWebInterceptor.init();

        for (final String uriToTest : URIS_TO_TEST) {
            when(request.getRequestURI()).thenReturn(uriToTest);
            Result result = loginRequiredWebInterceptor.intercept(request, response);
            assertEquals(result, Result.NEXT);
        }

        loginRequiredWebInterceptor.destroy();
    }

    /**
     * Test some restricted urls
     */
    @Test
    public void intercept_restricted_url_no_user() throws Exception {

        final String[] URIS_TO_TEST = {
                "/html/portlet/EXT/contentlet/image_tools/index.jsp",
                "/Html/portlet/ext/contentlet/image_tools/index.jsp",
                "/HTML/portlet/ext/contentlet/image_tools/index.jsp",
                "/htmL/portlet/ext/contentlet/image_tools/index.jsp",
                "/html/portlet/ext/contentlet/image_tools/index.jsp",
                "/Html/portlet/EXT/contentlet/image_tools/index.jsp",
                "/html/portlet/EXT/contentlet/image_tools/index.jsp",
                "/html/portlet/EXT/contentLET/image_tools/index.jsp",
                "/html/portlet/ext/contentlet/image_tools/index.jsp?fieldName=1%22%20%6f%6e%65%72%72%6f%72%3d%61%6c%65%72%74%28%27%31%27%29%20%3e&inode="
        };

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final PrintWriter printWriter = mock(PrintWriter.class);
        final HttpSession session = mock(HttpSession.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);


        final DefaultBackEndLoginRequiredWebInterceptor loginRequiredWebInterceptor =
                new DefaultBackEndLoginRequiredWebInterceptor(userWebAPI);

        when(request.getSession(false)).thenReturn(session);
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(false);
        when(response.getWriter()).thenReturn(printWriter);

        loginRequiredWebInterceptor.init();

        for (final String uriToTest : URIS_TO_TEST) {
            when(request.getRequestURI()).thenReturn(uriToTest);
            Result result = loginRequiredWebInterceptor.intercept(request, response);
            assertEquals(result, Result.SKIP_NO_CHAIN);
        }

        loginRequiredWebInterceptor.destroy();
    }

}