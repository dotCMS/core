package com.dotcms.rest.api.v1.maintenance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dotcms.cache.DynamicTTLCache;
import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.listeners.SessionMonitor;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSession;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.apps.view.SecretView;
import com.dotcms.rest.api.v1.system.cache.CacheResource;
import com.dotcms.rest.api.v1.system.redis.RedisResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for who may use the cache, Redis, JVM information and logged-in session routes of the Maintenance
 * tool, with real roles and layouts: a CMS Administrator is admitted, a back-end user who holds the Maintenance
 * portlet without being a CMS Administrator is not, and neither is a back-end user without the portlet.
 */
public class MaintenanceToolsAccessIntegrationTest {

    private static Layout maintenanceLayout;
    private static Role maintenanceRole;
    private static User maintenanceOnlyUser;
    private static User noLayoutUser;
    private static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        final Role backendRole = TestUserUtils.getBackendRole();
        maintenanceLayout = new LayoutDataGen()
                .portletIds(PortletID.MAINTENANCE.toString().toLowerCase())
                .nextPersisted();
        maintenanceRole = new RoleDataGen().layout(maintenanceLayout).nextPersisted();

        maintenanceOnlyUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(backendRole, maintenanceOnlyUser);
        APILocator.getRoleAPI().addRoleToUser(maintenanceRole, maintenanceOnlyUser);

        noLayoutUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(backendRole, noLayoutUser);

        adminUser = new UserDataGen().roles(APILocator.getRoleAPI().loadCMSAdminRole(), backendRole).nextPersisted();
    }

    /**
     * Removes the users, role and layout created for these tests, newest first, skipping any that is already gone.
     * Deleting a user or a role empties the role cache, so the system roles are loaded back for the classes that run
     * next.
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        removeUser(adminUser);
        removeUser(noLayoutUser);
        removeUser(maintenanceOnlyUser);
        if (null != maintenanceRole) {
            RoleDataGen.remove(maintenanceRole);
        }
        if (null != maintenanceLayout) {
            try {
                final Layout storedLayout = APILocator.getLayoutAPI().findLayout(maintenanceLayout.getId());
                if (null != storedLayout) {
                    APILocator.getLayoutAPI().removeLayout(storedLayout);
                }
            } catch (final Exception e) {
                Logger.error(MaintenanceToolsAccessIntegrationTest.class, "Unable to remove the test layout", e);
            }
        }
        APILocator.getRoleAPI().loadBackEndUserRole();
        APILocator.getRoleAPI().loadFrontEndUserRole();
        APILocator.getRoleAPI().loadCMSAdminRole();
    }

    /**
     * Removes a user created for these tests. Deleting a user looks up the role of the system user, which it reaches
     * only while the {@link Role#SYSTEM} role is in the role cache, and the previous deletion emptied that cache, so the
     * role is loaded back first.
     *
     * @param user the user to remove; {@code null} is ignored
     * @throws DotDataException if the role cannot be loaded
     */
    private static void removeUser(final User user) throws DotDataException {
        if (null != user) {
            APILocator.getRoleAPI().loadRoleByKey(Role.SYSTEM);
            UserDataGen.remove(user);
        }
    }

    /**
     * Method to test: {@link JVMInfoResource#getJvmInfo(HttpServletRequest, HttpServletResponse)}
     * Given scenario: a back-end user whose role has a layout with only the Maintenance portlet, and who is not a CMS
     * Administrator.
     * Expected result: the request is rejected with 401.
     */
    @Test
    public void getJvmInfo_userWithMaintenanceLayoutOnly_isRejected() {
        final SecurityException refusal = assertThrows(SecurityException.class,
                () -> new JVMInfoResource().getJvmInfo(requestFor(maintenanceOnlyUser), new MockHttpResponse()));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
    }

    /**
     * Method to test: {@link CacheResource#showObjects(HttpServletRequest, HttpServletResponse, String, String)}
     * Given scenario: a back-end user whose role has a layout with only the Maintenance portlet, and who is not a CMS
     * Administrator.
     * Expected result: the request is rejected with 401.
     */
    @Test
    public void showObjects_userWithMaintenanceLayoutOnly_isRejected() {
        final SecurityException refusal = assertThrows(SecurityException.class,
                () -> new CacheResource().showObjects(
                        requestFor(maintenanceOnlyUser), new MockHttpResponse(), "provider", "group"));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
    }

    /**
     * Method to test: {@link RedisResource#get(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: a back-end user whose role has a layout with only the Maintenance portlet, and who is not a CMS
     * Administrator.
     * Expected result: the request is rejected with 401 and the Redis client is never called.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void redisGet_userWithMaintenanceLayoutOnly_isRejected() {
        final RedisClient<String, Object> client = mock(RedisClient.class);
        final RedisResource redisResource = new RedisResource(new WebResource(), client);

        final SecurityException refusal = assertThrows(SecurityException.class,
                () -> redisResource.get(requestFor(maintenanceOnlyUser), new MockHttpResponse(), "key"));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
        verifyNoInteractions(client);
    }

    /**
     * Method to test: {@link JVMInfoResource#getJvmInfo(HttpServletRequest, HttpServletResponse)}
     * Given scenario: a CMS Administrator.
     * Expected result: 200 with the release, host, JVM, environment, configuration overrides and system sections.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void getJvmInfo_cmsAdministrator_returnsSystemInformation() throws Exception {
        final Response result = new JVMInfoResource().getJvmInfo(requestFor(adminUser), new MockHttpResponse());

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        final Map<String, Object> body = (Map<String, Object>) result.getEntity();
        for (final String section : new String[]{"release", "host", "jvm", "environment", "configOverrides",
                "system"}) {
            assertTrue("Missing section " + section, body.containsKey(section));
        }
    }

    /**
     * Method to test: {@link JVMInfoResource#getJvmInfo(HttpServletRequest, HttpServletResponse)}
     * Given scenario: a back-end user without the Maintenance portlet in any of its layouts.
     * Expected result: the request is rejected with 401.
     */
    @Test
    public void getJvmInfo_userWithoutMaintenanceLayout_isRejected() {
        final SecurityException refusal = assertThrows(SecurityException.class,
                () -> new JVMInfoResource().getJvmInfo(requestFor(noLayoutUser), new MockHttpResponse()));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
    }

    /**
     * Method to test: {@link CacheResource#showObjects(HttpServletRequest, HttpServletResponse, String, String)}
     * Given scenario: a CMS Administrator lists the user cache after a user was loaded into it.
     * Expected result: the cached user is listed, and no listed user carries its password, password-reset token or
     * remember-me token.
     */
    @Test
    public void showObjects_userCache_listsUsersWithoutCredentials() throws Exception {
        APILocator.getUserAPI().loadUserById(maintenanceOnlyUser.getUserId(), APILocator.systemUser(), false);
        // The cache administrator stores group names in lower case, and the routes take the stored name
        final String group = CacheLocator.getUserCache().getPrimaryGroup().toLowerCase();
        final CacheResource cacheResource = new CacheResource();

        final Map<String, Object> entries = entriesOf(cacheResource.showObjects(
                requestFor(adminUser), new MockHttpResponse(), providerNameFor(cacheResource, group), group));

        boolean userListed = false;
        for (final Object value : entries.values()) {
            if (value instanceof User) {
                final User listed = (User) value;
                assertNull("No password is returned", listed.getPassword());
                assertNull("No password-reset token is returned", listed.getIcqId());
                assertNull("No remember-me token is returned", listed.getRememberMeToken());
                userListed |= maintenanceOnlyUser.getUserId().equals(listed.getUserId());
            }
        }
        assertTrue("The loaded user is listed", userListed);
    }

    /**
     * Method to test: {@link CacheResource#showObject(HttpServletRequest, HttpServletResponse, String, String, String)}
     * and {@link CacheResource#showObjects(HttpServletRequest, HttpServletResponse, String, String)}
     * Given scenario: a CMS Administrator reads the Apps secrets cache group after a secret was cached.
     * Expected result: the hidden-value mask instead of the secret, for one entry and for the whole group.
     */
    @Test
    public void cacheObjectRoutes_appsSecretsGroup_returnTheMask() throws Exception {
        final String key = "maintenance-tools-test-key-" + UUID.randomUUID();
        CacheLocator.getAppsCache().putSecret(key, "maintenance-tools-test-value".toCharArray());
        try {
            final String group = CacheLocator.getAppsCache().getPrimaryGroup().toLowerCase();
            final CacheResource cacheResource = new CacheResource();
            final String provider = providerNameFor(cacheResource, group);

            final Response single = cacheResource.showObject(
                    requestFor(adminUser), new MockHttpResponse(), provider, group, key);
            assertEquals(SecretView.SecretViewSerializer.HIDDEN_SECRET_MASK,
                    ((ResponseEntityView<?>) single.getEntity()).getEntity());

            final Map<String, Object> entries = entriesOf(cacheResource.showObjects(
                    requestFor(adminUser), new MockHttpResponse(), provider, group));
            assertTrue("The cached secret is listed", entries.containsKey(key));
            for (final Object value : entries.values()) {
                assertEquals(SecretView.SecretViewSerializer.HIDDEN_SECRET_MASK, value);
            }
        } finally {
            CacheLocator.getAppsCache().flushSecret(key);
        }
    }

    /**
     * Method to test: {@link MaintenanceResource#listSessions(HttpServletRequest, HttpServletResponse)}
     * Given scenario: a back-end user whose role has a layout with only the Maintenance portlet, and who is not a CMS
     * Administrator.
     * Expected result: the request is rejected with 401, and no session-list secret is stored on the caller's session.
     */
    @Test
    public void listSessions_userWithMaintenanceLayoutOnly_isRejected() {
        final HttpServletRequest request = requestFor(maintenanceOnlyUser);

        final SecurityException refusal = assertThrows(SecurityException.class,
                () -> new MaintenanceResource().listSessions(request, new MockHttpResponse()));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
        assertNull(request.getSession().getAttribute(MaintenanceResource.CSRF_TOKEN_ATTRIBUTE));
    }

    /**
     * Method to test: {@link MaintenanceResource#killSession(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: a back-end user whose role has a layout with only the Maintenance portlet, and who is not a CMS
     * Administrator, sends a valid token for another active session, with a fresh session-list secret on its own
     * session.
     * Expected result: the request is rejected with 401 and the other session is still active.
     */
    @Test
    public void killSession_userWithMaintenanceLayoutOnly_isRejected() throws Exception {
        final HttpServletRequest request = requestFor(maintenanceOnlyUser);
        final String secret = UUID.randomUUID().toString();
        request.getSession().setAttribute(MaintenanceResource.CSRF_TOKEN_ATTRIBUTE, secret);
        request.getSession().setAttribute(MaintenanceResource.CSRF_TOKEN_TIMESTAMP_ATTRIBUTE, Instant.now());
        final HttpSession target = newActiveSession();
        final String targetId = target.getId();
        trackSession(target);
        try {
            final String token = SessionTokenUtil.obfuscateSessionId(targetId, secret);

            final SecurityException refusal = assertThrows(SecurityException.class,
                    () -> new MaintenanceResource().killSession(request, new MockHttpResponse(), token));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
            assertEquals("The other session is still active", noLayoutUser.getUserId(),
                    target.getAttribute(WebKeys.USER_ID));
        } finally {
            untrackSession(targetId);
        }
    }

    /**
     * Method to test: {@link MaintenanceResource#killAllSessions(HttpServletRequest, HttpServletResponse)}
     * Given scenario: a back-end user whose role has a layout with only the Maintenance portlet, and who is not a CMS
     * Administrator, while another session is active.
     * Expected result: the request is rejected with 401 and the other session is still active.
     */
    @Test
    public void killAllSessions_userWithMaintenanceLayoutOnly_isRejected() throws Exception {
        final HttpSession target = newActiveSession();
        final String targetId = target.getId();
        trackSession(target);
        try {
            final SecurityException refusal = assertThrows(SecurityException.class,
                    () -> new MaintenanceResource().killAllSessions(
                            requestFor(maintenanceOnlyUser), new MockHttpResponse()));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), refusal.getResponse().getStatus());
            assertEquals("The other session is still active", noLayoutUser.getUserId(),
                    target.getAttribute(WebKeys.USER_ID));
        } finally {
            untrackSession(targetId);
        }
    }

    /**
     * Returns a new session, with its own id, that belongs to the back-end user without the Maintenance layout.
     *
     * @return the session
     */
    private static HttpSession newActiveSession() {
        final HttpSession session = new MockSession(UUID.randomUUID().toString());
        session.setAttribute(WebKeys.USER_ID, noLayoutUser.getUserId());
        session.setAttribute(SessionMonitor.USER_REMOTE_ADDR, "127.0.0.1");
        return session;
    }

    /**
     * Registers a session in the {@link SessionMonitor} list of active sessions, which the session routes read.
     *
     * @param session the session to register
     * @throws Exception if the list cannot be reached
     */
    private static void trackSession(final HttpSession session) throws Exception {
        activeSessions().put(session.getId(), session);
    }

    /**
     * Removes a session registered by {@link #trackSession(HttpSession)} from the {@link SessionMonitor} list.
     *
     * @param sessionId the id the session had when it was registered
     * @throws Exception if the list cannot be reached
     */
    private static void untrackSession(final String sessionId) throws Exception {
        activeSessions().invalidate(sessionId);
    }

    /**
     * Returns the {@link SessionMonitor} list of active sessions. It has no public way to add a session, so the
     * field is read by reflection, as {@code MaintenanceResourceIntegrationTest} does.
     *
     * @return the list of active sessions, by session id
     * @throws Exception if the field cannot be read
     */
    @SuppressWarnings("unchecked")
    private static DynamicTTLCache<String, HttpSession> activeSessions() throws Exception {
        final Field field = SessionMonitor.class.getDeclaredField("userSessions");
        field.setAccessible(true);
        return (DynamicTTLCache<String, HttpSession>) field.get(null);
    }

    /**
     * Returns the name of the first cache provider that serves the given group, as the cache routes expect it.
     *
     * @param cacheResource the resource to ask
     * @param group         the cache group
     * @return the provider name
     */
    @SuppressWarnings("unchecked")
    private static String providerNameFor(final CacheResource cacheResource, final String group) {
        final Response providers = cacheResource.showProviders(requestFor(adminUser), new MockHttpResponse(), group);
        return ((List<CacheProvider>) ((ResponseEntityView<?>) providers.getEntity()).getEntity()).get(0).getName();
    }

    /**
     * Returns the entries of a cache listing response.
     *
     * @param response the response of a listing route
     * @return the listed entries, by cache key
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> entriesOf(final Response response) {
        return (Map<String, Object>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
    }

    /**
     * Builds a request that carries the given user as the logged-in user.
     *
     * @param user the {@link User} making the request
     * @return the request
     */
    private static HttpServletRequest requestFor(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/api/v1/maintenance")
                                        .request())
                                .request())
                        .request());
        request.setAttribute(WebKeys.USER, user);
        return request;
    }
}
