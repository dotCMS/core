package com.dotmarketing.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Regression tests for role-revocation propagation through
 * {@link PermissionCache#flushShortTermCache()}.
 *
 * Scenario: dotCMS/private-issues#567 — a user's cached
 * {@code doesUserHavePermission()} decision survives role revocation until a
 * full {@code Flush All}. Root cause was two gaps:
 * <ol>
 *   <li>{@link PermissionCacheImpl#clearCache()} did not flush
 *       {@code shortLivedGroup}.</li>
 *   <li>{@link RoleFactoryImpl#addRoleToUser}/{@code removeRoleFromUser}
 *       invalidated only the role cache, not cached permission decisions.</li>
 * </ol>
 *
 * These tests verify that after both fixes, cache invalidation happens without
 * any manual flush call.
 */
public class RoleRevocationPermissionCacheTest extends IntegrationTestBase {

    private static PermissionAPI permissionAPI;
    private static RoleAPI roleAPI;
    private static User sysuser;
    private static Host site;
    private static int originalShortLivedSize;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();
        roleAPI = APILocator.getRoleAPI();
        sysuser = APILocator.getUserAPI().getSystemUser();

        // The short-term permission cache must be enabled for this regression
        // to be meaningful; PermissionAPITest sets it to 0 to neutralize cache
        // flakes, but here we are specifically validating cache behavior.
        originalShortLivedSize = Config.getIntProperty("cache.permissionshortlived.size", 0);
        Config.setProperty("cache.permissionshortlived.size", 1000);

        site = new SiteDataGen().nextPersisted();
        permissionAPI.permissionIndividually(site.getParentPermissionable(), site, sysuser);

        CacheLocator.getPermissionCache().clearCache();
    }

    @AfterClass
    public static void cleanup() {
        try {
            HibernateUtil.startTransaction();
            APILocator.getHostAPI().archive(site, sysuser, false);
            APILocator.getHostAPI().delete(site, sysuser, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        } finally {
            HibernateUtil.closeSessionSilently();
        }

        Config.setProperty("cache.permissionshortlived.size", originalShortLivedSize);
        CacheLocator.getPermissionCache().clearCache();
    }

    /**
     * Role revocation must invalidate cached permission decisions without
     * requiring a manual cache flush.
     */
    @Test
    public void removeRoleFromUser_invalidatesShortTermPermissionCache() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().nextPersisted();

        roleAPI.addRoleToUser(role, user);

        final Permission p = new Permission();
        p.setPermission(PermissionAPI.PERMISSION_READ);
        p.setRoleId(role.getId());
        p.setInode(site.getIdentifier());
        permissionAPI.save(p, site, sysuser, false);

        // Prime the short-term cache with a TRUE decision for this user.
        assertTrue("Granted role should grant READ on test host",
                permissionAPI.doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, false));

        // Revoke the role. No manual cache flush.
        roleAPI.removeRoleFromUser(role, user);

        assertFalse("Cached TRUE decision must be invalidated by removeRoleFromUser;"
                        + " user no longer has any role granting READ on host",
                permissionAPI.doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, false));
    }

    /**
     * Role grant must also invalidate cached FALSE decisions so newly granted
     * permissions take effect immediately.
     */
    @Test
    public void addRoleToUser_invalidatesShortTermPermissionCache() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().nextPersisted();

        final Permission p = new Permission();
        p.setPermission(PermissionAPI.PERMISSION_READ);
        p.setRoleId(role.getId());
        p.setInode(site.getIdentifier());
        permissionAPI.save(p, site, sysuser, false);

        // Prime the short-term cache with a FALSE decision (user has no role yet).
        assertFalse("User without any granting role should be denied READ",
                permissionAPI.doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, false));

        // Grant the role. No manual cache flush.
        roleAPI.addRoleToUser(role, user);

        assertTrue("Cached FALSE decision must be invalidated by addRoleToUser;"
                        + " user now has a role granting READ on host",
                permissionAPI.doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, false));
    }

    /**
     * {@link PermissionCache#clearCache()} must flush both primaryGroup and
     * shortLivedGroup. This is what the admin-UI "Flush Permission Cache"
     * button ultimately calls (via {@link PermissionAPI#clearCache()}).
     */
    @Test
    public void clearCache_flushesShortLivedGroup() throws DotDataException, DotSecurityException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().nextPersisted();

        roleAPI.addRoleToUser(role, user);

        final Permission p = new Permission();
        p.setPermission(PermissionAPI.PERMISSION_READ);
        p.setRoleId(role.getId());
        p.setInode(site.getIdentifier());
        permissionAPI.save(p, site, sysuser, false);

        // Prime the short-lived cache.
        assertTrue(permissionAPI.doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, false));

        // Simulate the "Flush Permission Cache" admin-UI button by calling the
        // API-level clearCache(), which delegates to PermissionCacheImpl.
        permissionAPI.clearCache();

        // With the underlying DB state unchanged, the next check should still
        // return TRUE — but the cached boolean must be dropped and recomputed,
        // not served stale. We assert the permission decision remains correct
        // post-clear (the cache was wiped and re-populated from DB truth).
        assertTrue("Post-clear lookup should recompute from DB and still return TRUE",
                permissionAPI.doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, false));

        // Now revoke the role without going through RoleAPI/RoleFactory, because
        // that path has its own permission-cache invalidation. This leaves a
        // cached TRUE permission decision in place so clearCache() is the only
        // operation under test.
        revokeRoleMembershipInDbOnly(role, user);
        permissionAPI.clearCache();

        assertFalse("After role revocation + clearCache, decision must be FALSE",
                permissionAPI.doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, false));
    }

    private void revokeRoleMembershipInDbOnly(final Role role, final User user) throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL("delete from users_cms_roles where user_id = ? and role_id = ?");
        dc.addParam(user.getUserId());
        dc.addParam(role.getId());
        dc.loadResult();

        // Keep role lookups honest after bypassing RoleFactory; do not touch
        // PermissionCache here, because clearCache() is the behavior under test.
        CacheLocator.getCmsRoleCache().remove(user.getUserId());
    }
}
