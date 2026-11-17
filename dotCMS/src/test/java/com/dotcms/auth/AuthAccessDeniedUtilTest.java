package com.dotcms.auth;

import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthAccessDeniedUtil#sendNoAccessPage(HttpServletResponse, User)}, the 403 page shown when an
 * SSO user signs in but lacks the required access (issue #36591).
 *
 * The page must be committed before the caller returns. The SAML callback is a JAX-RS resource method returning
 * {@code void}; if the response is still only buffered when it returns, Jersey answers 204 No Content and the
 * page is lost.
 */
public class AuthAccessDeniedUtilTest {

    private HttpServletResponse response;
    private StringWriter body;

    @Before
    public void setUp() throws Exception {

        response = mock(HttpServletResponse.class);
        body     = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
    }

    /**
     * Given Scenario: a signed-in SSO user without the required access
     * ExpectedResult: a 403 HTML page that browsers and proxies must not cache
     */
    @Test
    public void sendsUncached403Html() throws Exception {

        AuthAccessDeniedUtil.sendNoAccessPage(response, userWithoutEmail());

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("text/html;charset=UTF-8");
        verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setDateHeader("Expires", 0);
    }

    /**
     * Given Scenario: no user is available
     * ExpectedResult: the page is still written, with an empty email
     */
    @Test
    public void nullUser_stillWritesThePage() throws Exception {

        AuthAccessDeniedUtil.sendNoAccessPage(response, null);

        final String html = written();
        assertTrue(html, html.contains("Your account does not have access to dotCMS"));
        assertTrue(html, html.contains("<span class=\"email\"></span>"));
    }

    /**
     * Given Scenario: the page is sent from a JAX-RS resource method that returns void (the SAML callback)
     * ExpectedResult: the response is flushed after the page is written, so it is committed as a 403 and cannot be
     * replaced by an empty 204
     */
    @Test
    public void flushesAfterWritingThePage() throws Exception {

        final AtomicReference<String> bodyAtFlush = new AtomicReference<>();
        doAnswer(invocation -> {
            bodyAtFlush.set(written());
            return null;
        }).when(response).flushBuffer();

        AuthAccessDeniedUtil.sendNoAccessPage(response, userWithoutEmail());

        verify(response).flushBuffer();
        assertNotNull("the response was never flushed", bodyAtFlush.get());
        assertTrue("the page must be written before the flush",
                bodyAtFlush.get().contains("Your account does not have access to dotCMS"));
    }

    private String written() {
        return body.toString();
    }

    /**
     * A user with no email. Escaping a real email goes through {@code UtilHTML}, whose static initializer needs a
     * running dotCMS, so these unit tests use the path that skips it; the status, headers and flush are the same.
     */
    private static User userWithoutEmail() {
        final User user = mock(User.class);
        when(user.getEmailAddress()).thenReturn(null);
        return user;
    }
}
