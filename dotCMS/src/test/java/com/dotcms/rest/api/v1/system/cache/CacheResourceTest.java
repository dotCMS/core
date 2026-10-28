package com.dotcms.rest.api.v1.system.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.enterprise.cache.provider.CacheProviderAPIImpl;
import com.dotcms.enterprise.cluster.ClusterFactory;
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
import com.dotmarketing.util.MaintenanceUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
 * Unit tests for the access rules of {@link CacheResource}. Each test runs the real {@link WebResource} checks: the
 * caller holds the back-end role and the Maintenance portlet, and the only thing that changes is whether it is a CMS
 * Administrator.
 */
class CacheResourceTest {

    private CacheProviderAPIImpl providerAPI;
    private CacheMaintenanceHelper helper;
    private CacheResource resource;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;
    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<MaintenanceUtil> maintenanceUtil;
    private MockedStatic<CacheLocator> cacheLocator;
    private MockedStatic<ClusterFactory> clusterFactory;

    @BeforeEach
    void setUp() throws Exception {
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKeyOrId(anyString())).thenReturn(mock(Role.class));
        when(roleAPI.doesUserHaveRole(any(User.class), any(Role.class))).thenReturn(true);
        apiLocator = mockStatic(APILocator.class);
        // The route bodies read the cache and cluster infrastructure, which a unit test does not have
        maintenanceUtil = mockStatic(MaintenanceUtil.class);
        cacheLocator = mockStatic(CacheLocator.class);
        final AppsCache appsCache = mock(AppsCache.class);
        when(appsCache.getPrimaryGroup()).thenReturn("SECRETS_CACHE_GROUP");
        cacheLocator.when(CacheLocator::getAppsCache).thenReturn(appsCache);
        clusterFactory = mockStatic(ClusterFactory.class);

        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), any(User.class))).thenReturn(true);
        final ApiProvider apiProvider = mock(ApiProvider.class);
        when(apiProvider.layoutAPI()).thenReturn(layoutAPI);
        when(apiProvider.roleAPI()).thenReturn(roleAPI);
        final WebResource webResource =
                new WebResource(apiProvider, mock(JsonWebTokenAuthCredentialProcessor.class));

        providerAPI = mock(CacheProviderAPIImpl.class);
        helper = mock(CacheMaintenanceHelper.class);
        resource = new CacheResource(webResource, providerAPI, helper);

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
        clusterFactory.close();
        cacheLocator.close();
        maintenanceUtil.close();
        apiLocator.close();
    }

    /**
     * One call per route of {@link CacheResource}, with route arguments that do not matter because the access check
     * runs first.
     */
    static Stream<Arguments> everyRoute() {
        return Stream.of(
                route("showProviders by group", (r, req, res) -> r.showProviders(req, res, "g")),
                route("showProviders by provider and group", (r, req, res) -> r.showProviders(req, res, "p", "g")),
                route("getKeys", (r, req, res) -> r.getKeys(req, res, "p", "g")),
                route("showObject", (r, req, res) -> r.showObject(req, res, "p", "g", "id")),
                route("showObjects", (r, req, res) -> r.showObjects(req, res, "p", "g")),
                route("flushGroup", (r, req, res) -> r.flushGroup(req, res, "p", "g")),
                route("flushObject", (r, req, res) -> r.flushObject(req, res, "p", "g", "id")),
                route("flushAll", (r, req, res) -> r.flushAll(req, res, "p")),
                route("listRegions", (r, req, res) -> r.listRegions(req, res)),
                route("getCacheStats", (r, req, res) -> r.getCacheStats(req, res)),
                route("flushRegion", (r, req, res) -> r.flushRegion(req, res, "Permission")),
                route("deleteMenuCache", (r, req, res) -> r.deleteMenuCache(req, res)));
    }

    /**
     * Method to test: every route of {@link CacheResource}.
     * Given scenario: the caller holds the back-end role and the Maintenance portlet, but is not a CMS Administrator.
     * Expected result: a {@code SecurityException} with status 401, and no cache provider, maintenance helper or menu
     * cache call.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("everyRoute")
    void everyRoute_callerWithoutAdministratorRole_isRejected(final String routeName, final RouteCall call) {
        when(user.isAdmin()).thenReturn(false);

        final SecurityException refusal = assertThrows(SecurityException.class,
                () -> call.invoke(resource, request, response),
                routeName + " must require a CMS Administrator");

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
        verifyNoInteractions(providerAPI, helper);
        maintenanceUtil.verifyNoInteractions();
    }

    /**
     * Method to test: {@link CacheResource#flushAll}.
     * Given scenario: the caller is a CMS Administrator holding the back-end role and the Maintenance portlet.
     * Expected result: 200, and the maintenance helper flushes all caches once.
     */
    @Test
    void flushAll_cmsAdministrator_flushesAllCaches() {
        when(user.isAdmin()).thenReturn(true);

        final Response result = resource.flushAll(request, response, "p");

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        verify(helper, times(1)).flushAllCaches();
    }

    /**
     * Method to test: {@link CacheResource#responseValue(String, Object)}.
     * Given scenario: a value of the Apps secrets cache group, with the group name in upper or lower case.
     * Expected result: the hidden-value mask instead of the secret.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"SECRETS_CACHE_GROUP", "secrets_cache_group"})
    void responseValue_secretsGroup_isMasked(final String group) {
        final Object result = CacheResource.responseValue(group, "app-secret-value".toCharArray());

        assertEquals(SecretView.SecretViewSerializer.HIDDEN_SECRET_MASK, result);
    }

    /**
     * Method to test: {@link CacheResource#responseValue(String, Object)}.
     * Given scenario: a cached {@link User} that has a stored password, a password-reset token and a remember-me
     * token.
     * Expected result: a different user object with the same id and email and none of the three values, and the
     * cached user keeps all three.
     */
    @Test
    void responseValue_user_hasNoCredentialsAndLeavesTheCachedUserIntact() {
        final User cachedUser = new User();
        cachedUser.setUserId("cached-user");
        cachedUser.setCompanyId("dotcms.org");
        cachedUser.setEmailAddress("cached.user@dotcms.local");
        cachedUser.setLanguageId("en_US");
        cachedUser.setTimeZoneId("UTC");
        cachedUser.setPassword("stored-password-value");
        cachedUser.setIcqId("reset-token-value");
        cachedUser.setRememberMeToken("remember-me-value");

        final Object result = CacheResource.responseValue("userdotcmscache", cachedUser);

        final User returned = assertInstanceOf(User.class, result);
        assertNotSame(cachedUser, returned);
        assertEquals("cached-user", returned.getUserId());
        assertEquals("cached.user@dotcms.local", returned.getEmailAddress());
        assertNull(returned.getPassword());
        assertNull(returned.getIcqId());
        assertNull(returned.getRememberMeToken());
        assertEquals("stored-password-value", cachedUser.getPassword());
        assertEquals("reset-token-value", cachedUser.getIcqId());
        assertEquals("remember-me-value", cachedUser.getRememberMeToken());
    }

    /**
     * Method to test: {@link CacheResource#responseValue(String, Object)}.
     * Given scenario: a value that is neither a user nor in the secrets group.
     * Expected result: the same value.
     */
    @Test
    void responseValue_otherValue_isUnchanged() {
        final String value = "cached-value";

        assertSame(value, CacheResource.responseValue("contentletcache", value));
    }

    /**
     * Method to test: {@link CacheResource#responseValue(String, Object)}.
     * Given scenario: no cached value.
     * Expected result: {@code "NOPE"}, as before.
     */
    @Test
    void responseValue_missingValue_isNope() {
        assertEquals("NOPE", CacheResource.responseValue("contentletcache", null));
    }

    private static Arguments route(final String name, final RouteCall call) {
        return Arguments.of(name, call);
    }

    /**
     * Calls one route of a {@link CacheResource} with the given request and response.
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
        void invoke(CacheResource resource, HttpServletRequest request, HttpServletResponse response)
                throws Exception;
    }
}
