//package com.dotcms.rest.config;
//
//import com.dotcms.repackage.com.google.common.base.Optional;
//import com.dotcms.repackage.com.sun.jersey.spi.container.ContainerRequest;
//import com.dotmarketing.business.ApiProvider;
//import com.dotmarketing.business.web.UserWebAPI;
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//import javax.servlet.http.HttpServletRequest;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import static com.dotcms.repackage.org.hamcrest.CoreMatchers.is;
//import static com.dotcms.repackage.org.junit.Assert.assertThat;
//import static com.dotcms.repackage.org.mockito.Mockito.mock;
//import static com.dotcms.repackage.org.mockito.Mockito.when;
//
//public class AuthenticationProviderTest {
//
//    private ApiProvider apiProvider;
//    private HttpServletRequest request;
//
//    @Before
//    public void setUp() throws Exception {
//        request = mockTheRequest();
//        apiProvider = mockTheApi();
//    }
//
//    private static HttpServletRequest mockTheRequest() {
//        HttpServletRequest mock = mock(HttpServletRequest.class);
//        return mock;
//    }
//
//    private static ApiProvider mockTheApi() {
//        ApiProvider mock = mock(ApiProvider.class);
//        UserWebAPI webAPI = mock(UserWebAPI.class);
//        when(mock.userWebAPI()).thenReturn(webAPI);
//        return mock;
//    }
//
//    @Test
//    public void testAuthenticate() throws Exception {
//
//    }
//
//    @Test
//    public void testGetAuthCredentialsFromBasicAuthHappyPath() throws Exception {
//        AuthenticationProvider auth = new AuthenticationProvider(apiProvider);
//        String username = "admin@dotcms.com";
//        String password = "admin";
//
//        String credentials = toBasicAuthString(username, password);
//        when(request.getHeader(ContainerRequest.AUTHORIZATION)).thenReturn(credentials);
//
//        Optional<AuthenticationProvider.UsernamePassword> token = auth.getAuthCredentialsFromBasicAuth(request);
//        assertThat(token.isPresent(), is(true));
//        assertThat(token.get().username, is(username));
//        assertThat(token.get().password, is(password));
//    }
//
//    private static String toBasicAuthString(String username, String password) {
//        String s = username + ":" + password;
//        return "Basic " + Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.US_ASCII));
//    }
//
//    @Ignore("Oh noes! Passwords with colons are invalid.")
//    @Test
//    public void testBasicAuthAllowsColon() throws Exception {
//        AuthenticationProvider auth = new AuthenticationProvider(apiProvider);
//        when(request.getHeader(ContainerRequest.AUTHORIZATION)).thenReturn("Basic YWRtaW5AZG90Y21zLmNvbTphZG1pbjpqaGc=");
//
//        Optional<AuthenticationProvider.UsernamePassword> token = auth.getAuthCredentialsFromBasicAuth(request);
//        assertThat(token.isPresent(), is(true));
//        assertThat(token.get().username, is("admin@dotcms.com"));
//        assertThat(token.get().password, is("admin:jhg"));
//    }
//
//    @Test
//    public void testGetAuthCredentialsFromBasicAuthThrowsOnBadToken() throws Exception {
//        AuthenticationProvider auth = new AuthenticationProvider(apiProvider);
//        when(request.getHeader(ContainerRequest.AUTHORIZATION)).thenReturn("Basic YWRtaW5AZG90Y21zLmNvbTphZG1pbg==");
//
//        Optional<AuthenticationProvider.UsernamePassword> token = auth.getAuthCredentialsFromBasicAuth(request);
//        assertThat(token.isPresent(), is(true));
//        assertThat(token.get().username, is("admin@dotcms.com"));
//        assertThat(token.get().password, is("admin"));
//    }
//
//    @Test
//    public void testGetAuthCredentialsFromHeaderAuth() throws Exception {
//
//    }
//
//    @Test
//    public void testAuthenticateUser() throws Exception {
//
//    }
//}