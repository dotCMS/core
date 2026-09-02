package com.dotcms.rendering.velocity.util;

import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.LoginMode;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple3;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class VelocityUtilTest {

    private HttpServletRequest request;
    private IHTMLPage page;
    private HttpSession session;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        page = mock(IHTMLPage.class);
        session = mock(HttpSession.class);
    }

    @DataProvider
    public static Object[] dataProviderTestConvertToVelocityVariable() {
        return new Tuple3[] {
                // actual, expected, firstLetterUppercase
                new Tuple3<>("123", "one23", false),
                new Tuple3<>("123", "One23", true),
                new Tuple3<>("_123", "_123", false),
                new Tuple3<>("_123a", "_123a", false),
                new Tuple3<>("_123a", "_123a", true),
                new Tuple3<>("asd123asd", "asd123asd", false),
                new Tuple3<>("asd123asd", "Asd123asd", true),
                new Tuple3<>("#%#$", "____", true),
                new Tuple3<>("#%#$1", "____1", true),
                new Tuple3<>("#%#$abc", "____abc", true),
                new Tuple3<>("#%#$abc", "____abc", false),

        };
    }

    @Test
    @UseDataProvider("dataProviderTestConvertToVelocityVariable")
    public void testConvertToVelocityVariable(final Tuple3<String, String, Boolean> testCase) {
        Assert.assertEquals(testCase._2,
                VelocityUtil.convertToVelocityVariable(testCase._1, testCase._3));
    }

    @Test
    public void testShouldPageCache_PageIsNull_ReturnsFalse() throws Exception {
        Assert.assertFalse(VelocityUtil.shouldPageCache(request, null));
    }

    @Test
    public void testShouldPageCache_TTLIsZero_ReturnsFalse() throws Exception {
        when(page.getCacheTTL()).thenReturn(0L);
        Assert.assertFalse(VelocityUtil.shouldPageCache(request, page));
    }

    @Test
    public void testShouldPageCache_UserBEInLiveMode_ReturnsFalse() throws Exception {
        User user = mock(User.class);
        when(page.getCacheTTL()).thenReturn(10L);

        try (MockedStatic<PortalUtil> portalUtilMock = Mockito.mockStatic(PortalUtil.class);
             MockedStatic<PageMode> pageModeMock = Mockito.mockStatic(PageMode.class);
             MockedStatic<LoginMode> loginModeMock = Mockito.mockStatic(LoginMode.class)) {

            portalUtilMock.when(() -> PortalUtil.getUser(request)).thenReturn(user);
            pageModeMock.when(() -> PageMode.get(request)).thenReturn(PageMode.LIVE);
            loginModeMock.when(() -> LoginMode.get(request)).thenReturn(LoginMode.BE);

            Assert.assertFalse(VelocityUtil.shouldPageCache(request, page));
        }
    }

    @Test
    public void testShouldPageCache_NotGetOrHead_ReturnsFalse() throws Exception {
        when(page.getCacheTTL()).thenReturn(10L);
        when(request.getMethod()).thenReturn("POST");

        try (MockedStatic<PortalUtil> portalUtilMock = Mockito.mockStatic(PortalUtil.class)) {
            portalUtilMock.when(() -> PortalUtil.getUser(request)).thenReturn(null);
            Assert.assertFalse(VelocityUtil.shouldPageCache(request, page));
        }
    }

    @Test
    public void testShouldPageCache_NoCacheParam_ReturnsFalse() throws Exception {
        when(page.getCacheTTL()).thenReturn(10L);
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter(VelocityUtil.DOTCACHE)).thenReturn("no");

        try (MockedStatic<PortalUtil> portalUtilMock = Mockito.mockStatic(PortalUtil.class)) {
            portalUtilMock.when(() -> PortalUtil.getUser(request)).thenReturn(null);
            Assert.assertFalse(VelocityUtil.shouldPageCache(request, page));
        }
    }

    @Test
    public void testShouldPageCache_RefreshParam_ReturnsFalse() throws Exception {
        when(page.getCacheTTL()).thenReturn(10L);
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter(VelocityUtil.DOTCACHE)).thenReturn("refresh");

        try (MockedStatic<PortalUtil> portalUtilMock = Mockito.mockStatic(PortalUtil.class)) {
            portalUtilMock.when(() -> PortalUtil.getUser(request)).thenReturn(null);
            Assert.assertFalse(VelocityUtil.shouldPageCache(request, page));
        }
    }

    @Test
    public void testShouldPageCache_NoCacheAttribute_ReturnsFalse() throws Exception {
        when(page.getCacheTTL()).thenReturn(10L);
        when(request.getMethod()).thenReturn("GET");
        when(request.getAttribute(VelocityUtil.DOTCACHE)).thenReturn("no");

        try (MockedStatic<PortalUtil> portalUtilMock = Mockito.mockStatic(PortalUtil.class)) {
            portalUtilMock.when(() -> PortalUtil.getUser(request)).thenReturn(null);
            Assert.assertFalse(VelocityUtil.shouldPageCache(request, page));
        }
    }

    @Test
    public void testShouldPageCache_NoCacheSessionAttribute_ReturnsFalse() throws Exception {
        when(page.getCacheTTL()).thenReturn(10L);
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(session.getAttribute(VelocityUtil.DOTCACHE)).thenReturn("no");

        try (MockedStatic<PortalUtil> portalUtilMock = Mockito.mockStatic(PortalUtil.class)) {
            portalUtilMock.when(() -> PortalUtil.getUser(request)).thenReturn(null);
            Assert.assertFalse(VelocityUtil.shouldPageCache(request, page));
        }
    }

    @Test
    public void testShouldPageCache_HappyPath_ReturnsTrue() throws Exception {
        when(page.getCacheTTL()).thenReturn(10L);
        when(request.getMethod()).thenReturn("GET");

        try (MockedStatic<PortalUtil> portalUtilMock = Mockito.mockStatic(PortalUtil.class)) {
            portalUtilMock.when(() -> PortalUtil.getUser(request)).thenReturn(null);
            Assert.assertTrue(VelocityUtil.shouldPageCache(request, page));
        }
    }

}
