package com.dotcms.rest.api.v1.system.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

/**
 * Unit tests for the access rules of {@link RedisResource}. Each test runs the real {@link WebResource} checks: the
 * caller holds the back-end role and the Maintenance portlet, and the only thing that changes is whether it is a CMS
 * Administrator.
 */
class RedisResourceTest {

    private RedisClient<String, Object> client;
    private RedisResource resource;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;
    private MockedStatic<APILocator> apiLocator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKeyOrId(anyString())).thenReturn(mock(Role.class));
        when(roleAPI.doesUserHaveRole(any(User.class), any(Role.class))).thenReturn(true);
        apiLocator = mockStatic(APILocator.class);

        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), any(User.class))).thenReturn(true);
        final ApiProvider apiProvider = mock(ApiProvider.class);
        when(apiProvider.layoutAPI()).thenReturn(layoutAPI);
        when(apiProvider.roleAPI()).thenReturn(roleAPI);
        final WebResource webResource =
                new WebResource(apiProvider, mock(JsonWebTokenAuthCredentialProcessor.class));

        client = mock(RedisClient.class);
        resource = new RedisResource(webResource, client);

        user = mock(User.class);
        when(user.getUserId()).thenReturn("maintenance-user");
        when(user.getFullName()).thenReturn("Maintenance User");
        when(user.getEmailAddress()).thenReturn("maintenance.user@dotcms.local");
        request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        response = mock(HttpServletResponse.class);
    }

    @AfterEach
    void tearDown() {
        apiLocator.close();
    }

    /**
     * One call per route of {@link RedisResource}, with route arguments that do not matter because the access check
     * runs first.
     */
    static Stream<Arguments> everyRoute() {
        return Stream.of(
                route("ping", (r, req, res) -> r.ping(req, res)),
                route("echo", (r, req, res) -> r.echo(req, res, "m")),
                route("set", (r, req, res) -> r.set(req, res, mock(SetForm.class))),
                route("get", (r, req, res) -> r.get(req, res, "k")),
                route("delete", (r, req, res) -> r.delete(req, res, "k")),
                route("setHash", (r, req, res) -> r.setHash(req, res, mock(SetHashForm.class))),
                route("getHash", (r, req, res) -> r.getHash(req, res, "k")),
                route("deleteHash", (r, req, res) -> r.deleteHash(req, res, "k")),
                route("incrementAsync", (r, req, res) -> r.incrementAsync(req, res, mock(AsyncResponse.class), "k")),
                route("getIncrement", (r, req, res) -> r.getIncrement(req, res, "k")));
    }

    /**
     * Method to test: every route of {@link RedisResource}.
     * Given scenario: the caller holds the back-end role and the Maintenance portlet, but is not a CMS Administrator.
     * Expected result: a {@code SecurityException} with status 401, and no Redis client call.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("everyRoute")
    void everyRoute_callerWithoutAdministratorRole_isRejected(final String routeName, final RouteCall call) {
        when(user.isAdmin()).thenReturn(false);

        final SecurityException refusal = assertThrows(SecurityException.class,
                () -> call.invoke(resource, request, response),
                routeName + " must require a CMS Administrator");

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
        verifyNoInteractions(client);
    }

    /**
     * Method to test: {@link RedisResource#get}.
     * Given scenario: the caller is a CMS Administrator holding the back-end role and the Maintenance portlet.
     * Expected result: 200, with the value the Redis client returned for the key.
     */
    @Test
    void get_cmsAdministrator_returnsTheStoredValue() {
        when(user.isAdmin()).thenReturn(true);
        when(client.get("k")).thenReturn("v");

        final Response result = resource.get(request, response, "k");

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals("v", ((ResponseEntityView<?>) result.getEntity()).getEntity());
    }

    private static Arguments route(final String name, final RouteCall call) {
        return Arguments.of(name, call);
    }

    /**
     * Calls one route of a {@link RedisResource} with the given request and response.
     */
    @FunctionalInterface
    interface RouteCall {

        /**
         * Invokes the route.
         *
         * @param resource the resource under test
         * @param request  the request that carries the caller
         * @param response the response
         * @throws Exception whatever the route throws
         */
        void invoke(RedisResource resource, HttpServletRequest request, HttpServletResponse response)
                throws Exception;
    }
}
