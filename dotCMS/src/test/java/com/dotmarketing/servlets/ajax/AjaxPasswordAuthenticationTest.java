package com.dotmarketing.servlets.ajax;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.publisher.ajax.RemotePublishAjaxAction;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import javax.ws.rs.core.Response;
import com.dotcms.publisher.endpoint.ajax.PublishingEndpointAjaxAction;
import com.dotcms.publisher.environment.ajax.EnvironmentAjaxAction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction;
import com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Contract test for the legacy password fallback shared by the Ajax actions. With
 * {@code ALLOW_BASIC_AUTH} at {@code NONE} or {@code JWT_TOKEN}, a missing user or a missing
 * portlet permission must never reach {@link LoginFactory#doLogin} through the {@code u/p} or
 * {@code user/passwd} URI parameters, and the existing 401 denial must remain. Only {@code ALL}
 * keeps the legacy fallback, and only when URL credentials are explicitly enabled.
 *
 * <p>Limitation: the service fallback tests stub {@link AjaxAction#setUser(HttpServletRequest)}, so they prove
 * that the submitted credentials never reach the password login factory; they do not exercise the
 * WebResource Basic/JWT path that runs afterwards.</p>
 */
public class AjaxPasswordAuthenticationTest extends UnitTestBase {

    private String previousMode;
    private String previousUrlCredentials;

    /** Preserves the mode property so an assertion failure cannot leak configuration. */
    @Before
    public void saveMode() {
        previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        previousUrlCredentials = Config.getStringProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", null);
        Config.setProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", true);
    }

    /** Restores the mode property after each test. */
    @After
    public void restoreMode() {
        Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
        Config.setProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", previousUrlCredentials);
    }

    /** Restrictive modes must reject u/p and user/passwd without invoking the password login. */
    @Test
    public void restrictiveModesNeverFallBackToPasswordLogin() throws Exception {
        for (final String mode : new String[]{"NONE", "JWT_TOKEN"}) {
            Config.setProperty("ALLOW_BASIC_AUTH", mode);

            for (final Class<? extends AjaxAction> actionType : allActionTypes()) {
                assertNoPasswordFallback(actionType, credentials("u", "p"), null);
            }
            for (final Class<? extends AjaxAction> actionType : portletGatedActionTypes()) {
                assertNoPasswordFallback(actionType, credentials("u", "p"), mockUser());
            }
            for (final Class<? extends AjaxAction> actionType : aliasActionTypes()) {
                assertNoPasswordFallback(actionType, credentials("user", "passwd"), null);
            }
        }
    }

    /** Missing, false and invalid URL opt-ins deny both query and path aliases, even in ALL mode. */
    @Test
    public void urlCredentialsAreDisabledByDefaultEvenInAllMode() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        for (final String enabled : new String[]{null, "false", "invalid"}) {
            Config.setProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", enabled);
            for (final Class<? extends AjaxAction> actionType : allActionTypes()) {
                for (final String transport : new String[]{"query", "path"}) {
                    assertNoPasswordFallback(actionType, parsedCredentials(transport, "u", "p"), null);
                }
            }
            for (final Class<? extends AjaxAction> actionType : aliasActionTypes()) {
                assertNoPasswordFallback(actionType, parsedCredentials("query", "user", "passwd"), null);
                assertNoPasswordFallback(actionType, parsedCredentials("path", "user", "passwd"), null);
            }
            for (final Class<? extends AjaxAction> actionType : portletGatedActionTypes()) {
                assertNoPasswordFallback(actionType, credentials("u", "p"), mockUser());
            }
        }
    }

    /** ALL with an explicit URL opt-in must keep both query and path aliases for every action. */
    @Test
    public void allModeRetainsLegacyPasswordFallback() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");

        for (final Class<? extends AjaxAction> actionType : allActionTypes()) {
            assertPasswordFallbackUsed(actionType, parsedCredentials("query", "u", "p"), null);
            assertPasswordFallbackUsed(actionType, parsedCredentials("path", "u", "p"), null);
        }
    }

    /** Legacy aliases must reach shared authentication before strict anonymous initialization rejects them. */
    @Test
    public void initializationAuthenticatesPathAndQueryAliasesBeforeTheStrictUserCheck() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        for (final String[] keys : new String[][]{{"u", "p"}, {"user", "passwd"}}) {
            for (final String transport : new String[]{"query", "path"}) {
                final Map<String, String> params = credentials(keys[0], keys[1]);
                final HttpServletRequest request = mockRequest();
                final HttpServletResponse response = mock(HttpServletResponse.class);
                final String path = "/DotAjaxDirector/example.Action/cmd/action"
                        + ("path".equals(transport) ? "/" + keys[0] + "/" + params.get(keys[0])
                                + "/" + keys[1] + "/" + params.get(keys[1]) : "");
                when(request.getRequestURI()).thenReturn(path);
                when(request.getParameterNames()).thenReturn(java.util.Collections.enumeration(
                        "query".equals(transport) ? params.keySet() : java.util.Collections.emptySet()));
                params.forEach((key, value) -> when(request.getParameter(key)).thenReturn(value));
                final WebResource webResource = mock(WebResource.class);
                final InitDataObject authenticated = new InitDataObject();
                final User user = mockUser();
                authenticated.setUser(user);
                when(webResource.init(path, true, request, true, null))
                        .thenThrow(new SecurityException("Invalid User", Response.Status.UNAUTHORIZED));
                when(webResource.initWithMap(any(), any(), any(), any(), any())).thenReturn(authenticated);
                final AjaxAction action = mock(AjaxAction.class, CALLS_REAL_METHODS);
                action.setWebResource(webResource);

                action.init(request, response);

                assertSame(user, action.getUser());
                verify(webResource).initWithMap(argThat(map -> params.get(keys[0]).equals(map.get("user"))
                        && params.get(keys[1]).equals(map.get("password"))), eq(request), eq(response),
                        eq(AnonymousAccess.NONE), isNull());
            }
        }
    }

    private void assertNoPasswordFallback(final Class<? extends AjaxAction> actionType,
            final Map<String, String> params, final User user) throws Exception {
        final HttpServletRequest request = mockRequest();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AjaxAction action = preparedAction(actionType, params, user);
        final LayoutAPI layoutAPI = layoutApiWithoutPortletAccess();

        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                MockedStatic<LoginFactory> loginFactory = mockStatic(LoginFactory.class)) {
            apis.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
            action.service(request, response);
            loginFactory.verify(
                    () -> LoginFactory.doLogin(any(), any(), anyBoolean(), any(), any()), never());
        }

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private void assertPasswordFallbackUsed(final Class<? extends AjaxAction> actionType,
            final Map<String, String> params, final User user) throws Exception {
        final HttpServletRequest request = mockRequest();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AjaxAction action = preparedAction(actionType, params, user);
        final LayoutAPI layoutAPI = layoutApiWithoutPortletAccess();

        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                MockedStatic<LoginFactory> loginFactory = mockStatic(LoginFactory.class)) {
            apis.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
            action.service(request, response);
            loginFactory.verify(
                    () -> LoginFactory.doLogin(any(), any(), anyBoolean(), any(), any()), atLeastOnce());
        }
    }

    private static HttpServletRequest mockRequest() {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(null);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        return request;
    }

    private static AjaxAction preparedAction(final Class<? extends AjaxAction> actionType,
            final Map<String, String> params, final User user) {
        final AjaxAction action = mock(actionType, CALLS_REAL_METHODS);
        when(action.getURIParams()).thenReturn(params);
        when(action.getUser()).thenReturn(user);
        doNothing().when(action).setUser(any(HttpServletRequest.class));
        return action;
    }

    private static LayoutAPI layoutApiWithoutPortletAccess() throws Exception {
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), any())).thenReturn(false);
        return layoutAPI;
    }

    private static Map<String, String> credentials(final String userKey, final String passwordKey) {
        final Map<String, String> params = new HashMap<>();
        params.put("cmd", "action");
        params.put(userKey, "test-user@example.com");
        params.put(passwordKey, "a-password");
        return params;
    }

    /** Uses the real shared parser so the gate covers query strings and alternating path segments. */
    private static Map<String, String> parsedCredentials(final String transport,
            final String userKey, final String passwordKey) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Map<String, String> params = credentials(userKey, passwordKey);
        when(request.getRequestURI()).thenReturn("path".equals(transport)
                ? "/cmd/action/" + userKey + "/" + params.get(userKey)
                        + "/" + passwordKey + "/" + params.get(passwordKey)
                : "/cmd/action");
        when(request.getParameterNames()).thenReturn(java.util.Collections.enumeration(
                "query".equals(transport) ? params.keySet() : java.util.Collections.emptySet()));
        params.forEach((key, value) -> when(request.getParameter(key)).thenReturn(value));
        final AjaxAction parser = mock(AjaxAction.class, CALLS_REAL_METHODS);
        parser.setURIParams(request);
        return parser.getURIParams();
    }

    private static User mockUser() {
        return mock(User.class);
    }

    private static List<Class<? extends AjaxAction>> allActionTypes() {
        return List.of(IndexAjaxAction.class, SiteSearchAjaxAction.class,
                PublishingEndpointAjaxAction.class, EnvironmentAjaxAction.class,
                RemotePublishAjaxAction.class);
    }

    private static List<Class<? extends AjaxAction>> portletGatedActionTypes() {
        return List.of(IndexAjaxAction.class, SiteSearchAjaxAction.class,
                PublishingEndpointAjaxAction.class, EnvironmentAjaxAction.class);
    }

    private static List<Class<? extends AjaxAction>> aliasActionTypes() {
        return List.of(PublishingEndpointAjaxAction.class, EnvironmentAjaxAction.class,
                RemotePublishAjaxAction.class);
    }
}
