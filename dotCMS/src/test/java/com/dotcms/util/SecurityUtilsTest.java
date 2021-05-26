package com.dotcms.util;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.google.common.collect.ImmutableList;

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

  /**
   * Method to test: {@link SecurityUtils#validateReferer(HttpServletRequest)}
   * Given Scenario: a font under /html/fonts
   * ExpectedResult: is a valid uri
   */
  @Test
  public void test_valid_referers_fonts() throws Exception {

    SecurityUtils utils = new SecurityUtils();
    SecurityUtils spyUtils = Mockito.spy(utils);
    Mockito.doReturn(ImmutableList.of(PORTAL_HOST)).when(spyUtils).loadIgnoreHosts();
    Mockito.doReturn(ImmutableList.of(IGNORE_THIS_URI,IGNORE_UNDER_THIS_URI)).when(spyUtils).loadIgnorePaths();
    Mockito.doReturn(PORTAL_HOST).when(spyUtils).getPortalHost();
    HttpServletRequest request = mockRequest("localhost", "/html/fonts/test.otf", null);
    assert(spyUtils.validateReferer(request));
  }

  /**
   * Method to test: {@link SecurityUtils#validateReferer(HttpServletRequest)}
   * Given Scenario: a font under /html/invalid
   * ExpectedResult: is an invalid uri
   */
  @Test
  public void test_invalid_referers_fonts() throws Exception {

    SecurityUtils utils = new SecurityUtils();
    SecurityUtils spyUtils = Mockito.spy(utils);
    Mockito.doReturn(ImmutableList.of(PORTAL_HOST)).when(spyUtils).loadIgnoreHosts();
    Mockito.doReturn(ImmutableList.of(IGNORE_THIS_URI,IGNORE_UNDER_THIS_URI)).when(spyUtils).loadIgnorePaths();
    Mockito.doReturn(PORTAL_HOST).when(spyUtils).getPortalHost();
    HttpServletRequest request = mockRequest("localhost", "/html/invalid/test.otf", null);
    Assert.assertFalse(spyUtils.validateReferer(request));

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
    

    return new MockHeaderRequest(new MockAttributeRequest(new MockSessionRequest(new MockHttpRequest(host, uri).request()).request()).request(), "referer", referer);
  }

}
