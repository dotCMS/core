package com.dotmarketing.business;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
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
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
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
}
