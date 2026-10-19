package com.dotmarketing.servlets.ajax;

import static org.mockito.ArgumentMatchers.any;
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
 * keeps the legacy fallback.
 *
 * <p>Limitation: {@link AjaxAction#setUser(HttpServletRequest)} is stubbed, so these tests prove
 * that the submitted credentials never reach the password login factory; they do not exercise the
 * WebResource Basic/JWT path that runs afterwards.</p>
 */
public class AjaxPasswordAuthenticationTest extends UnitTestBase {

    private String previousMode;

    /** Preserves the mode property so an assertion failure cannot leak configuration. */
    @Before
    public void saveMode() {
        previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
    }

    /** Restores the mode property after each test. */
    @After
    public void restoreMode() {
        Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
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

    /** {@code ALL} must keep the legacy u/p fallback for every action. */
    @Test
    public void allModeRetainsLegacyPasswordFallback() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");

        for (final Class<? extends AjaxAction> actionType : allActionTypes()) {
            assertPasswordFallbackUsed(actionType, credentials("u", "p"), null);
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
