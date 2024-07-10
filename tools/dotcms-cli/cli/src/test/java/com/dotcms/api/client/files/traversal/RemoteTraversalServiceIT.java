package com.dotcms.api.client.files.traversal;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.common.FilesTestHelperService;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class RemoteTraversalServiceIT {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    ServiceManager serviceManager;

    @Inject
    RemoteTraversalService remoteTraversalService;

    @Inject
    FilesTestHelperService filesTestHelper;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(
                ServiceBean.builder().
                        name("default").
                        url(new URL("http://localhost:8080")).
                        active(true).
                        build()
        );

        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    @Test
    void Test_Not_Found() {

        var folderPath = String.format("//%s/%s", siteName, "folderDoesNotExist");

        try {

            remoteTraversalService.traverseRemoteFolder(
                    folderPath,
                    0,
                    true,
                    parsePatternOption(null),
                    parsePatternOption(null),
                    parsePatternOption(null),
                    parsePatternOption(null)
            );

            Assertions.fail(" 404 Exception should have been thrown here.");
        } catch (Exception e) {
            Assertions.assertTrue(e.getCause() instanceof NotFoundException);
        }
    }

    @Test
    void Test_Folders_Check() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================

        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 2 asset)
        Assertions.assertEquals(2, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has 3 asset)
        Assertions.assertEquals(3,
                treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(3).assets().size());
    }

    /**
     * This method is used to test the remote traversal functionality for a specific asset, and
     * specifically an asset with spaces in the name.
     * <p>
     * It prepares the data for the test, sets up the folder path to be checked, and then performs
     * the asset check using the remote traversal service. Finally, it validates the result by
     * asserting the expected number of children and assets in the tree.
     *
     * @throws IOException if an I/O error occurs during the test
     */
    @Test
    void Test_Asset_Check() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s/folder3/image 3.png", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Folder3 (Root)
        Assertions.assertEquals(0, treeNode.children().size());

        // Folder3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.assets().size());
    }

    /**
     * This method is used to test the remote traversal functionality for a specific asset, and
     * specifically an asset located in a nested folder structure.
     * <p>
     * It prepares the data for the test, sets up the folder path to be checked, and then performs
     * the asset check using the remote traversal service. Finally, it validates the result by
     * asserting the expected number of children and assets in the tree.
     *
     * @throws IOException if an I/O error occurs during the test
     */
    @Test
    void Test_Asset_Check2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format(
                "//%s/folder2/subFolder2-1/subFolder2-1-1-子資料夾/image2.png",
                testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );

        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // subFolder2-1-1-子資料夾 (Root)
        Assertions.assertEquals(0, treeNode.children().size());

        // subFolder2-1-1-子資料夾 (has 3 asset)
        Assertions.assertEquals(3, treeNode.assets().size());
    }

    @Test
    void Test_Folders_Depth_Zero() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData(false);

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                0,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());
        // Folder1
        Assertions.assertEquals(0, treeNode.children().get(0).children().size());
        // Folder2
        Assertions.assertEquals(0, treeNode.children().get(1).children().size());
        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
    }

    @Test
    void Test_Include() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData(false);

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption("**/subFolder1-1/**"),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());
        for (var child : treeNode.children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        for (var child : treeNode.children().get(0).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }

        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        for (var child : treeNode.children().get(0).children().get(0).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
            Assertions.assertTrue(child.folder().explicitGlobInclude());
        }
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        for (var child : treeNode.children().get(0).children().get(1).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        for (var child : treeNode.children().get(1).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }

        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        for (var child : treeNode.children().get(1).children().get(0).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
    }

    @Test
    void Test_Include2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData(false);

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption("**/subFolder1-1"),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());
        for (var child : treeNode.children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        Assertions.assertTrue(
                treeNode.children().get(0).children().get(0).folder().explicitGlobInclude());
        Assertions.assertTrue(
                treeNode.children().get(0).children().get(0).folder().implicitGlobInclude());
        Assertions.assertFalse(
                treeNode.children().get(0).children().get(1).folder().implicitGlobInclude());
        Assertions.assertFalse(
                treeNode.children().get(0).children().get(2).folder().implicitGlobInclude());

        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        for (var child : treeNode.children().get(0).children().get(0).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        for (var child : treeNode.children().get(0).children().get(1).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        for (var child : treeNode.children().get(1).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }

        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        for (var child : treeNode.children().get(1).children().get(0).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
    }

    @Test
    void Test_Include3() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData(false);

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption("folder1,folder3"),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        Assertions.assertTrue(
                treeNode.children().get(0).folder().explicitGlobInclude());
        Assertions.assertTrue(
                treeNode.children().get(0).folder().implicitGlobInclude());
        Assertions.assertFalse(
                treeNode.children().get(1).folder().explicitGlobInclude());
        Assertions.assertFalse(
                treeNode.children().get(1).folder().implicitGlobInclude());
        Assertions.assertTrue(
                treeNode.children().get(2).folder().explicitGlobInclude());
        Assertions.assertTrue(
                treeNode.children().get(2).folder().implicitGlobInclude());

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        for (var child : treeNode.children().get(0).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }
    }

    @Test
    void Test_Include_Assets() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption("**/*.png"),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has 3 asset)
        Assertions.assertEquals(3,
                treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(3).assets().size());
    }

    @Test
    void Test_Include_Assets2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption("**/*.jpg"),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(3).assets().size());
    }

    @Test
    void Test_Include_Assets3() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption("**/*.jpg, **/*.png"),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 2 asset)
        Assertions.assertEquals(2, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has 3 asset)
        Assertions.assertEquals(3,
                treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(3).assets().size());
    }

    @Test
    void Test_Include_Assets4() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption("**/image?.*"),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 2 asset)
        Assertions.assertEquals(2, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(3).assets().size());
    }

    @Test
    void Test_Include_Assets5() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption("folder1/subFolder1-1/subFolder1-1-1/image?.*"),
                parsePatternOption(null),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 2 asset)
        Assertions.assertEquals(2, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(3).assets().size());
    }

    @Test
    void Test_Exclude() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData(false);

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("**/subFolder1-1/**"),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());
        for (var child : treeNode.children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        for (var child : treeNode.children().get(0).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }

        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        for (var child : treeNode.children().get(0).children().get(0).children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
            Assertions.assertTrue(child.folder().explicitGlobExclude());
        }
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        for (var child : treeNode.children().get(0).children().get(1).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        for (var child : treeNode.children().get(1).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }

        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        for (var child : treeNode.children().get(1).children().get(0).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
    }

    @Test
    void Test_Exclude2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData(false);

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("**/subFolder1-1"),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());
        for (var child : treeNode.children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        Assertions.assertTrue(
                treeNode.children().get(0).children().get(0).folder().explicitGlobExclude());
        Assertions.assertFalse(
                treeNode.children().get(0).children().get(0).folder().implicitGlobInclude());
        Assertions.assertTrue(
                treeNode.children().get(0).children().get(1).folder().implicitGlobInclude());
        Assertions.assertTrue(
                treeNode.children().get(0).children().get(2).folder().implicitGlobInclude());

        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        for (var child : treeNode.children().get(0).children().get(0).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        for (var child : treeNode.children().get(0).children().get(1).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        for (var child : treeNode.children().get(1).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }

        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        for (var child : treeNode.children().get(1).children().get(0).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
    }

    @Test
    void Test_Exclude3() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData(false);

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("folder1,folder3"),
                parsePatternOption(null)
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        Assertions.assertTrue(
                treeNode.children().get(0).folder().explicitGlobExclude());
        Assertions.assertFalse(
                treeNode.children().get(0).folder().implicitGlobInclude());
        Assertions.assertTrue(
                treeNode.children().get(1).folder().implicitGlobInclude());
        Assertions.assertTrue(
                treeNode.children().get(2).folder().explicitGlobExclude());
        Assertions.assertFalse(
                treeNode.children().get(2).folder().implicitGlobInclude());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        for (var child : treeNode.children().get(1).children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }
    }

    @Test
    void Test_Exclude_Assets() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("**/*.png")
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(3).assets().size());
    }

    @Test
    void Test_Exclude_Assets2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("**/*.jpg")
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has 3 asset)
        Assertions.assertEquals(3,
                treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(3).assets().size());
    }

    @Test
    void Test_Exclude_Assets3() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("**/*.jpg,**/*.png")
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(3).assets().size());
    }

    @Test
    void Test_Exclude_Assets4() throws IOException {

        // Preparing the data for the test
        final var testSiteName = filesTestHelper.prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var result = remoteTraversalService.traverseRemoteFolder(
                folderPath,
                null,
                true,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("folder1/subFolder1-1/subFolder1-1-1/image?.*")
        );
        var optional = result.treeNode();
        Assertions.assertTrue(optional.isPresent());
        var treeNode = optional.get();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(4, treeNode.children().size());

        // Sorting the children to make the test deterministic
        treeNode.sortChildren();
        treeNode.children().get(0).sortChildren();
        treeNode.children().get(1).sortChildren();
        treeNode.children().get(2).sortChildren();
        treeNode.children().get(3).sortChildren();

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // subFolder1-1-1 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(0).children().get(0).assets().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // subFolder2-1-1-子資料夾 (has 3 asset)
        Assertions.assertEquals(3,
                treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());

        // Folder 4
        Assertions.assertEquals(0, treeNode.children().get(3).children().size());
        // Folder 4 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(3).assets().size());
    }

    /**
     * Parses the pattern option string into a set of patterns.
     *
     * @param patterns the pattern option string containing patterns separated by commas
     * @return a set of parsed patterns
     */
    private Set<String> parsePatternOption(String patterns) {

        var patternsSet = new HashSet<String>();

        if (patterns == null) {
            return patternsSet;
        }

        for (String pattern : patterns.split(",")) {
            patternsSet.add(pattern.trim());
        }

        return patternsSet;
    }

}
