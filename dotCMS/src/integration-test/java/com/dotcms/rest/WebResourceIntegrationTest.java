package com.dotcms.rest;

import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.junit.Before;
import org.junit.Test;

import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

public class WebResourceIntegrationTest {


  private HttpServletResponse response;
  private User frontEndUser = null;
  private User backEndUser = null;
  private User cmsAnon = null;
  private User apiUser = null;

  @Before
  public void init() throws Exception {

    // Setting web app environment
    IntegrationTestInitService.getInstance().init();

    response = new MockHttpResponse().response();

    backEndUser = new UserDataGen().nextPersisted();
    APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), backEndUser);
    
    
    frontEndUser = new UserDataGen().nextPersisted();
    APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadFrontEndUserRole(), frontEndUser);
    
    apiUser = new UserDataGen().nextPersisted();
    
    cmsAnon = APILocator.getUserAPI().getAnonymousUser();
    
    
    assertTrue("backEndUser has backend role", backEndUser.isBackendUser());
    
    assertTrue("frontEndUser has frontEnd role", frontEndUser.isFrontendUser());
  }

  private HttpServletRequest request() {
    return new MockAttributeRequest(new MockHttpRequest("localhost", "/api/testing-web-resorce").request()).request();
  }
  
  private HttpServletRequest frontEndRequest() {
    HttpServletRequest request = request();
    request.setAttribute(WebKeys.USER, frontEndUser);
    return request;
  }
  
  private HttpServletRequest backEndRequest() {
    HttpServletRequest request = request();
    request.setAttribute(WebKeys.USER, backEndUser);
    return request;
  }
  
  private HttpServletRequest apiRequest() {
    HttpServletRequest request = request();
    request.setAttribute(WebKeys.USER, apiUser);
    return request;
  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_default() throws Exception {

    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");

      InitDataObject initDataObject = new WebResource
          .InitBuilder()
          .requestAndResponse(request(), response).init();


  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_server_set_to_read_and_write_required() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.WRITE)
        .requestAndResponse(request(), response)
        .init();

  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_server_if_no_anon_access_set() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .allowFrontendUser(true)
        .requestAndResponse(request(), response)
        .init();


  }
  
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_server_if_anon_access_set_NONE() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .allowFrontendUser(true)
        .requiredAnonAccess(AnonymousAccess.NONE)
        .requestAndResponse(request(), response)
        .init();


  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_server_if_server_set_to_none() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "None");
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .allowFrontendUser(true)
        .requiredAnonAccess(AnonymousAccess.READ)
        .requestAndResponse(request(), response)
        .init();
  }
  
  
  @Test
  public void disallow_front_end_access_server_if_only_allowBackendUser() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "None");
    Exception exception = null;
    try {
    
      final InitDataObject initDataObject = new WebResource.InitBuilder()
          .allowBackendUser(true)
          .requestAndResponse(frontEndRequest(), response)
          .init();

    }catch(Exception e) {
      exception=e;
    }
    
    assertTrue("we MUST have an exception", exception!=null);
    assertTrue("we need a SecurityException, got a " + exception, exception instanceof SecurityException);
    
  }
  
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_backEnd_access_server_if_only_allowFrontEndUser() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "None");
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .allowFrontendUser(true)
        .requestAndResponse(backEndRequest(), response)
        .init();

  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_apiUser_access_server_if_only_allowFrontEndUser() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "None");
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .allowFrontendUser(true)
        .requestAndResponse(apiRequest(), response)
        .init();

  }
  
  
  @Test
  public void allow_front_end_by_defualt() throws Exception {

    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requestAndResponse(frontEndRequest(), response)
        .init();
    assertTrue("Frontend should be allowed", initDataObject.getUser().equals(frontEndUser));

  }
  
  @Test
  public void allow_back_end_by_defualt() throws Exception {

    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requestAndResponse(backEndRequest(), response)
        .init();
    assertTrue("backend should be allowed", initDataObject.getUser().equals(backEndUser));

  }
  
  @Test
  public void allow_back_end_if_only_USER_ID_set_in_request() throws Exception {

    HttpServletRequest request = request();
    request.setAttribute(WebKeys.USER_ID, backEndUser.getUserId());
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requestAndResponse(request, response)
        .init();
    assertTrue("backend should be allowed", initDataObject.getUser().equals(backEndUser));

  }
  
  
  @Test
  public void allow_anon_access_if_server_set_to_write() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.WRITE)
        .requestAndResponse(request(), response)
        .init();
    assertTrue("Anonymous should be allowed", initDataObject.getUser().equals(cmsAnon));

  }
  
  @Test
  public void allow_anon_access_if_server_set_to_write_and_read_required() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .requestAndResponse(request(), response)
        .init();
    assertTrue("Anonymous should be allowed", initDataObject.getUser().equals(cmsAnon));

  }
  
  @Test
  public void allow_anon_access_if_server_set_to_write_and_read_required_with_required_roles() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .allowBackendUser(true)
        .allowFrontendUser(true)
        .requestAndResponse(request(), response)
        .init();
    assertTrue("Anonymous should be allowed", initDataObject.getUser().equals(cmsAnon));

  }
  
  @Test
  public void allow_anon_access_if_server_set_to_write_and_frontend_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.WRITE)
        .allowFrontendUser(true)
        .requestAndResponse(request(), response)
        .init();
    assertTrue("Anonymous should be allowed", initDataObject.getUser().equals(cmsAnon));

  }
  
  @Test
  public void allow_anon_access_if_server_set_to_read_and_frontend_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .allowFrontendUser(true)
        .requestAndResponse(request(), response)
        .init();
    assertTrue("Anonymous should be allowed", initDataObject.getUser().equals(cmsAnon));

  }
  
  @Test
  public void allow_frontEnd_access_if_server_set_to_read_and_frontend_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .allowFrontendUser(true)
        .requestAndResponse(frontEndRequest(), response)
        .init();
    assertTrue("frontEndUser should be allowed", initDataObject.getUser().equals(frontEndUser));

  }
  
  @Test
  public void allow_backEnd_access_if_server_set_to_read_and_bakckend_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .allowBackendUser(true)
        .requestAndResponse(backEndRequest(), response)
        .init();
    assertTrue("backEnd should be allowed", initDataObject.getUser().equals(backEndUser));

  }
  @Test
  public void allow_backEnd_access_if_both_allow_front_and_backend_users() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .allowFrontendUser(true)
        .allowBackendUser(true)
        .requestAndResponse(backEndRequest(), response)
        .init();
    assertTrue("backEnd should be allowed", initDataObject.getUser().equals(backEndUser));

  }
  
  
  @Test
  public void allow_api_user_access_if_server_set_to_nothing() throws Exception {
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requestAndResponse(apiRequest(), response)
        .init();
    assertTrue("apiUser should be allowed", initDataObject.getUser().equals(apiUser));

  }
  
  
  
  
}
