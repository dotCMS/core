package com.dotcms.rest;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.org.codehaus.cargo.util.Base64;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.liferay.portal.model.User;

public class WebResourceTest {

    private Client client;
    private WebTarget webTarget;
    private HttpServletRequest request;
    private String serverName;
    private Integer serverPort;

    @Before
    public void init() {
        client = RestClientBuilder.newClient();
        request = ServletTestRunner.localRequest.get();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        webTarget = client.target("http://" + serverName + ":" + serverPort + "/api/role");
        RestServiceUtil.addResource(DummyResource.class);
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAuthenticateNoUser() {
        webTarget.path("/loadchildren/").request().get(String.class);
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAuthenticateInvalidUserInURL() {
        webTarget.path("/loadchildren/user/wrong@user.com/password/123456").request().get(String.class);
    }

    @Test
    public void testAuthenticateValidUserInURL() {
        String response = webTarget.path("/loadchildren/user/admin@dotcms.com/password/admin").request().get(String.class);
        assertNotNull(response);
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAuthenticateInvalidUserBasicAuth() {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("wrong@user.com", "123456");
        client.register(feature);
        webTarget = client.target("http://" + serverName + ":" + serverPort + "/api/role");
        webTarget.path("/loadchildren/").request().get(String.class);
    }

    @Test
    public void testAuthenticateValidUserBasicAuth() {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("admin@dotcms.com", "admin");
        client.register(feature);
        webTarget = client.target("http://" + serverName + ":" + serverPort + "/api/role");
        String response = webTarget.path("/loadchildren/").request().get(String.class);
        assertNotNull(response);
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAuthenticateInvalidUserHeaderAuth() {
        webTarget.path("/loadchildren/").request().header("DOTAUTH", Base64.encode("wrong@user.com:123456")).get(String.class);
    }

    @Test
    public void testAuthenticateValidUserHeaderAuth() {
        String response = webTarget.path("/loadchildren/").request().header("DOTAUTH", Base64.encode("admin@dotcms.com:admin")).get(String.class);
        assertNotNull(response);
    }

    @Test(expected = SecurityException.class)
    public void testUserWithoutPermissionOnPortlet() throws DotDataException {
        final String requiredPortlet = "veryCoolPortlet";
        LayoutAPI mockLayoutAPI = mock(LayoutAPI.class);
        User user = APILocator.getUserAPI().getUsersByName("Admin User", 0, 0, APILocator.getUserAPI().getSystemUser(), false).get(0);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("DOTAUTH")).thenReturn(Base64.encode("admin@dotcms.com:admin"));
        when(mockLayoutAPI.doesUserHaveAccessToPortlet(requiredPortlet, user)).thenReturn(false);

        ApiProvider mockProvider = mock(ApiProvider.class);
        when(mockProvider.layoutAPI()).thenReturn(mockLayoutAPI);
        when(mockProvider.userAPI()).thenReturn(APILocator.getUserAPI());

        WebResource webResource = new WebResource(mockProvider);
        webResource.init(null, true, request, true, requiredPortlet);
    }

    @Test
    public void testUserWithPermissionOnPortlet() throws DotDataException {
        final String requiredPortlet = "veryCoolPortlet";
        LayoutAPI mockLayoutAPI = mock(LayoutAPI.class);
        final HttpSession session = mock(HttpSession.class);
        User user = APILocator.getUserAPI().getUsersByName("Admin User", 0, 0, APILocator.getUserAPI().getSystemUser(), false).get(0);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("DOTAUTH")).thenReturn(Base64.encode("admin@dotcms.com:admin"));
        when(mockLayoutAPI.doesUserHaveAccessToPortlet(requiredPortlet, user)).thenReturn(true);
        when(session.getAttribute(WebKeys.COMPANY_ID)).thenReturn("dotcms.org");
        when(session.getId()).thenReturn("1");
        when(request.getSession()).thenReturn(session);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);

        ApiProvider mockProvider = mock(ApiProvider.class);
        when(mockProvider.layoutAPI()).thenReturn(mockLayoutAPI);
        when(mockProvider.userAPI()).thenReturn(APILocator.getUserAPI());

        WebResource webResource = new WebResource(mockProvider);
        InitDataObject data = webResource.init(null, true, request, true, requiredPortlet);
        assertNotNull(data);
    }

}
