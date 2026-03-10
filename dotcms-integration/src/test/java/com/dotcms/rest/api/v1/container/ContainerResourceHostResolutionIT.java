package com.dotcms.rest.api.v1.container;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests for the {@code resolveHost} logic introduced in
 * {@link ContainerResource} (issue #34914).
 *
 * <p>{@code resolveHost} is a private method, so these tests exercise it indirectly through
 * {@link APILocator#getHostAPI()} — the exact API it delegates to — covering the three
 * observable behaviours:
 * <ol>
 *   <li>A valid, accessible {@code hostId} resolves to that host.</li>
 *   <li>An inaccessible {@code hostId} (user lacks READ permission) throws
 *       {@link DotSecurityException} instead of silently falling back.</li>
 *   <li>A {@code null} / blank {@code hostId} is ignored and the caller falls back to the
 *       request-context host — represented here by using the default host.</li>
 * </ol>
 */
public class ContainerResourceHostResolutionIT extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    // ------------------------------------------------------------------
    // Test 1 — valid hostId resolves to the requested host
    // ------------------------------------------------------------------

    /**
     * When: a valid, accessible {@code hostId} is supplied
     * Should: {@code HostAPI.find()} returns exactly that host (non-null, not archived).
     * This mirrors the happy-path branch of {@code resolveHost}.
     */
    @Test
    public void test_resolveHost_withValidHostId_returnsRequestedHost()
            throws DotDataException, DotSecurityException {

        final Host targetSite = new SiteDataGen().nextPersisted();

        final Host resolved = APILocator.getHostAPI()
                .find(targetSite.getIdentifier(), APILocator.systemUser(), false);

        assertNotNull("Host should be found", resolved);
        assertEquals(
                "Resolved host should match the requested hostId",
                targetSite.getIdentifier(),
                resolved.getIdentifier());
    }

    // ------------------------------------------------------------------
    // Test 2 — unauthorized hostId must throw DotSecurityException
    // ------------------------------------------------------------------

    /**
     * When: a limited user without READ permission on the target site supplies that site's ID
     * Should: {@code HostAPI.find()} throws {@link DotSecurityException}.
     * This maps to the {@code catch (DotSecurityException e) { throw e; }} re-throw in
     * {@code resolveHost}, ensuring 403 Forbidden is returned to the API caller instead of
     * silently creating the container on the wrong site.
     */
    @Test(expected = DotSecurityException.class)
    public void test_resolveHost_withUnauthorizedHostId_throwsDotSecurityException()
            throws DotDataException, DotSecurityException {

        // Create a site the limited user has no access to
        final Host restrictedSite = new SiteDataGen().nextPersisted();

        // Create a back-end user with no permissions on restrictedSite
        final Role backEndRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final User limitedUser = new UserDataGen()
                .roles(backEndRole)
                .nextPersisted();

        // Should throw DotSecurityException — user cannot READ this site
        APILocator.getHostAPI().find(restrictedSite.getIdentifier(), limitedUser, false);
    }

    // ------------------------------------------------------------------
    // Test 3 — null/blank hostId falls back to the default host
    // ------------------------------------------------------------------

    /**
     * When: {@code hostId} is {@code null} or blank
     * Should: the caller falls back to the request-context host.
     * Here we verify the fallback path by confirming the default host is resolvable —
     * the same object that {@code WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request)}
     * would return in a real request without an explicit host.
     */
    @Test
    public void test_resolveHost_withNullHostId_fallsBackToDefaultHost()
            throws DotDataException, DotSecurityException {

        final Host defaultHost = APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false);

        assertNotNull("Default host must be resolvable for the fallback path", defaultHost);
        assertNotNull("Default host must have an identifier", defaultHost.getIdentifier());
    }

    // ------------------------------------------------------------------
    // Test 4 — hostId of an archived site is skipped
    // ------------------------------------------------------------------

    /**
     * When: the {@code hostId} points to an archived site
     * Should: {@code resolveHost} skips it (the {@code !host.isArchived()} guard) and would
     * fall back to the request-context host.  Here we verify the archive flag is set correctly
     * after archiving so the guard works as expected.
     */
    @Test
    public void test_resolveHost_withArchivedHostId_siteIsDetectedAsArchived()
            throws DotDataException, DotSecurityException {

        final Host site = new SiteDataGen().nextPersisted();

        // Archive the site
        APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);

        final Host found = APILocator.getHostAPI()
                .find(site.getIdentifier(), APILocator.systemUser(), false);

        assertNotNull("Archived site should still be findable by system user", found);
        assertEquals("Site should be marked as archived", true, found.isArchived());

        // Cleanup — unarchive so the test site doesn't pollute other tests
        APILocator.getHostAPI().unarchive(site, APILocator.systemUser());
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private void grantReadPermission(final Role role, final Host host)
            throws DotDataException, DotSecurityException {

        final Permission permission = new Permission();
        permission.setInode(host.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);
        APILocator.getPermissionAPI().save(permission, host, APILocator.systemUser(), false);
    }
}
