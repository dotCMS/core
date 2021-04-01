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
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        public TmpBinaryFile(final boolean setContent) throws IOException {
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

    private static Host host;
    private static Role role;

    @BeforeClass
    public static void prepare() throws Exception {

        // Set testing environment
        IntegrationTestInitService.getInstance().init();

        host = new SiteDataGen().nextPersisted();
        role = new RoleDataGen().nextPersisted();

    }

    @DataProvider
    public static Object[][] testCases() {
        return new Object[][] {
                { BY_ID, READ_PERMISSIONS },
                { BY_ID, NO_PERMISSIONS },
                { BY_INODE, READ_PERMISSIONS },
                { BY_INODE, NO_PERMISSIONS },
        };
    }

    @UseDataProvider("testCases")
    @Test
    public void requestBinaryFile(
            final String byIdType, final String permissionType)
            throws DotDataException, DotSecurityException, ServletException, IOException {

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

    @Test
    public void requestGifFile()
            throws DotDataException, DotSecurityException, ServletException, IOException {

        File gifFile = new File("src/integration-test/resources/images/issue19338.gif");

        final Contentlet fileContentlet = new FileAssetDataGen(gifFile).host(host)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();

        final String fileURI = "/contentAsset/image/"+fileContentlet.getInode()+"/fileAsset/byInode/true/quality_q/30/resize_w/200";
        final HttpServletRequest request = mockServletRequest(fileURI);
        request.setAttribute(WebKeys.USER, APILocator.systemUser());

        TmpBinaryFile tmpTargetFile = new TmpBinaryFile(false);
        final HttpServletResponse response = mockServletResponse(tmpTargetFile);

        // Send servlet request
        sendRequest(request, response);
        final byte[] responseContent = Files.readAllBytes(tmpTargetFile.getPath());
        final String expectedContent = "RIFF�\u0003\u0000\u0000WEBPVP8 �\u0003\u0000\u0000�\u001C\u0000�\u0001*�\u0000c\u0000?\u0011x�R�'?���\n"
                + "S�\"\tin�\n"
                + "k\u001F\u001D�\n"
                + "���Wh��/\u0003�k�ߏ]����\u0013q��?�~a���G{��?�~�������A��wv*\u0018��\u0013�p�Z����C$\u0010[\u0000\"���w\u000E�\u000BF���\u001A'����u�E'��\u001C\f����&��J�,�����D\u0011\u0013�n�th�\\\u0015�4\u0015\u0000�\u000Bm2\u0006���\f��\u001D�\u001E��|�^H/�{�2&�6\u0004�H�S��ƍ6����Q\u0006��)���$\u0003���y��3\u0005\"�� /Un�b}�̘\u0000\u0000��q��x�?ջt�l;\u0014N2!�\u000F��6eD���^K�g���KB\u0005Da����u(�1�Gr��C�c�\u001C����?��D�C%O��Q\u00192��́�#�d\u00014�����\u000Bd\u001Aӷ�\u0010V^\u0005h�6�d�r����ںo1\u007F��y\tJ1�h˾�o5\u007F)O�2��\u0016�ꀊ�\u0010\f\u0011\u0010�\u001D3Y�S(i�b4��K�S��Cxs+�zF\u0004{��\u0010\u0006���\u001F�=#\u0002:��\u000B�L\u001D~���IE�q����f���}���[�\u0017lP\u0012\u001D�*�\u0002|�a_C��\u0005��!�]�\u0019V{�<WvY�\"3����j�\u001B-)IңZ\u0010\b\u0012F��'\u0001���R�A�|���<�?�V w�ҹ�|fw���\u0018�B\u0010r�I�P�e����C\u0013�[�#�Y�l\u000E��B�\u0012(�17\",b\"A�-�\u0012�v���\bϢ�r��?05��\u0010=�Bh\u0010�dS�Ы�ҩ+ɀ��at���>4�2��Fp\u0005|��_�\u0007�\u000F���\b�I\u0017\u0015��YI���{ȡMK�H�`�x͜ǝFj\u001E�8�����\n"
                + "��͢'���q\u0003��\u0002�%k��86����\u0010��$��g�V��\u007F�xi�\\�\t��Ÿ�0�����-��\u001C�C\u000F!A;�\u0017��\u0000i��BϦÒ�\b\t�a����,%u�)���vn'�0|�v4��O�//\u0006��Զ��J��#?L1��\u001F�['�q\u001B��\u001C\u001A-�ȡxwY�\u0016 �L=4M�j[�\u0016tU΅j|�a\u001F\u0015��_-�`_\u0005��kv���\u0007Q&�?ӻ(�\u001C\n"
                + "e�\n"
                + "\u0006�@\tĥ5���mx1����/1��.<%��bߞ�p�E;\u0007S�l�<�\u0011�\u000F��\"\u0003��\u007FY�%�%\u0003�\"O��Ǡ�\u0096�\b'�\u000FU~x�S\u0014��b���S�9��v�Xmo�ڢ}��A��3��L~\u0000\u0000\u0000\u0000";

        assertTrue(equalsIgnoreNewlineStyle(expectedContent, new String(responseContent, StandardCharsets.UTF_8)));

    }

    public static boolean equalsIgnoreNewlineStyle(String s1, String s2) {
        return s1 != null && s2 != null && normalizeLineEnds(s1).equals(normalizeLineEnds(s2));
    }

    private static String normalizeLineEnds(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
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

    private HttpServletResponse mockServletResponse(final TmpBinaryFile tmpTargetFile) {
        try (OutputStream outputStream = new FileOutputStream(tmpTargetFile.getFile())) {
            return new MockHttpStatusResponse(new MockHttpCaptureResponse(
                    new BaseResponse().response(), outputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final BinaryExporterServlet binaryExporterServlet = new BinaryExporterServlet();
        binaryExporterServlet.init();
        binaryExporterServlet.doGet(request, response);

    }

    private void addPermissions(final Contentlet fileAsset)
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
