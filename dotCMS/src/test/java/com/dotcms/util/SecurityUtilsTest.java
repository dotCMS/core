package com.dotcms.util;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestUnitTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.google.common.collect.ImmutableList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
