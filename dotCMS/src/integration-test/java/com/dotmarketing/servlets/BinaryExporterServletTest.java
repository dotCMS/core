package com.dotmarketing.servlets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockServletPathRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.mock.response.MockHttpCaptureResponse;
import com.dotcms.mock.response.MockHttpStatusResponse;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class BinaryExporterServletTest {

    // Temporary binary png file
    private static class TmpBinaryFile implements Closeable {

        private final Path pngFilePath;

        public TmpBinaryFile(boolean setContent) throws IOException {
            pngFilePath = Files.createTempFile("tmp", ".png");
            if (setContent) {
                Files.write(pngFilePath, ShortyServletAndTitleImageTest.pngPixel);
            }
        }

        public Path getPath() {
            return pngFilePath;
        }

        public File getFile() {
            return pngFilePath.toFile();
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(pngFilePath);
        }
    }

    private static final String BY_ID = "by-identifier";
    private static final String BY_INODE = "by-inode";

    private static final String READ_PERMISSIONS = "has-read-permissions";
    private static final String NO_PERMISSIONS = "no-permissions";

    private static Host host = null;
    private static Role role = null;

    @BeforeClass
    public static void prepare() throws Exception {
        // Set testing environment
        IntegrationTestInitService.getInstance().init();

        host = new SiteDataGen().nextPersisted();
        role = new RoleDataGen().nextPersisted();
    }

    @DataProvider
    public static Object[] testCases() {
        return new String[][] {
                { BY_ID, READ_PERMISSIONS },
                { BY_ID, NO_PERMISSIONS },
                { BY_INODE, READ_PERMISSIONS },
                { BY_INODE, NO_PERMISSIONS },
        };
    }


    @Test
    @UseDataProvider("testCases")
    public void requestBinaryFile(
            final String byIdType, final String permissionType) throws Exception {

        final boolean byIdentifier = byIdType.equals(BY_ID);
        final boolean permissionsRequired = permissionType.equals(NO_PERMISSIONS);

        Contentlet fileAsset = null;
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        try (TmpBinaryFile tmpSourceFile = new TmpBinaryFile(true);
             TmpBinaryFile tmpTargetFile = new TmpBinaryFile(false)) {

            // Checkin file
            fileAsset = checkinFileAsset(tmpSourceFile, folder);

            // Set asset permissions
            if (permissionsRequired) {
                addPermissions(fileAsset);
            }

            // Build request and response
            final String fileURI = "/contentAsset/raw-data/"
                    + (byIdentifier ? fileAsset.getIdentifier() : fileAsset.getInode())
                    + "/fileAsset/";
            final HttpServletRequest request = mockServletRequest(fileURI);
            final HttpServletResponse response = mockServletResponse(tmpTargetFile);

            // Send servlet request
            sendRequest(request, response);

            if (permissionsRequired) {
                // Verify response status
                assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
            } else {
                // Verify response
                final byte[] responseContent = Files.readAllBytes(tmpTargetFile.getPath());
                assertArrayEquals(ShortyServletAndTitleImageTest.pngPixel, responseContent);
            }

        } finally {
            if (UtilMethods.isSet(fileAsset) && UtilMethods.isSet(fileAsset.getInode())) {
                FileAssetDataGen.remove(fileAsset);
            }
            if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
                FolderDataGen.remove(folder);
            }
        }

    }

    private Contentlet checkinFileAsset(final TmpBinaryFile tmpSourceFile, final Folder folder)
            throws DotSecurityException, DotDataException {

        final Contentlet fileAsset = new FileAssetDataGen(
                folder, tmpSourceFile.getFile()).nextPersisted();
        ContentletDataGen.publish(fileAsset);
        assertTrue(APILocator.getContentletAPI().isInodeIndexed(
                fileAsset.getInode(), true, 1000));
        return fileAsset;

    }

    private HttpServletRequest mockServletRequest(final String fileURI) {
        return new MockSessionRequest(new MockServletPathRequest(
                new MockHttpRequest("localhost", fileURI).request(),
                "/contentAsset"));
    }

    private HttpServletResponse mockServletResponse(TmpBinaryFile tmpTargetFile) {
        return new MockHttpStatusResponse(new MockHttpCaptureResponse(
                new BaseResponse().response(), tmpTargetFile.getFile()));
    }

    private void sendRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final BinaryExporterServlet binaryExporterServlet = new BinaryExporterServlet();
        binaryExporterServlet.init();
        binaryExporterServlet.doGet(request, response);

    }

    private void addPermissions(Contentlet fileAsset)
            throws DotSecurityException, DotDataException {

        final User systemUser = APILocator.systemUser();
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();

        final Permission anonPermission = new Permission();
        anonPermission.setInode(fileAsset.getPermissionId());
        anonPermission.setRoleId(anonymousRole.getId());
        anonPermission.setPermission(0);

        final Permission permission = new Permission();
        permission.setInode(fileAsset.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(
                CollectionsUtils.list(anonPermission, permission),
                fileAsset, systemUser, false);

    }

}
