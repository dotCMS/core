package com.dotcms.filters.interceptor.saml;

import com.dotcms.IntegrationTestBase;
import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlWebUtilsTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_isByPass_NoByPass_should_be_false() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession        session = mock(HttpSession.class);

        when(request.getRequestURI()).thenReturn("/dotAdmin");
        Assert.assertFalse(samlWebUtils.isByPass(request, session));
    }

    @Test
    public void test_isByPass_should_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession        session = mock(HttpSession.class);

        when(request.getRequestURI()).thenReturn("/dotAdmin");
        when(request.getParameter(SamlWebUtils.BY_PASS_KEY)).thenReturn("true");
        Assert.assertTrue(samlWebUtils.isByPass(request, session));
    }

    @Test
    public void test_isBackEndAdmin_should_be_false() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        Assert.assertFalse(samlWebUtils.isBackEndAdmin(request, "/xxx"));
    }

    @Test
    public void test_isBackEndAdmin_dotAdmin_should_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        Assert.assertTrue(samlWebUtils.isBackEndAdmin(request, "/dotAdmin"));
    }

    @Test
    public void test_isBackEndAdmin_adminMode_should_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final User user                  = mock(User.class);
        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn("ADMIN_MODE");
        when(user.isBackendUser()).thenReturn(true);
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        Assert.assertTrue(samlWebUtils.isBackEndAdmin(request, "/"));
    }

    @Test
    public void test_isFrontEndLoginPage_dotAdmin_should_be_false() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        Assert.assertFalse(samlWebUtils.isFrontEndLoginPage("/dotAdmin"));
    }

    @Test
    public void test_isFrontEndLoginPage_App_Login_should_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        Assert.assertTrue(samlWebUtils.isFrontEndLoginPage("/application/login"));
    }

    @Test
    public void test_isLogoutRequest_Login_should_be_false() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        Assert.assertFalse(samlWebUtils.isLogoutRequest("/application/login",
                "/api/v1/logout","/c/portal/logout","/dotCMS/logout","/dotsaml/request/logout"));
    }

    @Test
    public void test_isLogoutRequest_Login_should_be_true() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils();
        Assert.assertTrue(samlWebUtils.isLogoutRequest("/dotCMS/logout",
                "/api/v1/logout","/c/portal/logout","/dotCMS/logout","/dotsaml/request/logout"));
    }

    @Test
    public void test_getRequest_relay_state() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils(mock(UserWebAPI.class));
        final DotCMSMockRequest request = new DotCMSMockRequest();
        request.setParameterMap(Map.of("companyId", new String [] {"123"}));
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final String relayStateTemplate = "token-companyId=$request.getParameter('companyId')";
        final String relayStateValue = samlWebUtils.getRelayState(request, response, identityProviderConfiguration, relayStateTemplate, Host.SYSTEM_HOST);
        Assert.assertNotNull("The relay state can not be null", relayStateValue);
        Assert.assertTrue("The relay state has to have the uuid token", relayStateValue.contains("token-companyId=123"));
    }

    @Test
    public void test_getRandom_relay_state() {

        final SamlWebUtils samlWebUtils  = new SamlWebUtils(mock(UserWebAPI.class));
        final HttpServletRequest request = new DotCMSMockRequest();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final String relayStateTemplate = "uuid=$math.getRandom()";
        final String relayStateValue = samlWebUtils.getRelayState(request, response, identityProviderConfiguration, relayStateTemplate, Host.SYSTEM_HOST);
        Assert.assertNotNull("The relay state can not be null", relayStateValue);
        Assert.assertTrue("The relay state has to have the uuid token", relayStateValue.contains("uuid="));
    }

    @Test
    public void test_getCustom_relay_state() {

        SamlWebUtils.addRelayStateStrategy(Host.SYSTEM_HOST, (final HttpServletRequest request, final HttpServletResponse response, final IdentityProviderConfiguration identityProviderConfiguration)->"uuid=123");
        final SamlWebUtils samlWebUtils  = new SamlWebUtils(mock(UserWebAPI.class));
        final HttpServletRequest request = new DotCMSMockRequest();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final String relayStateTemplate = "uuid=$math.getRandom()";
        final String relayStateValue = samlWebUtils.getRelayState(request, response, identityProviderConfiguration, relayStateTemplate, Host.SYSTEM_HOST);
        Assert.assertNotNull("The relay state can not be null", relayStateValue);
        Assert.assertTrue("The relay state has to have the uuid token", relayStateValue.contains("uuid=123"));
    }
}
