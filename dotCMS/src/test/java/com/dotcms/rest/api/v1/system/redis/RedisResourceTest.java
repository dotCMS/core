package com.dotcms.rest.api.v1.system.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.dotcms.rest.api.v1.apps.view.SecretView;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.security.apps.AppsCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.Map;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Unit tests for the access rules of {@link RedisResource} and for the values its read routes return. The access
 * tests run the real {@link WebResource} checks: the caller holds the back-end role and the Maintenance portlet, and
 * the only thing that changes is whether it is a CMS Administrator.
 */
class RedisResourceTest {

    private RedisClient<String, Object> client;
    private RedisResource resource;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;
    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<CacheLocator> cacheLocator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKeyOrId(anyString())).thenReturn(mock(Role.class));
        when(roleAPI.doesUserHaveRole(any(User.class), any(Role.class))).thenReturn(true);
        // A user written as JSON reads its own role through the locator
        when(roleAPI.loadRoleByKey(anyString())).thenReturn(new Role());
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
        // The read routes look up the Apps secrets cache group, which a unit test does not have
        cacheLocator = mockStatic(CacheLocator.class);
        final AppsCache appsCache = mock(AppsCache.class);
        when(appsCache.getPrimaryGroup()).thenReturn("SECRETS_CACHE_GROUP");
        cacheLocator.when(CacheLocator::getAppsCache).thenReturn(appsCache);

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
        cacheLocator.close();
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

    /**
     * Method to test: {@link RedisResource#get}.
     * Given scenario: a CMS Administrator reads a key that the cache wrote for the Apps secrets group, with the
     * group name in lower or upper case.
     * Expected result: 200, with the hidden-value mask instead of the stored value.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"PREFIX_KEY.secrets_cache_group.app-key.", "PREFIX_KEY.SECRETS_CACHE_GROUP.app-key."})
    void get_secretsGroupKey_isMasked(final String key) {
        when(user.isAdmin()).thenReturn(true);
        when(client.get(key)).thenReturn("app-secret-value".toCharArray());

        final Response result = resource.get(request, response, key);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(SecretView.SecretViewSerializer.HIDDEN_SECRET_MASK,
                ((ResponseEntityView<?>) result.getEntity()).getEntity());
    }

    /**
     * Method to test: {@link RedisResource#get}.
     * Given scenario: a CMS Administrator reads a key of the Apps key-name group, and a key of a group whose name
     * only starts like the secrets group.
     * Expected result: 200, with the stored value as it is.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"PREFIX_KEY.keys_cache_group.app-key.", "PREFIX_KEY.secrets_cache_group_copy.app-key."})
    void get_otherGroupKey_isNotMasked(final String key) {
        when(user.isAdmin()).thenReturn(true);
        when(client.get(key)).thenReturn("app-key-name");

        final Response result = resource.get(request, response, key);

        assertEquals("app-key-name", ((ResponseEntityView<?>) result.getEntity()).getEntity());
    }

    /**
     * Method to test: {@link RedisResource#getHash}.
     * Given scenario: a CMS Administrator reads a hash stored under a key of the Apps secrets group.
     * Expected result: 200, with the hidden-value mask instead of the stored hash.
     */
    @Test
    void getHash_secretsGroupKey_isMasked() {
        when(user.isAdmin()).thenReturn(true);
        when(client.getHash("PREFIX_KEY.secrets_cache_group.app-key."))
                .thenReturn(Map.of("value", "app-secret-value"));

        final Response result = resource.getHash(request, response, "PREFIX_KEY.secrets_cache_group.app-key.");

        assertEquals(SecretView.SecretViewSerializer.HIDDEN_SECRET_MASK,
                ((ResponseEntityView<?>) result.getEntity()).getEntity());
    }

    /**
     * Method to test: {@link RedisResource#get}.
     * Given scenario: a CMS Administrator reads a key that holds a {@link User} with a stored password, a
     * password-reset token and a remember-me token.
     * Expected result: 200, with the user as JSON keeping its id and without the three values.
     */
    @Test
    void get_storedUser_hasNoCredentials() {
        when(user.isAdmin()).thenReturn(true);
        when(client.get("PREFIX_KEY.userdotcmscache.stored-user.")).thenReturn(userWithCredentials("stored-user"));

        final Response result = resource.get(request, response, "PREFIX_KEY.userdotcmscache.stored-user.");

        final JsonNode entity = assertInstanceOf(JsonNode.class,
                ((ResponseEntityView<?>) result.getEntity()).getEntity());
        assertEquals("stored-user", entity.get("userId").asText());
        assertHasNoCredentials(entity);
    }

    /**
     * Method to test: {@link RedisResource#getHash}.
     * Given scenario: a CMS Administrator reads a hash with a {@link User} field and a plain text field.
     * Expected result: 200, the user field without its credentials and the text field as stored.
     */
    @Test
    void getHash_storedUser_hasNoCredentials() {
        when(user.isAdmin()).thenReturn(true);
        when(client.getHash("h")).thenReturn(Map.of("owner", userWithCredentials("hash-user"), "label", "plain"));

        final Response result = resource.getHash(request, response, "h");

        final JsonNode entity = assertInstanceOf(JsonNode.class,
                ((ResponseEntityView<?>) result.getEntity()).getEntity());
        assertEquals("hash-user", entity.get("owner").get("userId").asText());
        assertHasNoCredentials(entity.get("owner"));
        assertEquals("plain", entity.get("label").asText());
    }

    /**
     * Builds a user that holds a stored password, a password-reset token and a remember-me token.
     *
     * @param userId the user id; the email is derived from it
     * @return the user
     */
    private static User userWithCredentials(final String userId) {
        final User stored = new User();
        stored.setUserId(userId);
        stored.setCompanyId("dotcms.org");
        stored.setEmailAddress(userId + "@dotcms.local");
        stored.setLanguageId("en_US");
        stored.setTimeZoneId("UTC");
        stored.setPassword("stored-password-value");
        stored.setIcqId("reset-token-value");
        stored.setRememberMeToken("remember-me-value");
        return stored;
    }

    /**
     * Asserts that a user written as JSON carries no stored password, password-reset token ({@code icqId}) or
     * remember-me token ({@code skinId}).
     *
     * @param json the user as JSON
     */
    private static void assertHasNoCredentials(final JsonNode json) {
        assertNotNull(json);
        assertFalse(json.has("password"));
        assertFalse(json.has("icqId"));
        assertFalse(json.has("skinId"));
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
