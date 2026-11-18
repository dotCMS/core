package com.dotcms.util;

import java.net.URL;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.mockito.Mockito;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestUnitTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityUtilsTest {

  final static String PORTAL_HOST = "portalhost.com";
  
  final static String IGNORE_THIS_URI = "/ignorethisuri";
  
  final static String IGNORE_UNDER_THIS_URI = "/ignoreunderthisuri/*";
  
  /**
   * An array of good {URI,REFERER}
   */
  final String[][] GOOD_REFERERS = new String[][] {
      {"http://localhost:8080/abc123","http://localhost:8080/abc123"}, 
      {"http://127.0.0.1/abc123/test.css", null},   // allow CSS files
      {"http://demo.dotcms.com/dotAdmin/polyfills.js", "http://demo.dotcms.com/"},
      {"http://gmail.com/dotAdmin/polyfills.js?badstuff=true", "https://gmail.com/testing"},
      {"http://gmail.com/dotAdmin/polyfills.js?badstuff=true", "https://"+PORTAL_HOST+"/testing"}, // allow the host found in Company().getPortalURL()
      {"http://demo.dotcms.com/ignorethisuri", null},
      {"http://demo.dotcms.com/ignoreunderthisuri/testing", null},
      {"http://demo.dotcms.com/ignoreunderthisuri/", null},
      {"http://demo.dotcms.com/ignoreunderthisuri/235326egr4t/asdasd/asdasd", null},
  };


  /**
   * An array of bad {URI,REFERER}
   */
  final String[][] BAD_REFERERS = new String[][] {
      {"http://localhost:8080/abc123",null}, 
      {"http://demo.dotcms.com/dotAdmin/polyfills.js", "http://www.dotcms.com/"},
      {"http://gmail.com/dotAdmin/polyfills.js?badstuff=true", "https://demo.dotcms.com/testing"},
      {"http://gmail.com/dotAdmin/polyfills.js?badstuff=true", null}, // allow the host found in Company().getPortalURL()
      {"http://demo.dotcms.com/ignorethisurixyz", "google"},
      {"http://123.dotcms.com/ignoreunderthisuri", "google.com"},
      {"http://345.dotcms.com/ignoreunderthisur/", "badSite"},
      {"http://675.dotcms.com/./ignoreunderthisuri/235326egr4t/asdasd/asdasd", "localhost"},
  };

  
  
  @Test
  public void test_valid_referers() throws Exception {

    SecurityUtils utils = new SecurityUtils();
    SecurityUtils spyUtils = Mockito.spy(utils);
    Mockito.doReturn(ImmutableList.of(PORTAL_HOST)).when(spyUtils).loadIgnoreHosts();
    Mockito.doReturn(ImmutableList.of(IGNORE_THIS_URI,IGNORE_UNDER_THIS_URI)).when(spyUtils).loadIgnorePaths();
    Mockito.doReturn(PORTAL_HOST).when(spyUtils).getPortalHost();
    for (String[] urlReferer : GOOD_REFERERS) {
      URL url = new URL(urlReferer[0]);
      HttpServletRequest request = mockRequest(url.getHost(), url.getPath() , urlReferer[1]);
      System.out.println("testing good url" + urlReferer[0] + " with referer: " + urlReferer[1]);
      assert(spyUtils.validateReferer(request));
      
    }
    
  }

  @Test
  public void test_invalid_referers() throws Exception {

    SecurityUtils utils = new SecurityUtils();
    SecurityUtils spyUtils = Mockito.spy(utils);
    Mockito.doReturn(ImmutableList.of(PORTAL_HOST)).when(spyUtils).loadIgnoreHosts();
    Mockito.doReturn(ImmutableList.of(IGNORE_THIS_URI,IGNORE_UNDER_THIS_URI)).when(spyUtils).loadIgnorePaths();
    Mockito.doReturn(PORTAL_HOST).when(spyUtils).getPortalHost();
    Mockito.doReturn(false).when(spyUtils).resolveHost(Mockito.anyString());
    
    
    
    for (String[] urlReferer : BAD_REFERERS) {
      URL url = new URL(urlReferer[0]);
      HttpServletRequest request = mockRequest(url.getHost(), url.getPath() , urlReferer[1]);
      System.out.println("testing bad  url" + urlReferer[0] + " with referer: " + urlReferer[1]);
      assert(!spyUtils.validateReferer(request));
    }
    
  }
  
  
  
  
  
  
  private HttpServletRequest mockRequest(String host, String uri, String referer) {


    return new MockHeaderRequest(new MockAttributeRequest(new MockSessionRequest(new MockHttpRequestUnitTest(host, uri).request()).request()).request(), "referer", referer);
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: An authenticated (non-anonymous) user lacks permission on a resource.
   * Expected Result: A clean 403 is sent, the REDIRECT_AFTER_LOGIN intent is cleared, and it is
   * never set (so the container error page does not bounce the user back through the SSO flow).
   */
  @Test
  public void test_sendPermissionDenied_authenticatedUser_returns403_andClearsRedirect() throws Exception {

    final User authenticated = mock(User.class);
    when(authenticated.isAnonymousUser()).thenReturn(false);

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    when(request.getSession(false)).thenReturn(session);

    SecurityUtils.sendPermissionDenied(authenticated, "/protected/file.pdf", request, response);

    verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    verify(session).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
    verify(session, never()).setAttribute(Mockito.eq(WebKeys.REDIRECT_AFTER_LOGIN), Mockito.any());
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: The anonymous user requests a protected resource.
   * Expected Result: A 401 is sent and REDIRECT_AFTER_LOGIN is set so login can return the user.
   */
  @Test
  public void test_sendPermissionDenied_anonymousUser_returns401_andSetsRedirect() throws Exception {

    final User anonymous = mock(User.class);
    when(anonymous.isAnonymousUser()).thenReturn(true);

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    when(request.getSession()).thenReturn(session);

    SecurityUtils.sendPermissionDenied(anonymous, "/protected/file.pdf", request, response);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    verify(session).setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, "/protected/file.pdf");
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: A null user (not resolved / not logged in) requests a protected resource.
   * Expected Result: Treated as anonymous -> 401 with the redirect intent set.
   */
  @Test
  public void test_sendPermissionDenied_nullUser_returns401() throws Exception {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    when(request.getSession()).thenReturn(session);

    SecurityUtils.sendPermissionDenied(null, "/protected/file.pdf", request, response);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    verify(session).setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, "/protected/file.pdf");
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: A signed-out visitor requests a protected page with a query string; the 401 is raised
   * inside the forward to the rendering servlet, where the original query string is a forward attribute
   * (issue #36591).
   * Expected Result: The query string is saved under its own session key, next to the path in
   * REDIRECT_AFTER_LOGIN (which stays the path only), so a SAML login can return the visitor to the full URL.
   */
  @Test
  public void test_sendPermissionDenied_anonymous_savesForwardQueryString() throws Exception {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);
    when(request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)).thenReturn("id=42");

    SecurityUtils.sendPermissionDenied(anonymousUser(), "/members/report", request, mock401Response());

    verify(session).setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, "/members/report");
    verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING, "id=42");
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: No forward attribute, but the request itself carries a query string.
   * Expected Result: The request's query string is saved.
   */
  @Test
  public void test_sendPermissionDenied_anonymous_savesRequestQueryString() throws Exception {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);
    when(request.getQueryString()).thenReturn("id=7");

    SecurityUtils.sendPermissionDenied(anonymousUser(), "/members/report", request, mock401Response());

    verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING, "id=7");
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: A signed-out visitor requests a protected page without a query string.
   * Expected Result: Any query string saved for an earlier page is removed, so it cannot be reattached later.
   */
  @Test
  public void test_sendPermissionDenied_anonymous_withoutQuery_removesSavedQueryString() throws Exception {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);

    SecurityUtils.sendPermissionDenied(anonymousUser(), "/members/report", request, mock401Response());

    verify(session).removeAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING);
    verify(session, never()).setAttribute(Mockito.eq(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING), Mockito.any());
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: A signed-in user lacks permission (403).
   * Expected Result: The saved query string is cleared together with REDIRECT_AFTER_LOGIN.
   */
  @Test
  public void test_sendPermissionDenied_authenticated_clearsSavedQueryString() throws Exception {

    final User authenticated = mock(User.class);
    when(authenticated.isAnonymousUser()).thenReturn(false);
    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(session);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);

    SecurityUtils.sendPermissionDenied(authenticated, "/members/report", request, response);

    verify(session).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
    verify(session).removeAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING);
  }

  /**
   * Method to test: {@link SecurityUtils#rememberLoginQueryString(HttpServletRequest)}
   * Given Scenario: The request was forwarded to a rendering servlet with the original query string.
   * Expected Result: The query string is saved under REDIRECT_AFTER_LOGIN_QUERY_STRING.
   */
  @Test
  public void test_rememberLoginQueryString_savesForwardQueryString() {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);
    when(request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)).thenReturn("id=42");

    SecurityUtils.rememberLoginQueryString(request);

    verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING, "id=42");
  }

  /**
   * Method to test: {@link SecurityUtils#rememberLoginQueryString(HttpServletRequest)}
   * Given Scenario: No forward attribute; the request carries its own query string.
   * Expected Result: The request's query string is saved.
   */
  @Test
  public void test_rememberLoginQueryString_savesRequestQueryString() {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);
    when(request.getQueryString()).thenReturn("id=7");

    SecurityUtils.rememberLoginQueryString(request);

    verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING, "id=7");
  }

  /**
   * Method to test: {@link SecurityUtils#rememberLoginQueryString(HttpServletRequest)}
   * Given Scenario: No query string at all.
   * Expected Result: Any previously saved query string is removed.
   */
  @Test
  public void test_rememberLoginQueryString_withoutQuery_removesKey() {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);

    SecurityUtils.rememberLoginQueryString(request);

    verify(session).removeAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING);
    verify(session, never()).setAttribute(Mockito.eq(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING), Mockito.any());
  }

  /**
   * Method to test: {@link SecurityUtils#rememberLoginQueryString(HttpServletRequest)}
   * Given Scenario: A forwarded page request /members/report?id=42.
   * Expected Result: The original path is saved next to the query string, so an SSO login can match it against
   * the referrer even when the error page's REDIRECT_AFTER_LOGIN does not persist (Redis sessions).
   */
  @Test
  public void test_rememberLoginQueryString_savesOriginalPathWithQuery() {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);
    when(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)).thenReturn("/members/report");
    when(request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)).thenReturn("id=42");

    SecurityUtils.rememberLoginQueryString(request);

    verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING, "id=42");
    verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_PATH, "/members/report");
  }

  /**
   * Method to test: {@link SecurityUtils#rememberLoginQueryString(HttpServletRequest)}
   * Given Scenario: No forward attributes; the request itself is /members/report?id=7.
   * Expected Result: The request's own path is saved.
   */
  @Test
  public void test_rememberLoginQueryString_savesRequestPath() {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);
    when(request.getRequestURI()).thenReturn("/members/report");
    when(request.getQueryString()).thenReturn("id=7");

    SecurityUtils.rememberLoginQueryString(request);

    verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_PATH, "/members/report");
  }

  /**
   * Method to test: {@link SecurityUtils#rememberLoginQueryString(HttpServletRequest)}
   * Given Scenario: No query string.
   * Expected Result: The saved path is removed together with the query string.
   */
  @Test
  public void test_rememberLoginQueryString_withoutQuery_removesPath() {

    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = anonymousRequest(session);
    when(request.getRequestURI()).thenReturn("/members/report");

    SecurityUtils.rememberLoginQueryString(request);

    verify(session).removeAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_PATH);
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: A signed-in user lacks permission (403).
   * Expected Result: The saved path is cleared too.
   */
  @Test
  public void test_sendPermissionDenied_authenticated_clearsSavedPath() throws Exception {

    final User authenticated = mock(User.class);
    when(authenticated.isAnonymousUser()).thenReturn(false);
    final HttpSession session = mock(HttpSession.class);
    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(session);

    SecurityUtils.sendPermissionDenied(authenticated, "/members/report", request, mock401Response());

    verify(session).removeAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_PATH);
  }

  private static User anonymousUser() {
    final User anonymous = mock(User.class);
    when(anonymous.isAnonymousUser()).thenReturn(true);
    return anonymous;
  }

  private static HttpServletRequest anonymousRequest(final HttpSession session) {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(session);
    return request;
  }

  private static HttpServletResponse mock401Response() {
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    return response;
  }

  /**
   * Method to test: {@link SecurityUtils#sendPermissionDenied(User, String, HttpServletRequest, HttpServletResponse)}
   * Given Scenario: The response has already been committed.
   * Expected Result: No error is written (avoids IllegalStateException / double response).
   */
  @Test
  public void test_sendPermissionDenied_committedResponse_isNoOp() throws Exception {

    final User authenticated = mock(User.class);
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(true);

    SecurityUtils.sendPermissionDenied(authenticated, "/protected/file.pdf", request, response);

    verify(response, never()).sendError(Mockito.anyInt());
  }

  /**
   * Test validateIdentifier with valid UUIDs
   */
  @Test
  public void test_validateIdentifier_validUUIDs() {
    // Valid UUIDs should not throw exceptions
    SecurityUtils.validateIdentifier("550e8400-e29b-41d4-a716-446655440000");
    SecurityUtils.validateIdentifier("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    SecurityUtils.validateIdentifier("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
  }

  /**
   * Test validateIdentifier with system identifiers
   */
  @Test
  public void test_validateIdentifier_systemIdentifiers() {
    // System identifiers should not throw exceptions
    SecurityUtils.validateIdentifier("SYSTEM_HOST");
    SecurityUtils.validateIdentifier("SYSTEM_FOLDER");
  }

  /**
   * Test validateIdentifier with valid variable names
   */
  @Test
  public void test_validateIdentifier_validVariableNames() {
    // Valid variable names (alphanumeric starting with letter or underscore)
    SecurityUtils.validateIdentifier("myVariable");
    SecurityUtils.validateIdentifier("_privateVar");
    SecurityUtils.validateIdentifier("var123");
    SecurityUtils.validateIdentifier("myContentType");
    SecurityUtils.validateIdentifier("_test");
  }

  /**
   * Test validateIdentifier with malicious SQL injection attempts
   */
  @Test(expected = SecurityException.class)
  public void test_validateIdentifier_sqlInjection() {
    SecurityUtils.validateIdentifier("'; DROP TABLE containers; --");
  }

  /**
   * Test validateIdentifier with XSS attempts
   */
  @Test(expected = SecurityException.class)
  public void test_validateIdentifier_xssAttempt() {
    SecurityUtils.validateIdentifier("<script>alert('xss')</script>");
  }

  /**
   * Test validateIdentifier with null value
   */
  @Test(expected = SecurityException.class)
  public void test_validateIdentifier_null() {
    SecurityUtils.validateIdentifier(null);
  }

  /**
   * Test validateIdentifier with empty string
   */
  @Test(expected = SecurityException.class)
  public void test_validateIdentifier_empty() {
    SecurityUtils.validateIdentifier("");
  }

  /**
   * Test validateIdentifier with invalid characters
   */
  @Test(expected = SecurityException.class)
  public void test_validateIdentifier_invalidChars() {
    SecurityUtils.validateIdentifier("test@value!");
  }

  /**
   * Test validateIdentifier with path traversal attempt
   */
  @Test(expected = SecurityException.class)
  public void test_validateIdentifier_pathTraversal() {
    SecurityUtils.validateIdentifier("../../../etc/passwd");
  }

  /**
   * Test isValidIdentifier returns true for valid inputs
   */
  @Test
  public void test_isValidIdentifier_validInputs() {
    assertTrue(SecurityUtils.isValidIdentifier("550e8400-e29b-41d4-a716-446655440000"));
    assertTrue(SecurityUtils.isValidIdentifier("SYSTEM_HOST"));
    assertTrue(SecurityUtils.isValidIdentifier("SYSTEM_FOLDER"));
    assertTrue(SecurityUtils.isValidIdentifier("myVariable"));
    assertTrue(SecurityUtils.isValidIdentifier("_test123"));
  }

  /**
   * Test isValidIdentifier returns false for invalid inputs
   */
  @Test
  public void test_isValidIdentifier_invalidInputs() {
    assertFalse(SecurityUtils.isValidIdentifier(null));
    assertFalse(SecurityUtils.isValidIdentifier(""));
    assertFalse(SecurityUtils.isValidIdentifier("'; DROP TABLE"));
    assertFalse(SecurityUtils.isValidIdentifier("<script>"));
    assertFalse(SecurityUtils.isValidIdentifier("test@value"));
    assertFalse(SecurityUtils.isValidIdentifier("../etc/passwd"));
  }

  /**
   * Test isSystemIdentifier
   */
  @Test
  public void test_isSystemIdentifier() {
    assertTrue(SecurityUtils.isSystemIdentifier("SYSTEM_HOST"));
    assertTrue(SecurityUtils.isSystemIdentifier("SYSTEM_FOLDER"));
    assertTrue(SecurityUtils.isSystemIdentifier("SYSTEM_CONTAINER"));
    assertTrue(SecurityUtils.isSystemIdentifier("SYSTEM_TEMPLATE"));
    assertTrue(SecurityUtils.isSystemIdentifier("SYSTEM_THEME"));
    assertFalse(SecurityUtils.isSystemIdentifier("system_host")); // case-sensitive
    assertFalse(SecurityUtils.isSystemIdentifier("OTHER_SYSTEM"));
    assertFalse(SecurityUtils.isSystemIdentifier(null));
  }

  /**
   * Test isValidVariableName
   */
  @Test
  public void test_isValidVariableName() {
    assertTrue(SecurityUtils.isValidVariableName("myVariable"));
    assertTrue(SecurityUtils.isValidVariableName("_privateVar"));
    assertTrue(SecurityUtils.isValidVariableName("var123"));
    assertTrue(SecurityUtils.isValidVariableName("_test"));

    assertFalse(SecurityUtils.isValidVariableName("123invalid")); // starts with number
    assertFalse(SecurityUtils.isValidVariableName("invalid-var")); // contains hyphen
    assertFalse(SecurityUtils.isValidVariableName("invalid var")); // contains space
    assertFalse(SecurityUtils.isValidVariableName(null));
    assertFalse(SecurityUtils.isValidVariableName(""));
  }

}
