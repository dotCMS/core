package com.dotmarketing.business.ajax;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.repackage.org.directwebremoting.Container;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.lang.reflect.Field;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.rules.ExternalResource;

/**
 * JUnit 4 class rule for integration tests that call DWR classes directly: {@link #loginAs(User)}
 * makes {@code WebContextFactory.get()} return a request whose logged-in user is the given one,
 * and when the test class ends the builder that was attached before it ran is put back, so later
 * tests in the same JVM never see this class's mocked request.
 *
 * <pre>
 * &#64;ClassRule
 * public static final DwrWebContextRule dwrContext = new DwrWebContextRule();
 * ...
 * dwrContext.loginAs(user);
 * </pre>
 */
public class DwrWebContextRule extends ExternalResource {

    private WebContextFactory.WebContextBuilder previousWebContextBuilder;

    @Override
    protected void before() throws Throwable {
        previousWebContextBuilder = currentWebContextBuilder();
    }

    @Override
    protected void after() {
        attachWebContextBuilder(previousWebContextBuilder);
    }

    /**
     * Makes the DWR request of the following calls belong to the given user ({@code null} for a
     * request with nobody logged in).
     *
     * @param user the user the DWR classes resolve as logged in
     * @return the mocked request, for tests that need extra attributes on it
     */
    public HttpServletRequest loginAs(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);

        final WebContextFactory.WebContextBuilder webContextBuilder =
                mock(WebContextFactory.WebContextBuilder.class);
        when(webContextBuilder.get()).thenReturn(webContext);

        attachWebContextBuilder(webContextBuilder);
        return request;
    }

    /**
     * Reads the builder {@code WebContextFactory} currently uses; the field is private and has no
     * getter.
     */
    private static WebContextFactory.WebContextBuilder currentWebContextBuilder() throws Exception {
        final Field builder = WebContextFactory.class.getDeclaredField("builder");
        builder.setAccessible(true);
        return (WebContextFactory.WebContextBuilder) builder.get(null);
    }

    /**
     * Makes {@code WebContextFactory} use the given builder ({@code null} detaches it).
     */
    private static void attachWebContextBuilder(final WebContextFactory.WebContextBuilder webContextBuilder) {
        final Container dwrContainer = mock(Container.class);
        when(dwrContainer.getBean(WebContextFactory.WebContextBuilder.class)).thenReturn(webContextBuilder);
        WebContextFactory.attach(dwrContainer);
    }
}
