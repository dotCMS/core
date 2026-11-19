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
import com.dotmarketing.image.filter.ResizeImageFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        final Contentlet fileAsset1 = FileAssetDataGen.createFileAsset(folder1, "text1", ".txt");
        final Contentlet fileAsset2 = FileAssetDataGen.createFileAsset(folder1, "text2", ".txt");

        final Folder folder2 = new FolderDataGen().site(host).nextPersisted();
        final Contentlet fileAsset3 = FileAssetDataGen.createFileAsset(folder2, "text1", ".txt");
        final Contentlet fileAsset4 = FileAssetDataGen.createFileAsset(folder2, "text2", ".txt");

        this.addPermission(role, folder1, folder2, fileAsset1, fileAsset2, fileAsset3, fileAsset4);
        final List<FileAsset> files = fileAssetAPIImpl.findFileAssetsByFolder(folder1, user, false);

        assertEquals(2, files.size());

        final List<String> filesInodes = files.stream().map((file) -> file.getInode()).collect(Collectors.toList());

        assertTrue(filesInodes.contains(fileAsset1.getInode()));
        assertTrue(filesInodes.contains(fileAsset2.getInode()));
    }

    /**
     * Method to test: {@link FileAssetAPIImpl#findFileAssetsByFolder(Folder, User, boolean)}
     * When: Create one Folder with one files, and try to get the files from the first folder with a user without permission
     * Should: return a empty List
     *
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void userWithoutPermission() throws IOException, DotDataException, DotSecurityException {
        final Role backEndUserRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role, backEndUserRole).nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Folder folder1 = new FolderDataGen().site(host).nextPersisted();
        FileAssetDataGen.createFileAsset(folder1, "text1", ".txt");

        final List<FileAsset> fileAssetsByFolder = fileAssetAPIImpl.findFileAssetsByFolder(folder1, user, false);

        assertTrue(fileAssetsByFolder.isEmpty());
    }



    private void addPermission(
            final Role role,
            final Permissionable... permissionables)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        for (final Permissionable permissionable : permissionables) {

            final Permission readPermission = new Permission();
            readPermission.setInode(permissionable.getPermissionId());
            readPermission.setRoleId(role.getId());
            readPermission.setPermission(PermissionAPI.PERMISSION_READ);

            APILocator.getPermissionAPI().save(readPermission, permissionable, systemUser, false);
        }
    }

    /**
     * Method to test: {@link FileAssetAPIImpl#cleanThumbnailsFromFileAsset(IFileAsset)}
     * When: two file asset versions whose inodes share the same first two characters (so their
     * renditions live in the same dotGenerated bucket folder) both have chained renditions, generated
     * from a request that names the content by identifier, and the thumbnails of one are cleaned
     * Should: delete only that inode's renditions and leave the other asset's renditions alone
     */
    @Test
    public void cleanThumbnailsFromFileAsset_deletesOnlyThatInodesRenditions() throws Exception {
        final String inodeA = UUID.randomUUID().toString();
        final String inodeB = inodeA.substring(0, 2) + UUID.randomUUID().toString().substring(2);
        final List<File> renditionsA = generateRenditions(inodeA);
        final List<File> renditionsB = generateRenditions(inodeB);
        try {
            final FileAsset fileAssetA = new FileAsset();
            fileAssetA.setInode(inodeA);

            fileAssetAPIImpl.cleanThumbnailsFromFileAsset(fileAssetA);

            for (final File rendition : renditionsA) {
                assertFalse("should be deleted: " + rendition, rendition.exists());
            }
            for (final File rendition : renditionsB) {
                assertTrue("should be kept: " + rendition, rendition.exists());
            }
        } finally {
            for (final File rendition : renditionsB) {
                rendition.delete();
            }
            for (final File rendition : renditionsA) {
                rendition.delete();
            }
        }
    }

    /**
     * Writes the renditions a two-stage resize chain would produce for the binary stored under the
     * given version inode, requested by an unrelated identifier.
     */
    private static List<File> generateRenditions(final String inode) throws IOException {
        final File binary = new File(File.separator + "assets" + File.separator + inode.charAt(0)
                + File.separator + inode.charAt(1) + File.separator + inode + File.separator
                + "fileAsset" + File.separator + "image.jpg");
        final Map<String, String[]> parameters = new HashMap<>();
        parameters.put("filter", new String[]{"Resize", "Resize"});
        parameters.put("resize_w", new String[]{"100"});
        parameters.put("fieldVarName", new String[]{"fileAsset"});
        parameters.put("assetInodeOrIdentifier", new String[]{UUID.randomUUID().toString()});

        final File firstStage = new ResizeImageFilter().getResultsFile(binary, parameters);
        final File secondStage = new ResizeImageFilter().getResultsFile(firstStage, parameters);
        final List<File> renditions = new ArrayList<>();
        for (final File stage : new File[]{firstStage, secondStage}) {
            assertTrue(stage.getName(), stage.getName().startsWith("dotGenerated_" + inode + "_"));
            Files.write(stage.toPath(), "rendition".getBytes());
            renditions.add(stage);
        }
        return renditions;
    }
}
