package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.CloseDB;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSession;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebResourceIntegrationTest extends IntegrationTestBase {

  private static final String WRITE = "WRITE";
  private static final String READ = "READ";
  private static final String NONE = "None";

  private static HttpServletResponse response;
  private static User frontEndUser = null;
  private static User backEndUser = null;
  private static User cmsAnon = null;
  private static User apiUser = null;
  private static User cmsAdmin = null;

  /** Users created by the tests below, removed once after the class. */
  private static final List<User> createdUsers = new ArrayList<>();

  /**
   * Removes the users the tests created.
   */
  @AfterClass
  public static void cleanup() {
    createdUsers.forEach(UserDataGen::remove);
  }
  @CloseDB
  @BeforeClass
  public static void init() throws Exception {

    // Setting web app environment
    IntegrationTestInitService.getInstance().init();

    response = new MockHttpResponse().response();

    backEndUser = new UserDataGen().nextPersisted();
    APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), backEndUser);
    
    
    frontEndUser = new UserDataGen().nextPersisted();
    APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadFrontEndUserRole(), frontEndUser);
    
    apiUser = new UserDataGen().nextPersisted();
    
    cmsAnon = APILocator.getUserAPI().getAnonymousUser();


    cmsAdmin = new UserDataGen().nextPersisted();
    APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), cmsAdmin);
    APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadCMSAdminRole(), cmsAdmin);

    assertTrue("backEndUser has backend role", backEndUser.isBackendUser());
    
    assertTrue("frontEndUser has frontEnd role", frontEndUser.isFrontendUser());

    assertTrue("cmsAdmin has CMS_Admin role", cmsAdmin.isAdmin());
  }

  private HttpServletRequest anonymousRequest() {
    return new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/api/testing-web-resorce").request()).request();
  }
  
  private HttpServletRequest frontEndRequest() {
    final HttpServletRequest request = anonymousRequest();
    assertTrue("frontEndUser has frontEnd role", frontEndUser.isFrontendUser());
    request.setAttribute(WebKeys.USER, frontEndUser);
    return request;
  }
  
  private HttpServletRequest backEndRequest() {
    final HttpServletRequest request = anonymousRequest();
    assertTrue("backEndUser has backend role", backEndUser.isBackendUser());
    request.setAttribute(WebKeys.USER, backEndUser);
    return request;
  }

  private HttpServletRequest adminRequest() {
    final HttpServletRequest request = anonymousRequest();
    request.setAttribute(WebKeys.USER, cmsAdmin);
    return request;
  }

  
  private HttpServletRequest apiRequest() {
    final HttpServletRequest request = anonymousRequest();
    request.setAttribute(WebKeys.USER, apiUser);
    return request;
  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_default() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, WRITE);
    InitDataObject initDataObject = new WebResource
        .InitBuilder()
        .requestAndResponse(anonymousRequest(), response).init();

  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_server_set_to_read_and_write_required() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.WRITE)
        .requestAndResponse(anonymousRequest(), response)
        .init();
  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_server_if_no_anon_access_set() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .requiredFrontendUser(true)
        .requestAndResponse(anonymousRequest(), response)
        .init();
  }


  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void test_checkAnonymousPermissions_NONE() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, NONE);
    final InitBuilder initBuilder = new WebResource.InitBuilder()
            .rejectWhenNoUser(true)
            .requestAndResponse(anonymousRequest(), response);

    new WebResource().checkAnonymousPermissions(initBuilder,APILocator.getUserAPI().getAnonymousUser() );

  }

  @Test
  public void test_checkAnonymousPermissions_READ_works() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    final InitBuilder initBuilder = new WebResource.InitBuilder()
            .requiredAnonAccess(AnonymousAccess.READ)
            .requestAndResponse(anonymousRequest(), response);

    new WebResource().checkAnonymousPermissions(initBuilder,APILocator.getUserAPI().getAnonymousUser() );
    assertTrue("Anonymous read should be allowed", true);
  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void test_checkAnonymousPermissions_READ_fails_when_write_requested() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    final InitBuilder initBuilder = new WebResource.InitBuilder()
            .requiredAnonAccess(AnonymousAccess.WRITE)
            .requestAndResponse(anonymousRequest(), response);

    new WebResource().checkAnonymousPermissions(initBuilder,APILocator.getUserAPI().getAnonymousUser() );
    assertTrue("Anonymous read should be allowed", true);
  }


  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void test_checkAnonymousPermissions_fails_when_reject_with_no_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    final InitBuilder initBuilder = new WebResource.InitBuilder()
            .rejectWhenNoUser(true)
            .requestAndResponse(anonymousRequest(), response);

    new WebResource().checkAnonymousPermissions(initBuilder,APILocator.getUserAPI().getAnonymousUser() );
    assertTrue("Anonymous read should be allowed", true);
  }



  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_server_if_anon_access_set_NONE() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, WRITE);
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .requiredFrontendUser(true)
        .requiredAnonAccess(AnonymousAccess.NONE)
        .requestAndResponse(anonymousRequest(), response)
        .init();
  }
  
  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_access_server_if_server_set_to_none() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, NONE);
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .requiredFrontendUser(true)
        .requiredAnonAccess(AnonymousAccess.READ)
        .requestAndResponse(anonymousRequest(), response)
        .init();
  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_front_end_access_server_if_only_allowBackendUser() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, NONE);
    final InitDataObject initDataObject = new WebResource.InitBuilder()
          .requiredBackendUser(true)
          .requestAndResponse(frontEndRequest(), response)
          .init();
  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_backEnd_access_server_if_only_allowFrontEndUser() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, NONE);
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .requiredFrontendUser(true)
        .requestAndResponse(backEndRequest(), response)
        .init();
  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_apiUser_access_server_if_only_allowFrontEndUser() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, NONE);
    final InitDataObject initDataObject = new WebResource.InitBuilder()
        .requiredFrontendUser(true)
        .requestAndResponse(apiRequest(), response)
        .init();
  }

  @Test
  public void allow_front_end_by_defualt() throws Exception {

    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requestAndResponse(frontEndRequest(), response)
        .init();
    assertEquals("Frontend should be allowed", initDataObject.getUser(), frontEndUser);

  }



  @Test
  public void allow_cms_admin_when_specified() throws Exception {

    InitDataObject initDataObject =
            new WebResource.InitBuilder()
                    .requestAndResponse(adminRequest(), response)
                    .requireAdmin(true)
                    .init();
    assertEquals("CMS Admin should be allowed", initDataObject.getUser(), cmsAdmin);

  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_backend_access_server_if_multiple_required_roles_include_cmsAdmin() throws Exception {

    final InitDataObject initDataObject = new WebResource.InitBuilder()
            .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE, Role.DOTCMS_BACK_END_USER)
            .requestAndResponse(backEndRequest(), response)
            .init();
  }



  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_backend_access_server_if_backendUser_tries_to_access_cmsadmin() throws Exception {

    final InitDataObject initDataObject = new WebResource.InitBuilder()
            .requiredBackendUser(true)
            .requireAdmin(true)
            .requestAndResponse(backEndRequest(), response)
            .init();
  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_backend_access_server_if_frontendUser_tries_to_access_cmsadmin() throws Exception {

    final InitDataObject initDataObject = new WebResource.InitBuilder()
            .requiredBackendUser(true)
            .requireAdmin(true)
            .requestAndResponse(frontEndRequest(), response)
            .init();
  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_backend_access_server_if_anon_tries_to_access_cmsadmin() throws Exception {

    final InitDataObject initDataObject = new WebResource.InitBuilder()
            .requiredBackendUser(true)
            .requireAdmin(true)
            .requestAndResponse(anonymousRequest(), response)
            .init();
  }



  public void allow_backend_access_server_if_cmsadmin_tries_to_access_cmsadmin() throws Exception {

    final InitDataObject initDataObject = new WebResource.InitBuilder()
            .requiredBackendUser(true)
            .requireAdmin(true)
            .requestAndResponse(adminRequest(), response)
            .init();
    assertEquals("CMS Admin should be allowed", initDataObject.getUser(), cmsAdmin);
  }







  @Test
  public void allow_back_end_by_defualt() throws Exception {

    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requestAndResponse(backEndRequest(), response)
        .init();
    assertEquals("backend should be allowed", initDataObject.getUser(), backEndUser);

  }
  
  @Test
  public void allow_back_end_if_only_USER_ID_set_in_request() throws Exception {

    final HttpServletRequest request = anonymousRequest();
    request.setAttribute(WebKeys.USER_ID, backEndUser.getUserId());

    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requestAndResponse(request, response)
        .init();
    assertEquals("backend should be allowed", initDataObject.getUser(), backEndUser);

  }
  

  @Test
  public void allow_anon_access_if_server_set_to_write() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, WRITE);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.WRITE)
        .requestAndResponse(anonymousRequest(), response)
        .init();
    assertEquals("Anonymous should be allowed", initDataObject.getUser(), cmsAnon);

  }
  
  @Test
  public void allow_anon_access_if_server_set_to_write_and_read_required() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, WRITE);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .requestAndResponse(anonymousRequest(), response)
        .init();
    assertEquals("Anonymous should be allowed", initDataObject.getUser(), cmsAnon);

  }
  
  @Test
  public void allow_anon_access_if_server_set_to_write_and_read_required_with_required_roles() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, WRITE);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .requiredBackendUser(true)
        .requiredFrontendUser(true)
        .requestAndResponse(anonymousRequest(), response)
        .init();
    assertEquals("Anonymous should be allowed", initDataObject.getUser(), cmsAnon);

  }
  
  @Test
  public void allow_anon_access_if_server_set_to_write_and_frontend_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, WRITE);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.WRITE)
        .requiredFrontendUser(true)
        .requestAndResponse(anonymousRequest(), response)
        .init();
    assertEquals("Anonymous should be allowed", initDataObject.getUser(), cmsAnon);

  }

  @Test
  public void allow_anon_access_if_server_set_to_read_and_frontend_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .requiredFrontendUser(true)
        .requestAndResponse(anonymousRequest(), response)
        .init();
    assertEquals("Anonymous should be allowed", initDataObject.getUser(), cmsAnon);

  }

  @Test
  public void allow_frontEnd_access_if_server_set_to_read_and_frontend_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .requiredFrontendUser(true)
        .requestAndResponse(frontEndRequest(), response)
        .init();
    assertEquals("frontEndUser should be allowed", initDataObject.getUser(), frontEndUser);

  }

  @Test
  public void allow_backEnd_access_if_server_set_to_read_and_bakckend_user() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .requiredBackendUser(true)
        .requestAndResponse(backEndRequest(), response)
        .init();
    assertEquals("backEnd should be allowed", initDataObject.getUser(), backEndUser);

  }

  @Test
  public void allow_backEnd_access_if_both_allow_front_and_backend_users() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requiredAnonAccess(AnonymousAccess.READ)
        .requiredFrontendUser(true)
        .requiredBackendUser(true)
        .requestAndResponse(backEndRequest(), response)
        .init();
    assertEquals("backEnd should be allowed", initDataObject.getUser(), backEndUser);

  }

  @Test
  public void allow_api_user_access_if_server_set_to_nothing() throws Exception {
    InitDataObject initDataObject =
        new WebResource.InitBuilder()
        .requestAndResponse(apiRequest(), response)
        .init();
    assertEquals("apiUser should be allowed", initDataObject.getUser(), apiUser);

  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_api_user_when_required_both_frontend_and_backend() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    InitDataObject initDataObject =
            new WebResource.InitBuilder()
                    .requiredAnonAccess(AnonymousAccess.READ)
                    .requiredFrontendUser(true)
                    .requiredBackendUser(true)
                    .requestAndResponse(apiRequest(), response)
                    .init();

  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_when_required_both_frontend_and_backend() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    InitDataObject initDataObject =
            new WebResource.InitBuilder()
                    .requiredFrontendUser(false)
                    .requiredBackendUser(true)
                    .requestAndResponse(anonymousRequest(), response)
                    .init();

  }

  @Test(expected = com.dotcms.rest.exception.SecurityException.class)
  public void disallow_anon_when_required_frontend_but_not_backend() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, READ);
    InitDataObject initDataObject =
            new WebResource.InitBuilder()
                    .requiredFrontendUser(true)
                    .requiredBackendUser(false)
                    .requestAndResponse(anonymousRequest(), response)
                    .init();

  }

  /**
   * Method to test: {@link InitBuilder#init()} with {@link InitBuilder#requiredBackendUser(boolean)}
   * Given Scenario: a back-end user logged in through the session, which holds the user id, is
   * made inactive after the login.
   * ExpectedResult: a request on that session is accepted before the change and answered 401
   * after it.
   */
  @Test
  public void reject_session_user_made_inactive_after_login() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, NONE);
    final User user = new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
    createdUsers.add(user);
    final HttpSession session = new MockSession(UUIDGenerator.generateUuid());
    session.setAttribute(WebKeys.USER_ID, user.getUserId());

    final InitDataObject accepted = new WebResource.InitBuilder()
        .requiredBackendUser(true)
        .requestAndResponse(requestOnSession(session), response)
        .init();
    assertEquals("an active session user is accepted", user.getUserId(), accepted.getUser().getUserId());

    final User toSave = (User) APILocator.getUserAPI().loadUserById(user.getUserId()).clone();
    toSave.setActive(false);
    APILocator.getUserAPI().save(toSave, APILocator.systemUser(), false);

    try {
      new WebResource.InitBuilder()
          .requiredBackendUser(true)
          .requestAndResponse(requestOnSession(session), response)
          .init();
      fail("a session whose user was made inactive must be rejected");
    } catch (final com.dotcms.rest.exception.SecurityException e) {
      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
    }
  }

  /**
   * Method to test: {@link InitBuilder#init()} with {@link InitBuilder#requiredBackendUser(boolean)}
   * Given Scenario: a back-end user logged in through the session is deleted after the login.
   * ExpectedResult: a request on that session is answered 401 after the deletion.
   */
  @Test
  public void reject_session_user_deleted_after_login() throws Exception {
    Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, NONE);
    final User user = new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
    final HttpSession session = new MockSession(UUIDGenerator.generateUuid());
    session.setAttribute(WebKeys.USER_ID, user.getUserId());
    final InitDataObject accepted = new WebResource.InitBuilder()
        .requiredBackendUser(true)
        .requestAndResponse(requestOnSession(session), response)
        .init();
    assertEquals(user.getUserId(), accepted.getUser().getUserId());

    APILocator.getUserAPI().delete(APILocator.getUserAPI().loadUserById(user.getUserId()),
        APILocator.systemUser(), APILocator.systemUser(), false);

    try {
      new WebResource.InitBuilder()
          .requiredBackendUser(true)
          .requestAndResponse(requestOnSession(session), response)
          .init();
      fail("a session whose user was deleted must be rejected");
    } catch (final com.dotcms.rest.exception.SecurityException e) {
      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
    }
  }

  /**
   * A new request on the given session, as each browser request after a login is. Each request
   * is a new object because the user resolved for a request is kept on that request.
   */
  private static HttpServletRequest requestOnSession(final HttpSession session) {
    final MockSessionRequest sessionRequest = new MockSessionRequest(
        new MockHttpRequestIntegrationTest("localhost", "/api/testing-web-resorce").request());
    sessionRequest.setSession(session);
    return new MockAttributeRequest(sessionRequest).request();
  }

}
