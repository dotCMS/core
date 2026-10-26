package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.business.CloseDB;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

public class WebResourceIntegrationTest extends IntegrationTestBase {

  private static final String WRITE = "WRITE";
  private static final String READ = "READ";
  private static final String NONE = "None";

  private final List<User> authenticationUsers = new ArrayList<>();
  private final List<ApiToken> authenticationTokens = new ArrayList<>();
  private final List<HttpServletRequest> authenticationRequests = new ArrayList<>();
  private final String authenticationPassword = UUIDGenerator.generateUuid();
  private String previousBasicAuth;
  private String previousAnonymousAccess;
  private String previousForceSsl;
  private String previousPrincipal;
  private HttpServletRequest previousRequest;

  /** Saves the authentication policy and thread-local identity for each test. */
  @Before
  public void saveAuthenticationState() {
    previousBasicAuth = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
    previousAnonymousAccess = Config.getStringProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, null);
    previousForceSsl = Config.getStringProperty("FORCE_SSL_ON_RESP_API", null);
    previousPrincipal = PrincipalThreadLocal.getName();
    previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
  }

  /** Removes only this test's fixtures and restores shared authentication state. */
  @After
  public void restoreAuthenticationState() {
    try {
      for (final HttpServletRequest request : authenticationRequests) {
        final HttpSession session = request.getSession(false);
        if (session != null) {
          session.invalidate();
        }
      }
      for (final ApiToken token : authenticationTokens) {
        APILocator.getApiTokenAPI().deleteToken(token, APILocator.systemUser());
      }
      for (final User user : authenticationUsers) {
        // Silent like every other fixture cleanup: UserAPIImpl.delete NPEs when the default
        // replacement user has no personal role, and a throw here would abort the rest of the
        // cleanup and error every test in the class.
        UserDataGen.remove(user);
      }
    } finally {
      Config.setProperty("ALLOW_BASIC_AUTH", previousBasicAuth);
      Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, previousAnonymousAccess);
      Config.setProperty("FORCE_SSL_ON_RESP_API", previousForceSsl);
      PrincipalThreadLocal.setName(previousPrincipal);
      HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
    }
  }

  private static HttpServletResponse response;
  private static User frontEndUser = null;
  private static User backEndUser = null;
  private static User cmsAnon = null;
  private static User apiUser = null;
  private static User cmsAdmin = null;
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
  
  /** Default and invalid settings must reject even correct password or API-JWT credentials. */
  @Test
  public void basic_password_and_api_jwt_fail_closed_without_explicit_enablement() throws Exception {
    final User user = authenticationUser();
    final String jwt = apiJwt(authenticationToken(user, null));
    for (final String mode : new String[]{null, "NONE", "FALSE", "", "invalid"}) {
      Config.setProperty("ALLOW_BASIC_AUTH", mode);
      assertCredentialDenied("Basic", loginName(user), authenticationPassword);
      assertCredentialDenied("Basic", loginName(user), jwt);
    }
  }

  /** Token mode accepts a persisted same-owner API JWT, not a real account password. */
  @Test
  public void jwt_token_mode_accepts_api_jwt_but_not_password() throws Exception {
    final User user = authenticationUser();
    final String jwt = apiJwt(authenticationToken(user, null));
    final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
    try (MockedStatic<APILocator> apis = mockStatic(APILocator.class, CALLS_REAL_METHODS)) {
      apis.when(APILocator::getLoginServiceAPI).thenReturn(loginService);
      try {
        for (final String mode : new String[]{"JWT_TOKEN", " jwt_token "}) {
          Config.setProperty("ALLOW_BASIC_AUTH", mode);
          assertCredentialUser(user, "Basic", jwt);
          assertCredentialDenied("Basic", loginName(user), authenticationPassword);
        }
      } finally {
        verifyNoInteractions(loginService);
      }
    }
  }

  /** Compatibility mode retains real password login and adds username-bound API-JWT login. */
  @Test
  public void all_mode_accepts_password_and_api_jwt() throws Exception {
    final User user = authenticationUser();
    final String jwt = apiJwt(authenticationToken(user, null));
    for (final String mode : new String[]{"ALL", " all "}) {
      Config.setProperty("ALLOW_BASIC_AUTH", mode);
      assertCredentialUser(user, "Basic", authenticationPassword);
      assertCredentialUser(user, "Basic", jwt);
    }
  }

  /** DOTAUTH and the live user/password path parameters must obey the same policy as Basic. */
  @Test
  public void legacy_header_and_url_credentials_obey_authentication_mode() throws Exception {
    final User user = authenticationUser();
    final String jwt = apiJwt(authenticationToken(user, null));
    for (final String transport : new String[]{"DOTAUTH", "params"}) {
      Config.setProperty("ALLOW_BASIC_AUTH", "NONE");
      assertCredentialDenied(transport, loginName(user), authenticationPassword);
      assertCredentialDenied(transport, loginName(user), jwt);
      Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
      assertCredentialDenied(transport, loginName(user), authenticationPassword);
      assertCredentialUser(user, transport, jwt);
      Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
      assertCredentialUser(user, transport, authenticationPassword);
      assertCredentialUser(user, transport, jwt);
    }
  }

  /** A valid API JWT cannot be used with another existing account's login name in either mode. */
  @Test
  public void api_jwt_basic_requires_matching_username_in_both_enabled_modes() throws Exception {
    final User owner = authenticationUser();
    final User other = authenticationUser();
    final String jwt = apiJwt(authenticationToken(owner, null));
    for (final String mode : new String[]{"JWT_TOKEN", "ALL"}) {
      Config.setProperty("ALLOW_BASIC_AUTH", mode);
      assertCredentialUser(owner, "Basic", jwt);
      assertCredentialDenied("Basic", loginName(other), jwt);
    }
  }

  /** Token-only mode must not confuse a valid browser USER_TOKEN with an API token. */
  @Test
  public void jwt_token_mode_rejects_user_token_and_malformed_jwt() throws Exception {
    final User user = authenticationUser();
    final String userToken = JsonWebTokenUtils.getInstance().createUserToken(user, 1);
    assertEquals("The browser token fixture must be valid before testing Basic rejection",
        user.getUserId(), APILocator.getApiTokenAPI().userFromJwt(userToken, "127.0.0.1").get().getUserId());
    Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
    assertCredentialDenied("Basic", loginName(user), userToken);
    assertCredentialDenied("Basic", loginName(user), "not-a-jwt");
  }

  /** Both enabled modes must validate the persisted API token's allowed client network. */
  @Test
  public void api_jwt_basic_enforces_network_in_both_enabled_modes() throws Exception {
    final User user = authenticationUser();
    final String localJwt = apiJwt(authenticationToken(user, "127.0.0.0/8"));
    final String foreignJwt = apiJwt(authenticationToken(user, "192.0.2.0/24"));
    for (final String mode : new String[]{"JWT_TOKEN", "ALL"}) {
      Config.setProperty("ALLOW_BASIC_AUTH", mode);
      assertCredentialUser(user, "Basic", localJwt);
      assertCredentialDenied("Basic", loginName(user), foreignJwt);
    }
  }

  /** Enabled credential modes must not bypass mandatory HTTPS for otherwise valid credentials. */
  @Test
  public void enabled_basic_credentials_cannot_bypass_force_ssl() throws Exception {
    final User user = authenticationUser();
    final String jwt = apiJwt(authenticationToken(user, null));
    assertEquals(user.getUserId(), APILocator.getApiTokenAPI()
        .userFromJwt(jwt, "127.0.0.1").get().getUserId());
    Config.setProperty("FORCE_SSL_ON_RESP_API", true);
    for (final String mode : new String[]{"JWT_TOKEN", "ALL"}) {
      Config.setProperty("ALLOW_BASIC_AUTH", mode);
      final String[] credentials = "ALL".equals(mode)
          ? new String[]{jwt, authenticationPassword} : new String[]{jwt};
      for (final String credential : credentials) {
        final HttpServletRequest request = credentialRequest("Basic", loginName(user), credential);
        assertFalse("The request must be insecure to exercise the SSL requirement", request.isSecure());
        final SecurityException denied = assertThrows(SecurityException.class,
            () -> new WebResource.InitBuilder().requestAndResponse(request, response).init());
        assertEquals(403, denied.getResponse().getStatus());
      }
    }
  }

  /** A successful API-JWT request cannot mint a password session or cookie that survives revocation. */
  @Test
  public void api_jwt_basic_rechecks_revocation_without_creating_login_session() throws Exception {
    final User user = authenticationUser();
    for (final String mode : new String[]{"JWT_TOKEN", "ALL"}) {
      Config.setProperty("ALLOW_BASIC_AUTH", mode);
      final ApiToken token = authenticationToken(user, null);
      final String jwt = apiJwt(token);
      final HttpServletRequest request = credentialRequest("Basic", loginName(user), jwt);
      final HttpServletResponse tokenResponse = mock(HttpServletResponse.class);
      assertEquals(user.getUserId(), new WebResource.InitBuilder()
          .requestAndResponse(request, tokenResponse).init().getUser().getUserId());
      verify(tokenResponse, never()).addCookie(any());
      verify(tokenResponse, never()).setHeader(eq("Set-Cookie"), any());
      verify(tokenResponse, never()).addHeader(eq("Set-Cookie"), any());
      final HttpSession session = request.getSession(false);
      if (session != null) {
        assertNull("API-JWT authentication must not establish a password login session",
            session.getAttribute(WebKeys.USER_ID));
        assertNull("API-JWT authentication must not cache an authenticated session user",
            session.getAttribute(WebKeys.USER));
      }
      final MockSessionRequest sessionReplay = new MockSessionRequest(authenticationRequest());
      sessionReplay.setSession(session);
      assertThrows(SecurityException.class, () -> new WebResource.InitBuilder()
          .requestAndResponse(sessionReplay, response).init());
      APILocator.getApiTokenAPI().revokeToken(token, APILocator.systemUser());
      DateUtil.sleep(1000); // The existing token fixture considers revocation strictly before now.
      assertCredentialDenied("Basic", loginName(user), jwt);
    }
  }

  /** Disabling Basic must not disable ordinary Bearer or already authenticated session access. */
  @Test
  public void none_mode_preserves_bearer_and_session_authentication() throws Exception {
    Config.setProperty("ALLOW_BASIC_AUTH", "NONE");
    final User user = authenticationUser();
    final String jwt = apiJwt(authenticationToken(user, null));
    final HttpServletRequest bearer = new MockHeaderRequest(authenticationRequest(),
        "Authorization", "Bearer " + jwt);
    assertEquals(user.getUserId(), new WebResource.InitBuilder()
        .requestAndResponse(bearer, response).init().getUser().getUserId());
    final HttpServletRequest sessionRequest = authenticationRequest();
    sessionRequest.getSession().setAttribute(WebKeys.USER_ID, user.getUserId());
    final HttpServletRequest replayedBasic = new MockHeaderRequest(sessionRequest, "Authorization",
        "Basic " + Base64.getEncoder().encodeToString(
            "foreign-proxy-user:foreign-proxy-secret".getBytes(StandardCharsets.UTF_8)));
    assertEquals(user.getUserId(), new WebResource.InitBuilder()
        .requestAndResponse(replayedBasic, response).init().getUser().getUserId());
  }

  /** Only the explicit servlet fallback may ignore foreign Basic credentials for public assets. */
  @Test
  public void foreign_basic_can_fall_back_to_anonymous_only_for_public_asset_callers() {
    for (final String mode : new String[]{"NONE", "JWT_TOKEN", "ALL"}) {
      Config.setProperty("ALLOW_BASIC_AUTH", mode);
      final HttpServletRequest asset = credentialRequest("Basic", "foreign-proxy-user", "foreign-proxy-secret");
      assertEquals(cmsAnon.getUserId(), new WebResource().getCurrentUser(asset, response,
          Collections.emptyMap(), AnonymousAccess.READ, true).getUserId());
      assertCredentialDenied("Basic", "foreign-proxy-user", "foreign-proxy-secret");
      final HttpServletRequest protectedAsset = credentialRequest("Basic", "foreign-proxy-user", "foreign-proxy-secret");
      assertThrows(SecurityException.class, () -> new WebResource().getCurrentUser(protectedAsset,
          response, Collections.emptyMap(), AnonymousAccess.NONE, true));
    }
  }

  /** API-JWT authentication must not grant administrator permissions to a non-admin owner. */
  @Test
  public void api_jwt_basic_preserves_protected_resource_permissions() throws Exception {
    final User user = authenticationUser();
    final String jwt = apiJwt(authenticationToken(user, null));
    for (final String mode : new String[]{"JWT_TOKEN", "ALL"}) {
      Config.setProperty("ALLOW_BASIC_AUTH", mode);
      assertCredentialUser(user, "Basic", jwt);
      final HttpServletRequest request = credentialRequest("Basic", loginName(user), jwt);
      assertThrows(SecurityException.class, () -> new WebResource.InitBuilder()
          .requestAndResponse(request, response).requireAdmin(true).init());
    }
  }

  /** Creates a disposable front-end account with a known password and no administrator role. */
  private User authenticationUser() throws DotDataException {
    final User user = new UserDataGen().emailAddress(UUIDGenerator.generateUuid() + "@example.com")
        .password(authenticationPassword)
        .roles(APILocator.getRoleAPI().loadFrontEndUserRole()).nextPersisted();
    authenticationUsers.add(user);
    return user;
  }

  /** Uses the company's actual login convention rather than assuming email-based authentication. */
  private String loginName(final User user) {
    return Company.AUTH_TYPE_EA.equals(CompanyUtils.getDefaultCompany().getAuthType())
        ? user.getEmailAddress() : user.getUserId();
  }

  /** Persists an API token using the same fixture API as the JWT integration tests. */
  private ApiToken authenticationToken(final User user, final String network) {
    final ApiToken token = APILocator.getApiTokenAPI().persistApiToken(ApiToken.builder()
        .withUserId(user.getUserId()).withExpires(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
        .withRequestingIp("127.0.0.1").withAllowNetwork(network == null ? "0.0.0.0/0" : network)
        .build(), APILocator.systemUser());
    authenticationTokens.add(token);
    return token;
  }

  /** Signs a JWT for a persisted API token. */
  private String apiJwt(final ApiToken token) {
    return APILocator.getApiTokenAPI().getJWT(token, APILocator.systemUser());
  }

  /** Creates a fresh request with no cookies, authentication attributes or shared session. */
  private HttpServletRequest authenticationRequest() {
    final HttpServletRequest request = anonymousRequest();
    authenticationRequests.add(request);
    return request;
  }

  /** Builds header credentials; URL credentials are passed through InitBuilder.params instead. */
  private HttpServletRequest credentialRequest(final String transport, final String username,
      final String credential) {
    final HttpServletRequest request = authenticationRequest();
    if ("params".equals(transport)) {
      return request;
    }
    final String encoded = Base64.getEncoder().encodeToString(
        (username + ":" + credential).getBytes(StandardCharsets.UTF_8));
    return new MockHeaderRequest(request, "Basic".equals(transport) ? "Authorization" : "DOTAUTH",
        "Basic".equals(transport) ? "Basic " + encoded : encoded);
  }

  /** Exercises the real URL keys user/password, not the unused userid/pwd builder aliases. */
  private InitBuilder credentialBuilder(final String transport, final String username,
      final String credential) {
    final InitBuilder builder = new WebResource.InitBuilder()
        .requestAndResponse(credentialRequest(transport, username, credential), response);
    return "params".equals(transport)
        ? builder.params("/user/" + username + "/password/" + credential) : builder;
  }

  /** Asserts an authenticated identity without bypassing the resource's permission checks. */
  private void assertCredentialUser(final User user, final String transport, final String credential) {
    assertEquals(user.getUserId(), credentialBuilder(transport, loginName(user), credential)
        .init().getUser().getUserId());
  }

  /** Asserts strict REST rejection rather than accepting an anonymous fallback. */
  private void assertCredentialDenied(final String transport, final String username,
      final String credential) {
    final SecurityException denied = assertThrows(SecurityException.class,
        () -> credentialBuilder(transport, username, credential).init());
    assertEquals(401, denied.getResponse().getStatus());
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



}
