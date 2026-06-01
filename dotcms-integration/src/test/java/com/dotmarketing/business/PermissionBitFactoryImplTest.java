package com.dotmarketing.business;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.liferay.portal.model.User;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class PermissionBitFactoryImplTest {

    @Before
    public void init() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#addPermissionsToCache(Permissionable)} When:
     * Create a {@link ContentType} and two {@link Contentlet} and assign two new permission to the
     * ContentType Should: Create a register for each COntentlet in dist_reindex_journal
     *
     * @throws DotDataException
     */
    @Test
    public void assignPermissions() throws DotDataException, DotSecurityException {

        ReindexQueueAPI queueAPI = APILocator.getReindexQueueAPI();
        TestDataUtils.assertEmptyQueue();

        final User user = new UserDataGen().nextPersisted();
        final PermissionBitFactoryImpl permissionBitFactory = new PermissionBitFactoryImpl(
                CacheLocator.getPermissionCache());

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        // Ensure contentlets indexed but not permissions
        TestDataUtils.assertEmptyQueue();

        LocalTransaction.wrap(() -> {
            final Role userRole = APILocator.getRoleAPI().getUserRole(user);
            Permission readPermissions = new Permission(contentType.getPermissionId(),
                    userRole.getId(), PermissionAPI.PERMISSION_READ);

            Permission editPermissions = new Permission(contentType.getPermissionId(),
                    userRole.getId(), PermissionAPI.PERMISSION_EDIT);

            permissionBitFactory.assignPermissions(list(readPermissions, editPermissions),
                    contentType);

            final List<Permission> permissions = APILocator.getPermissionAPI()
                    .getPermissions(contentType);

            assertEquals(2, permissions.size());
            assertEquals(userRole.getId(), permissions.get(0).getRoleId());
            assertEquals(userRole.getId(), permissions.get(1).getRoleId());
            final boolean hasReadPermission = permissions.stream()
                    .anyMatch(permission -> PermissionAPI.PERMISSION_READ
                            == permission.getPermission());

            final boolean hasEditPermission = permissions.stream()
                    .anyMatch(permission -> PermissionAPI.PERMISSION_EDIT
                            == permission.getPermission());

            assertTrue(hasReadPermission);
            assertTrue(hasEditPermission);
            // reindex jornal will clear after queue is unpaused after transsaction.
            final String query = "SELECT * from dist_reindex_journal WHERE inode_to_index = ? OR inode_to_index = ?";
            DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(query);
            dotConnect.addParam(contentlet_1.getIdentifier());
            dotConnect.addParam(contentlet_2.getIdentifier());

            final List<Map<String, Object>> maps = dotConnect.loadObjectResults();

            assertEquals(2, maps.size());

            final boolean hasContentlet1Permission = maps.stream()
                    .anyMatch(
                            map -> map.get("inode_to_index").equals(contentlet_1.getIdentifier()));
            final boolean hasContentlet2Permission = maps.stream()
                    .anyMatch(
                            map -> map.get("inode_to_index").equals(contentlet_2.getIdentifier()));

            assertTrue(hasContentlet1Permission);
            assertTrue(hasContentlet2Permission);
        });

        // Ensure queue is cleared up.
        TestDataUtils.assertEmptyQueue();

    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl} loadPermissions walk-up caching behavior
     * When: a permissionable has no parent (getParentPermissionable() returns null), causing the
     * walk-up to return an empty permission list
     * Should: NOT cache the empty result — the next request must retry the walk-up instead of
     * being served a cached empty list that would cause persistent 401s for anonymous users
     */
    @Test
    public void testEmptyWalkUpResultIsNotCached() throws DotDataException {
        final PermissionCache cache = CacheLocator.getPermissionCache();

        // Permissionable with no parent and no DB rows forces the walk-up to return empty
        final String orphanId = UUID.randomUUID().toString();
        final Permissionable orphan = new Permissionable() {
            @Override public String getPermissionId() { return orphanId; }
            @Override public Permissionable getParentPermissionable() { return null; }
            @Override public String getPermissionType() { return IHTMLPage.class.getCanonicalName(); }
            @Override public String getOwner() { return null; }
            @Override public void setOwner(String owner) {}
            @Override public List<PermissionSummary> acceptedPermissions() { return Collections.emptyList(); }
            @Override public List<RelatedPermissionableGroup> permissionDependencies(int r) { return Collections.emptyList(); }
            @Override public boolean isParentPermissionable() { return false; }
        };

        assertNull("Sanity: orphan must not be in cache before test",
                cache.getPermissionsFromCache(orphanId));

        final List<Permission> perms = APILocator.getPermissionAPI().getPermissions(orphan);
        assertTrue("Walk-up with null parent must return empty permissions", perms.isEmpty());

        // Before the fix this returned [] (not null), causing every subsequent request to skip
        // the walk-up and return no permissions — a persistent 401 until the next
        // resetPermissionReferences() evicted the entry.
        assertNull("Empty walk-up result must not be written to cache",
                cache.getPermissionsFromCache(orphanId));
    }
}
