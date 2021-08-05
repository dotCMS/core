package org.apache.velocity.tools.view.tools;

import com.dotmarketing.filters.CookieServletResponse;
import org.apache.commons.lang.mutable.MutableObject;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieToolTest {

    /**
     * Method to test: {@link CookieTool#add(String, String)}
     * Given Scenario: Adding a cookie should have the path to /
     * ExpectedResult: The cookie path should be /
     *
     *
     */
    @Test()
    public void test_add_cookie_success () {

        final CookieTool cookieTool = new CookieTool();
        final ViewContext context   = Mockito.mock(ViewContext.class);
        final MutableObject cookieHolder = new MutableObject();
        final HttpServletResponse response = new CookieServletResponse(Mockito.mock(HttpServletResponse.class), true) {

            @Override
            public void addCookie(final Cookie cookie) {
                cookieHolder.setValue(cookie);
            }
        };
        final HttpServletRequest  request  = Mockito.mock(HttpServletRequest.class);

        Mockito.when(context.getResponse()).thenReturn(response);
        Mockito.when(context.getRequest()).thenReturn(request);
        cookieTool.init(context);

        cookieTool.add("name", "value");
        Assert.assertNotNull(cookieHolder.getValue());
        final Cookie cookie = (Cookie) cookieHolder.getValue();

        Assert.assertEquals("name", cookie.getName());
        Assert.assertEquals("value", cookie.getValue());
        Assert.assertEquals("/", cookie.getPath());
    }

    /**
     * Method to test: {@link CookieTool#add(String, String, int)}
     * Given Scenario: Adding a cookie should have the path to / and max age 1000
     * ExpectedResult: The cookie path should be / and max age 1000
     *
     *
     */
    @Test()
    public void test_add_cookie_max_timesuccess () {

        final CookieTool cookieTool = new CookieTool();
        final ViewContext context   = Mockito.mock(ViewContext.class);
        final MutableObject cookieHolder = new MutableObject();
        final HttpServletResponse response = new CookieServletResponse(Mockito.mock(HttpServletResponse.class), true) {

            @Override
            public void addCookie(final Cookie cookie) {
                cookieHolder.setValue(cookie);
            }
        };
        final HttpServletRequest  request  = Mockito.mock(HttpServletRequest.class);

        Mockito.when(context.getResponse()).thenReturn(response);
        Mockito.when(context.getRequest()).thenReturn(request);
        cookieTool.init(context);

        cookieTool.add("name", "value", 1000);
        Assert.assertNotNull(cookieHolder.getValue());
        final Cookie cookie = (Cookie) cookieHolder.getValue();

        Assert.assertEquals("name", cookie.getName());
        Assert.assertEquals("value", cookie.getValue());
        Assert.assertEquals("/", cookie.getPath());
        Assert.assertEquals(1000, cookie.getMaxAge());
    }
}
