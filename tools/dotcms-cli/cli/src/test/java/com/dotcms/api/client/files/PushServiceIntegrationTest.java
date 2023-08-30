package com.dotcms.api.client.files;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.cli.common.FilesTestHelper;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PushServiceIntegrationTest extends FilesTestHelper {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    ServiceManager serviceManager;

    @Inject
    RemoteTraversalService remoteTraversalService;

    @Inject
    PullService pullService;

    @Inject
    PushService pushService;

    @Inject
    WorkspaceManager workspaceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(
                ServiceBean.builder().
                        name("default").
                        active(true).
                        build()
        );

        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    /**
     * This method tests the scenario where there is nothing to push. It creates a temporal folder
     * for the pull operation, prepares the test data, performs a pull operation, and then attempts
     * to push the same content. It asserts that there are no assets or folders to push, delete,
     * modify, or create.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void Test_Nothing_To_Push() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = prepareData();

            final var folderPath = String.format("//%s", testSiteName);

            var result = remoteTraversalService.traverseRemoteFolder(
                    folderPath,
                    null,
                    true,
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>()
            );

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();
            pullService.pullTree(outputOptions, result.getRight(), workspace.files().toAbsolutePath().toFile(),
                    true, true, true, 0);

            // ---
            // Now we are going to push the content
            var traversalResult = pushService.traverseLocalFolders(outputOptions, tempFolder.toFile(), tempFolder.toFile(),
                    true, true, true, true);

            Assertions.assertNotNull(traversalResult);
            Assertions.assertEquals(2, traversalResult.size());// Live and working folders
            Assertions.assertEquals(0, traversalResult.get(0).getLeft().size());// No errors should be found
            Assertions.assertEquals(0, traversalResult.get(1).getLeft().size());// No errors should be found

            var treeNode = traversalResult.get(0).getRight();
            var treeNodePushInfo = treeNode.collectTreeNodePushInfo();

            // Should be nothing to push as we are pushing the same folder we pull
            Assertions.assertEquals(0, treeNodePushInfo.assetsToPushCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsNewCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsModifiedCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsToDeleteCount());
            Assertions.assertEquals(0, treeNodePushInfo.foldersToPushCount());
            Assertions.assertEquals(0, treeNodePushInfo.foldersToDeleteCount());

            pushService.processTreeNodes(outputOptions, tempFolder.toAbsolutePath().toString(),
                    traversalResult.get(0).getMiddle(), traversalResult.get(0).getRight(), treeNodePushInfo,
                    true, 0);
        } finally {
            // Clean up the temporal folder
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method tests the scenario where there is a new site to push. It creates a temporal
     * folder for the pull operation, prepares the test data, performs a pull operation, renames the
     * site folder to simulate a new site, pushes the content, and validates that the new site was
     * created correctly.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void Test_Push_New_Site() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = prepareData();

            final var folderPath = String.format("//%s", testSiteName);

            var result = remoteTraversalService.traverseRemoteFolder(
                    folderPath,
                    null,
                    true,
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>()
            );

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();
            pullService.pullTree(outputOptions, result.getRight(), workspace.files().toAbsolutePath().toFile(),
                    true, true, true, 0);

            // ---
            // Renaming the site folder to simulate a new site
            final String newSiteName = String.format("site-%d", System.currentTimeMillis());
            var currentLiveSitePath = workspace.files().toAbsolutePath() + "/live/en-us/" + testSiteName;
            var newLiveSitePath = workspace.files().toAbsolutePath() + "/live/en-us/" + newSiteName;
            Files.move(Path.of(currentLiveSitePath), Path.of(newLiveSitePath));

            var currentWorkingSitePath = workspace.files().toAbsolutePath() + "/working/en-us/" + testSiteName;
            var newWorkingSitePath = workspace.files().toAbsolutePath() + "/working/en-us/" + newSiteName;
            Files.move(Path.of(currentWorkingSitePath), Path.of(newWorkingSitePath));

            // ---
            // Now we are going to push the content
            var traversalResult = pushService.traverseLocalFolders(outputOptions, tempFolder.toFile(), tempFolder.toFile(),
                    true, true, true, true);

            Assertions.assertNotNull(traversalResult);
            Assertions.assertEquals(2, traversalResult.size());// Live and working folders
            Assertions.assertEquals(0, traversalResult.get(0).getLeft().size());// No errors should be found
            Assertions.assertEquals(0, traversalResult.get(1).getLeft().size());// No errors should be found

            var treeNode = traversalResult.get(0).getRight();
            var treeNodePushInfo = treeNode.collectTreeNodePushInfo();

            // Should be nothing to push as we are pushing the same folder we pull
            Assertions.assertEquals(5, treeNodePushInfo.assetsToPushCount());
            Assertions.assertEquals(5, treeNodePushInfo.assetsNewCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsModifiedCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsToDeleteCount());
            Assertions.assertEquals(9, treeNodePushInfo.foldersToPushCount());
            Assertions.assertEquals(0, treeNodePushInfo.foldersToDeleteCount());

            pushService.processTreeNodes(outputOptions, tempFolder.toAbsolutePath().toString(),
                    traversalResult.get(0).getMiddle(), traversalResult.get(0).getRight(), treeNodePushInfo,
                    true, 0);

            // ---
            // Validate the new site was created
            var newSiteResults = remoteTraversalService.traverseRemoteFolder(
                    String.format("//%s", newSiteName),
                    null,
                    true,
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>()
            );
            var newSiteTreeNode = newSiteResults.getRight();

            //Validating the tree
            // subFolder1-1-1 (has 2 asset)
            Assertions.assertEquals(2, newSiteTreeNode.children().get(0).children().get(0).children().get(0).assets().size());
            // subFolder2-1-1 (has 1 asset)
            Assertions.assertEquals(1, newSiteTreeNode.children().get(1).children().get(0).children().get(0).assets().size());
            // Folder 3 (has 1 asset)
            Assertions.assertEquals(1, newSiteTreeNode.children().get(2).assets().size());

        } finally {
            // Clean up the temporal folder
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method tests the scenario where modified data is pushed. It creates a temporal folder
     * for the pull operation, prepares the test data, performs a pull operation, modifies the
     * pulled data, pushes the modified content, and validates that the modifications were pushed
     * correctly.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void Test_Push_Modified_Data() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = prepareData();

            final var folderPath = String.format("//%s", testSiteName);

            var result = remoteTraversalService.traverseRemoteFolder(
                    folderPath,
                    null,
                    true,
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>()
            );

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();
            pullService.pullTree(outputOptions, result.getRight(), workspace.files().toAbsolutePath().toFile(),
                    true, true, true, 0);

            // --
            // Modifying the pulled data
            // Removing a folder
            var folderToRemove = workspace.files().toAbsolutePath() + "/live/en-us/" + testSiteName + "/folder3";
            FileUtils.deleteDirectory(new File(folderToRemove));

            // Removing an asset
            var assetToRemove = workspace.files().toAbsolutePath() + "/live/en-us/" + testSiteName +
                    "/folder2/subfolder2-1/subfolder2-1-1/image2.png";
            //FileUtils.delete(new File(assetToRemove));
/*
            // Modifying an asset
            var toModifyAsset = workspace.files().toAbsolutePath() + "/live/en-us/" + testSiteName +
                    "/folder1/subFolder1-1/subFolder1-1-1/image1.png";
            FileUtils.delete(new File(toModifyAsset));
            try (InputStream inputStream = getClass().getResourceAsStream(
                    String.format("/%s", "image 3.png"))) {
                FileUtils.copyInputStreamToFile(inputStream, new File(toModifyAsset));
            }

            // Create a new folder and asset
            var newFolder = workspace.files().toAbsolutePath() + "/live/en-us/" + testSiteName +
                    "/folder5/subFolder5-1/subFolder5-1-1";
            FileUtils.forceMkdir(new File(newFolder));
            try (InputStream inputStream = getClass().getResourceAsStream(String.format("/%s", "image2.png"))) {
                FileUtils.copyInputStreamToFile(inputStream, new File(newFolder + "/image2.png"));
            }

            // Create a second new folder and asset, the folder contains a space in the name
            var newFolderWithSpace =
                    workspace.files().toAbsolutePath() + "/live/en-us/" + testSiteName +
                            "/folder6 withSpace/subFolder6-1/subFolder6-1-1";
            FileUtils.forceMkdir(new File(newFolderWithSpace));
            try (InputStream inputStream = getClass().getResourceAsStream(
                    String.format("/%s", "image5.jpg"))) {
                FileUtils.copyInputStreamToFile(inputStream,
                        new File(newFolderWithSpace + "/image5.jpg"));
            }

            // ---
            // Now we are going to push the content
            var traversalResult = pushService.traverseLocalFolders(outputOptions, tempFolder.toFile(), tempFolder.toFile(),
                    true, true, true, true);

            Assertions.assertNotNull(traversalResult);
            Assertions.assertEquals(2, traversalResult.size());// Live and working folders
            Assertions.assertEquals(0, traversalResult.get(0).getLeft().size());// No errors should be found
            Assertions.assertEquals(0, traversalResult.get(1).getLeft().size());// No errors should be found

            var treeNode = traversalResult.get(0).getRight();
            var treeNodePushInfo = treeNode.collectTreeNodePushInfo();

            // Should be nothing to push as we are pushing the same folder we pull
            Assertions.assertEquals(3, treeNodePushInfo.assetsToPushCount());
            Assertions.assertEquals(2, treeNodePushInfo.assetsNewCount());
            Assertions.assertEquals(1, treeNodePushInfo.assetsModifiedCount());
            Assertions.assertEquals(2, treeNodePushInfo.assetsToDeleteCount());
            Assertions.assertEquals(6, treeNodePushInfo.foldersToPushCount());
            Assertions.assertEquals(1, treeNodePushInfo.foldersToDeleteCount());

            pushService.processTreeNodes(outputOptions, tempFolder.toAbsolutePath().toString(),
                    traversalResult.get(0).getMiddle(), traversalResult.get(0).getRight(), treeNodePushInfo,
                    true, 0);

            // ---
            // Validate we pushed the data properly
            try {
                // Sleep for 5 seconds to give time to the server to process the push (indices)
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Assertions.fail(e.getMessage());
            }
            var updatedResults = remoteTraversalService.traverseRemoteFolder(
                    folderPath,
                    null,
                    true,
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>()
            );
            var updatedTreeNode = updatedResults.getRight();

            //Validating the tree
            // subFolder1-1-1 (has 2 asset)
            Assertions.assertEquals(2, updatedTreeNode.children().get(0).children().get(0).children().get(0).assets().size());
            // subFolder2-1-1 (has 0 asset)
            Assertions.assertEquals(0, updatedTreeNode.children().get(1).children().get(0).children().get(0).assets().size());
            // Folder 4 withSpace (has 1 asset)
            Assertions.assertEquals(1, updatedTreeNode.children().get(2).assets().size());
            // Folder 5 (has 1 asset)
            Assertions.assertEquals(1,
                    updatedTreeNode.children().get(3).children().get(0).children().get(0).assets()
                            .size());
            // Folder 5 withSpace (has 1 asset)
            Assertions.assertEquals(1,
                    updatedTreeNode.children().get(4).children().get(0).children().get(0).assets()
                            .size());

            // Make sure folder 3 was deleted
            for (var child : updatedTreeNode.children()) {
                Assertions.assertNotEquals("folder3", child.folder().name());
            }
*/
        } finally {
            // Clean up the temporal folder
            deleteTempDirectory(tempFolder);
        }
    }

}
