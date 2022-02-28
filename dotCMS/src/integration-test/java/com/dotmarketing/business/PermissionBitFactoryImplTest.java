package com.dotmarketing.business;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionType;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PermissionBitFactoryImplTest {

    @Before
    public void init() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void assignPermissions() throws DotDataException {

        final User user = new UserDataGen().nextPersisted();
        final PermissionBitFactoryImpl permissionBitFactory = new PermissionBitFactoryImpl(CacheLocator.getPermissionCache());

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Role userRole = APILocator.getRoleAPI().getUserRole(user);
        Permission readPermissions = new Permission( contentType.getPermissionId(),
                userRole.getId(), PermissionAPI.PERMISSION_READ );

        Permission editPermissions = new Permission( contentType.getPermissionId(),
                userRole.getId(), PermissionAPI.PERMISSION_EDIT );

        permissionBitFactory.assignPermissions(list(readPermissions, editPermissions), contentType);

        final List<Permission> permissions = APILocator.getPermissionAPI()
                .getPermissions(contentType);

        assertEquals(2, permissions.size());
        assertEquals(userRole.getId(), permissions.get(0).getRoleId());
        assertEquals(userRole.getId(), permissions.get(1).getRoleId());
        final boolean hasReadPermission = permissions.stream()
                .anyMatch(permission -> PermissionAPI.PERMISSION_READ == permission.getPermission());

        final boolean hasEditPermission = permissions.stream()
                .anyMatch(permission -> PermissionAPI.PERMISSION_EDIT == permission.getPermission());

        assertTrue(hasReadPermission);
        assertTrue(hasEditPermission);

        final String query = "SELECT * from dist_reindex_journal WHERE inode_to_index = ? OR inode_to_index = ?";
        DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(query);
        dotConnect.addParam(contentlet_1.getIdentifier());
        dotConnect.addParam(contentlet_2.getIdentifier());

        final List<Map<String, Object>> maps = dotConnect.loadObjectResults();

        assertEquals(maps.size(), 2);

        final boolean hasContentlet1Permission = maps.stream()
                .anyMatch(map -> map.get("inode_to_index").equals(contentlet_1.getIdentifier()));
        final boolean hasContentlet2Permission = maps.stream()
                .anyMatch(map -> map.get("inode_to_index").equals(contentlet_2.getIdentifier()));

        assertTrue(hasContentlet1Permission);
        assertTrue(hasContentlet2Permission);
    }
}
