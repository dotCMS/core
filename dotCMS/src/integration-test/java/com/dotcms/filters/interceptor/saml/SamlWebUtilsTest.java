package com.dotcms.filters.interceptor.saml;

import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlWebUtilsTest {

    @Test
    public void test_isByPass_NoByPass_shoud_be_false() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession        session = mock(HttpSession.class);

        when(request.getRequestURI()).thenReturn("/dotAdmin");
        Assert.assertFalse(samlWebUtils.isByPass(request, session));
    }

    @Test
    public void test_isByPass_shoud_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession        session = mock(HttpSession.class);

        when(request.getRequestURI()).thenReturn("/dotAdmin");
        when(request.getParameter(SamlWebUtils.BY_PASS_KEY)).thenReturn("true");
        Assert.assertTrue(samlWebUtils.isByPass(request, session));
    }

    @Test
    public void test_isBackEndAdmin_shoud_be_false() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        Assert.assertFalse(samlWebUtils.isBackEndAdmin(request, "/xxx"));
    }

    @Test
    public void test_isBackEndAdmin_dotAdmin_shoud_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        Assert.assertTrue(samlWebUtils.isBackEndAdmin(request, "/dotAdmin"));
    }

    @Test
    public void test_isBackEndAdmin_adminMode_shoud_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final User user                  = mock(User.class);
        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn("ADMIN_MODE");
        when(user.isBackendUser()).thenReturn(true);
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        Assert.assertTrue(samlWebUtils.isBackEndAdmin(request, "/"));
    }

    @Test
    public void test_isFrontEndLoginPage_dotAdmin_shoud_be_false() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        Assert.assertFalse(samlWebUtils.isFrontEndLoginPage("/dotAdmin"));
    }

    @Test
    public void test_isFrontEndLoginPage_App_Login_shoud_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        Assert.assertTrue(samlWebUtils.isFrontEndLoginPage("/application/login"));
    }

    @Test
    public void test_isLogoutRequest_Login_shoud_be_false() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        Assert.assertFalse(samlWebUtils.isLogoutRequest("/application/login",
                "/api/v1/logout","/c/portal/logout","/dotCMS/logout","/dotsaml/request/logout"));
    }

    @Test
    public void test_isLogoutRequest_Login_shoud_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        Assert.assertTrue(samlWebUtils.isLogoutRequest("/dotCMS/logout",
                "/api/v1/logout","/c/portal/logout","/dotCMS/logout","/dotsaml/request/logout"));
    }
}
