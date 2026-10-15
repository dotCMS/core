package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.adminsite.AdminSiteAPI;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that cached READ decisions for unpublished content do not cross the
 * admin-site boundary for a backend user who is not an administrator.
 * Uses real users, content, permission evaluation and the short-lived cache;
 * only the HTTP requests are mocked.
 */
public class AdminSitePermissionCacheTest extends IntegrationTestBase {

    private static final String ADMIN_HOST = "admin.permission-cache.example";
    private static final String PUBLIC_HOST = "public.permission-cache.example";
    private static final String CACHE_SIZE = "cache.permissionshortlived.size";

    private final Map<String, String> originalConfig = new LinkedHashMap<>();
    private HttpServletRequest originalRequest;
    private PermissionAPI permissionAPI;
    private ContentType contentType;
    private Contentlet draft;
    private User backendUser;

    /** Initializes the integration services before creating persisted fixtures. */
    @BeforeClass
    public static void initialize() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Creates an unpublished contentlet and a non-admin backend user with READ
     * permission. Establishes both uncached outcomes before testing cache reuse.
     */
    @Before
    public void setUp() throws Exception {
        originalRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        permissionAPI = APILocator.getPermissionAPI();

        setConfig(AdminSiteAPI.ADMIN_SITE_ENABLED, "true");
        setConfig(AdminSiteAPI.ADMIN_SITE_ALLOW_BACKEND_LOGINS_ANY_SITE, "false");
        setConfig(AdminSiteAPI.ADMIN_SITE_URL, "https://" + ADMIN_HOST);
        setConfig(AdminSiteAPI.ADMIN_SITE_REQUEST_DOMAINS, ADMIN_HOST);
        // Other permission tests disable this region; cache reuse is essential here.
        setConfig(CACHE_SIZE, "1000");
        APILocator.getAdminSiteAPI().invalidateCache();
        CacheLocator.getPermissionCache().flushShortTermCache();

        final Role backendRole = APILocator.getRoleAPI().loadBackEndUserRole();
        backendUser = new UserDataGen().roles(backendRole).nextPersisted();
        contentType = new ContentTypeDataGen().nextPersisted();
        draft = new ContentletDataGen(contentType).nextPersisted();
        assertFalse("Fixture must remain unpublished", draft.isLive());

        permissionAPI.permissionIndividually(draft.getParentPermissionable(), draft,
                APILocator.systemUser());
        final Permission read = new Permission();
        read.setPermission(PermissionAPI.PERMISSION_READ);
        read.setRoleId(backendRole.getId());
        read.setInode(draft.getPermissionId());
        permissionAPI.save(read, draft, APILocator.systemUser(), false);

        useHost(PUBLIC_HOST);
        assertFalse("Public-host request must lose backend eligibility", backendUser.isBackendUser());
        assertFalse("Fixture user must not be an administrator", backendUser.isAdmin());
        CacheLocator.getPermissionCache().flushShortTermCache();
        assertFalse("Uncached public-host READ must be denied", canReadDraft());

        useHost(ADMIN_HOST);
        assertTrue("Admin-host request must retain backend eligibility", backendUser.isBackendUser());
        assertFalse("Admin shortcut must not bypass the cache under test", backendUser.isAdmin());
        CacheLocator.getPermissionCache().flushShortTermCache();
        assertTrue("Uncached admin-host READ must be allowed", canReadDraft());

        CacheLocator.getPermissionCache().flushShortTermCache();
    }

    /**
     * Deletes fixtures and restores configuration, request context and caches,
     * including when a regression assertion or fixture cleanup fails.
     */
    @After
    public void tearDown() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        try {
            if (draft != null) {
                ContentletDataGen.destroy(draft, false);
            }
        } finally {
            try {
                if (backendUser != null) {
                    UserDataGen.remove(backendUser, false);
                }
            } finally {
                try {
                    if (contentType != null) {
                        ContentTypeDataGen.remove(contentType, false);
                    }
                } finally {
                    try {
                        originalConfig.forEach(Config::setProperty);
                        APILocator.getAdminSiteAPI().invalidateCache();
                        CacheLocator.getPermissionCache().flushShortTermCache();
                    } finally {
                        HttpServletRequestThreadLocal.INSTANCE.setRequest(originalRequest);
                    }
                }
            }
        }
    }

    /** An admin-host cached allowance must not expose unpublished content on a public host. */
    @Test
    public void adminHostThenPublicHost_doesNotReuseCachedAllowance() throws Exception {
        useHost(ADMIN_HOST);
        assertTrue("Prime the cache with an allowed admin-host READ", canReadDraft());
        assertCachedDecision(true);

        useHost(PUBLIC_HOST);
        assertFalse("The second request must be backend-restricted", backendUser.isBackendUser());
        // Do not flush between hosts: this is the cache isolation regression.
        assertFalse("Admin-host cached READ must not authorize the public-host request", canReadDraft());
    }

    /** A public-host cached denial must not reject a permitted READ on an admin host. */
    @Test
    public void publicHostThenAdminHost_doesNotReuseCachedDenial() throws Exception {
        useHost(PUBLIC_HOST);
        assertFalse("Prime the cache with a denied public-host READ", canReadDraft());
        assertCachedDecision(false);

        useHost(ADMIN_HOST);
        assertTrue("The second request must be backend-eligible", backendUser.isBackendUser());
        // The user remains non-admin, so the early admin shortcut cannot hide the defect.
        assertFalse(backendUser.isAdmin());
        assertTrue("Public-host cached denial must not reject the admin-host request", canReadDraft());
    }

    /** Changes the request context without changing the user, content or permissions. */
    private void useHost(final String host) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("host")).thenReturn(host);
        when(request.getServerName()).thenReturn(host);
        when(request.getRequestURI()).thenReturn("/public-page");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        assertEquals("Request must have the intended admin-host eligibility",
                ADMIN_HOST.equals(host), APILocator.getAdminSiteAPI().isAdminSite(request));
    }

    /** Checks READ through the public permission API, including its short-lived cache. */
    private boolean canReadDraft() throws Exception {
        return permissionAPI.doesUserHavePermission(draft, PermissionAPI.PERMISSION_READ,
                backendUser, false);
    }

    /** Ensures the test actually primed the cache instead of silently running with it disabled. */
    private void assertCachedDecision(final boolean expected) {
        assertEquals("The first request must have populated the short-lived cache",
                Optional.of(expected), CacheLocator.getPermissionCache().doesUserHavePermission(
                        draft, String.valueOf(PermissionAPI.PERMISSION_READ), backendUser, false, null));
    }

    /** Saves the original effective configuration value before applying a test override. */
    private void setConfig(final String name, final String value) {
        originalConfig.put(name, Config.getStringProperty(name, null));
        Config.setProperty(name, value);
    }
}
