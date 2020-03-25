package com.dotmarketing.portlets.fileassets.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.*;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileAssetAPIImplIntegrationTest  extends IntegrationTestBase {

    private static FileAssetAPIImpl fileAssetAPIImpl;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment o
        IntegrationTestInitService.getInstance().init();

        fileAssetAPIImpl = new FileAssetAPIImpl();
    }

    /**
     * Method to test: {@link FileAssetAPIImpl#findFileAssetsByFolder(Folder, User, boolean)}
     * When: Create two Folder with two files each one, grant permission over folders and files
     * and try to get the files from the first folder
     * Should: return only the two files into the first folder
     *
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void shouldReturnTwoFiles() throws IOException, DotDataException, DotSecurityException {
        final Role backEndUserRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role, backEndUserRole).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Folder folder1 = new FolderDataGen().site(host).nextPersisted();
        final Contentlet fileAsset1 = createFileAsset(folder1, "text1", ".txt");
        final Contentlet fileAsset2 = createFileAsset(folder1, "text2", ".txt");

        final Folder folder2 = new FolderDataGen().site(host).nextPersisted();
        final Contentlet fileAsset3 = createFileAsset(folder2, "text1", ".txt");
        final Contentlet fileAsset4 = createFileAsset(folder2, "text2", ".txt");

        this.addPermission(role, folder1, folder2, fileAsset1, fileAsset2, fileAsset3, fileAsset4);
        final List<FileAsset> files = fileAssetAPIImpl.findFileAssetsByFolder(folder1, user, false);

        assertEquals(2, files.size());

        final List<String> filesInodes = files.stream().map((file) -> file.getInode()).collect(Collectors.toList());

        assertTrue(filesInodes.contains(fileAsset1.getInode()));
        assertTrue(filesInodes.contains(fileAsset2.getInode()));
    }

    private Contentlet createFileAsset(
            final Folder folder,
            final String fileName,
            final String suffix)
            throws IOException, DotSecurityException, DotDataException {
        final java.io.File file = java.io.File.createTempFile(fileName, suffix);
        FileUtil.write(file, "helloworld");

        return  new FileAssetDataGen(folder, file).nextPersisted();
    }

    private void addPermission(
            final Role role,
            final Permissionable... permissionables)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        for (Permissionable permissionable : permissionables) {

            final Permission readPermission = new Permission();
            readPermission.setInode(permissionable.getPermissionId());
            readPermission.setRoleId(role.getId());
            readPermission.setPermission(PermissionAPI.PERMISSION_READ);

            APILocator.getPermissionAPI().save(readPermission, permissionable, systemUser, false);
        }
    }
}
