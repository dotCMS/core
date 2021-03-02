package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class PermissionBitAPITest extends IntegrationTestBase  {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link PermissionBitAPIImpl#doesUserHavePermission(Permissionable, int, User, boolean)}
     * When: Admin user ask for permission about a newly Folder (with inode and id equals to null)
     * Should: Return false
     */
    @Test
    public void testDoesUserHavePermissionForNewFolder() throws DotDataException {
        final Permissionable newlyFolder = new FolderDataGen().next();
        int permissionType = PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
        final User user = mock(User.class);

        final PermissionBitAPIImpl permissionBitAPIImpl = new PermissionBitAPIImpl();

        final boolean havePermission = permissionBitAPIImpl.doesUserHavePermission(newlyFolder, permissionType, user,
                false);

        assertFalse(havePermission);
    }
}
