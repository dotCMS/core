package com.dotcms.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A request or session whose identity is the built-in account is treated as having no user, through the real
 * request path. Every refusal is compared with a request that carries no identity at all, and a real
 * administrator goes through the same helper as the negative control.
 */
public class InteractiveUserResolutionIT extends IntegrationTestBase {

    private static HttpServletResponse response;
    private static User administrator;
    private final List<HttpServletRequest> requests = new ArrayList<>();
    private final List<ApiToken> tokens = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        response = new MockHttpResponse().response();
        administrator = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), administrator);
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadCMSAdminRole(), administrator);
    }

    private HttpServletRequest previousRequest;

    @Before
    public void saveThreadRequest() {
        previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    @After
    public void cleanup() throws Exception {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        for (final HttpServletRequest request : requests) {
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
        }
        for (final ApiToken token : tokens) {
            APILocator.getApiTokenAPI().deleteToken(token, APILocator.systemUser());
        }
    }

    private HttpServletRequest freshRequest() {
        final HttpServletRequest request = new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/interactive-user-resolution").request())
                .request());
        requests.add(request);
        return request;
    }

    private int statusOf(final HttpServletRequest request) {
        try {
            final User resolved = new WebResource.InitBuilder().requestAndResponse(request, response)
                    .requireAdmin(true).init().getUser();
            throw new AssertionError("expected a refusal but resolved user: " + resolved.getUserId());
        } catch (final SecurityException denied) {
            return denied.getResponse().getStatus();
        }
    }

    private int controlStatus() {
        return statusOf(freshRequest());
    }

    private static User system() throws Exception {
        return APILocator.getUserAPI().getSystemUser();
    }

    /** Sanity: a request that carries no identity is refused by the helper (the comparison baseline). */
    @Test
    public void request_without_identity_is_refused() {
        controlStatus();
    }

    /** A session whose cached user is the built-in account behaves like a request with no identity. */
    @Test
    public void session_user_for_the_built_in_account_matches_the_no_identity_control() throws Exception {
        final HttpServletRequest request = freshRequest();
        request.getSession().setAttribute(WebKeys.USER, system());
        assertEquals(controlStatus(), statusOf(request));
    }

    /** A session holding only the built-in account's id behaves the same. */
    @Test
    public void session_user_id_for_the_built_in_account_matches_the_no_identity_control() {
        final HttpServletRequest request = freshRequest();
        request.getSession().setAttribute(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        assertEquals(controlStatus(), statusOf(request));
    }

    /** Negative control: a real administrator session through the same helper is accepted. */
    @Test
    public void real_administrator_session_is_accepted() throws Exception {
        final HttpServletRequest request = freshRequest();
        request.getSession().setAttribute(WebKeys.USER, administrator);
        request.getSession().setAttribute(WebKeys.USER_ID, administrator.getUserId());
        assertEquals(administrator.getUserId(), new WebResource.InitBuilder()
                .requestAndResponse(request, response).requireAdmin(true).init()
                .getUser().getUserId());
    }

    /** Repeated requests on a refused session stay refused and leave unrelated session data intact. */
    @Test
    public void repeated_requests_stay_refused_and_keep_unrelated_session_data() throws Exception {
        final HttpServletRequest request = freshRequest();
        request.getSession().setAttribute(WebKeys.USER, system());
        request.getSession().setAttribute("unrelated", "kept");
        final int expected = controlStatus();
        for (int i = 0; i < 3; i++) {
            assertEquals(expected, statusOf(request));
        }
        assertEquals("kept", request.getSession().getAttribute("unrelated"));
    }

    /** A bearer token whose user is the built-in account behaves like a request with no identity. */
    @Test
    public void bearer_token_for_the_built_in_account_matches_the_no_identity_control() throws Exception {
        final ApiToken token = APILocator.getApiTokenAPI().persistApiToken(ApiToken.builder()
                .withUserId(UserAPI.SYSTEM_USER_ID)
                .withExpires(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .withRequestingIp("127.0.0.1").withAllowNetwork("0.0.0.0/0").build(), APILocator.systemUser());
        tokens.add(token);
        final String jwt = APILocator.getApiTokenAPI().getJWT(token, APILocator.systemUser());
        assertNotNull(jwt);
        final HttpServletRequest bearer = new MockHeaderRequest(freshRequest(), "Authorization", "Bearer " + jwt);
        assertEquals(controlStatus(), statusOf(bearer));
    }
}
