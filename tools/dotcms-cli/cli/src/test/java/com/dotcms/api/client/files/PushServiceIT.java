package com.dotcms.api.client.files;

import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_EMPTY_FOLDERS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.NON_RECURSIVE;
import static com.dotcms.api.client.pull.file.OptionConstants.PRESERVE;
import static com.dotcms.common.AssetsUtils.buildRemoteAssetURL;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.files.traversal.PushTraverseParams;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.api.client.pull.PullService;
import com.dotcms.api.client.pull.file.FileFetcher;
import com.dotcms.api.client.pull.file.FilePullHandler;
import com.dotcms.cli.command.PushContext;
import com.dotcms.cli.common.FilesTestHelperService;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.pull.PullOptions;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the PushService class.
 */
@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class PushServiceIT {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    ServiceManager serviceManager;

    @Inject
    RemoteTraversalService remoteTraversalService;

    @Inject
    PullService pullService;

    @Inject
    FileFetcher fileProvider;

    @Inject
    FilePullHandler filePullHandler;

    @Inject
    PushService pushService;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    PushContext pushContext;

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
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = filesTestHelper.prepareData();

            final var folderPath = String.format("//%s", testSiteName);

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();

            Map<String, Object> customOptions = Map.of(
                    INCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    INCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    EXCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    EXCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    NON_RECURSIVE, false,
                    PRESERVE, false,
                    INCLUDE_EMPTY_FOLDERS, true
            );

            // Execute the pull
            pullService.pull(
                    PullOptions.builder().
                            destination(workspace.files().toAbsolutePath().toFile()).
                            contentKey(folderPath).
                            isShortOutput(false).
                            failFast(true).
                            maxRetryAttempts(0).
                            customOptions(customOptions).
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler
            );

            // ---
            // Now we are going to push the content
            var traverseResults = pushService.traverseLocalFolders(outputOptions, tempFolder.toFile(), tempFolder.toFile(),
                    true, true, true, true);

            Assertions.assertNotNull(traverseResults);
            Assertions.assertEquals(2, traverseResults.size());// Live and working folders
            Assertions.assertTrue( traverseResults.get(0).exceptions().isEmpty());// No errors should be found
            Assertions.assertTrue( traverseResults.get(1).exceptions().isEmpty());// No errors should be found

            var treeNode = traverseResults.get(0).treeNode();
            var treeNodePushInfo = treeNode.collectPushInfo();

            // Should be nothing to push as we are pushing the same folder we pull
            Assertions.assertEquals(0, treeNodePushInfo.assetsToPushCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsNewCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsModifiedCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsToDeleteCount());
            Assertions.assertEquals(0, treeNodePushInfo.foldersToPushCount());
            Assertions.assertEquals(0, treeNodePushInfo.foldersToDeleteCount());

            pushService.processTreeNodes(outputOptions, treeNodePushInfo,
                    PushTraverseParams.builder()
                            .workspacePath(tempFolder.toFile().getAbsolutePath())
                            .localPaths(traverseResults.get(0).localPaths())
                            .rootNode(traverseResults.get(0).treeNode())
                            .failFast(true)
                            .maxRetryAttempts(0)
                            .pushContext(pushContext)
                            .build());
        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
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
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = filesTestHelper.prepareData();

            final var folderPath = String.format("//%s", testSiteName);

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();

            Map<String, Object> customOptions = Map.of(
                    INCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    INCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    EXCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    EXCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    NON_RECURSIVE, false,
                    PRESERVE, false,
                    INCLUDE_EMPTY_FOLDERS, true
            );

            // Execute the pull
            pullService.pull(
                    PullOptions.builder().
                            destination(workspace.files().toAbsolutePath().toFile()).
                            contentKey(folderPath).
                            isShortOutput(false).
                            failFast(true).
                            maxRetryAttempts(0).
                            customOptions(customOptions).
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler
            );

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
            var traverseResults = pushService.traverseLocalFolders(outputOptions, tempFolder.toFile(), tempFolder.toFile(),
                    true, true, true, true);

            Assertions.assertNotNull(traverseResults);
            Assertions.assertEquals(2, traverseResults.size());// Live and working folders
            // Validating the processing order
            Assertions.assertEquals(
                    "live",
                    traverseResults.get(0).localPaths().status()
            );
            Assertions.assertEquals(
                    "working",
                    traverseResults.get(1).localPaths().status()
            );
            // Validating no errors were found
            Assertions.assertTrue(traverseResults.get(0).exceptions().isEmpty());// No errors should be found
            Assertions.assertTrue(traverseResults.get(1).exceptions().isEmpty());// No errors should be found

            var treeNode = traverseResults.get(0).treeNode();
            var treeNodePushInfo = treeNode.collectPushInfo();

            // Should be nothing to push as we are pushing the same folder we pull
            Assertions.assertEquals(7, treeNodePushInfo.assetsToPushCount());
            Assertions.assertEquals(7, treeNodePushInfo.assetsNewCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsModifiedCount());
            Assertions.assertEquals(0, treeNodePushInfo.assetsToDeleteCount());
            Assertions.assertEquals(9, treeNodePushInfo.foldersToPushCount());
            Assertions.assertEquals(0, treeNodePushInfo.foldersToDeleteCount());

            pushService.processTreeNodes(outputOptions, treeNodePushInfo,
                    PushTraverseParams.builder()
                            .workspacePath(tempFolder.toFile().getAbsolutePath())
                            .localPaths(traverseResults.get(0).localPaths())
                            .rootNode(traverseResults.get(0).treeNode())
                            .failFast(true)
                            .maxRetryAttempts(0)
                            .pushContext(pushContext)
                            .build());

            // Validate some pushed data, giving some time to the system to index the new data
            indexCheckAndWait(newSiteName,
                    "/folder1/subFolder1-1/subFolder1-1-1",
                    "image1.png");

            // ---
            // Validate we pushed the data properly
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

            Assertions.assertEquals(4, newSiteTreeNode.children().size());

            // Sorting the children to make the test deterministic
            newSiteTreeNode.sortChildren();
            newSiteTreeNode.children().get(0).sortChildren();
            newSiteTreeNode.children().get(1).sortChildren();
            newSiteTreeNode.children().get(2).sortChildren();

            //Validating the tree
            // subFolder1-1-1 (has 2 asset)
            Assertions.assertEquals(2, newSiteTreeNode.children().get(0).children().get(0).children().get(0).assets().size());
            // subFolder2-1-1-子資料夾 (has 3 asset)
            Assertions.assertEquals(3,
                    newSiteTreeNode.children().get(1).
                            children().get(0).children().get(0).assets().size());
            // Folder 3 (has 1 asset)
            Assertions.assertEquals(1, newSiteTreeNode.children().get(2).assets().size());

        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
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
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = filesTestHelper.prepareData();

            final var folderPath = String.format("//%s", testSiteName);

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();
            final Path absolutePath = workspace.files().toAbsolutePath();

            Map<String, Object> customOptions = Map.of(
                    INCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    INCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    EXCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    EXCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    NON_RECURSIVE, false,
                    PRESERVE, false,
                    INCLUDE_EMPTY_FOLDERS, true
            );

            // Execute the pull
            pullService.pull(
                    PullOptions.builder().
                            destination(absolutePath.toFile()).
                            contentKey(folderPath).
                            isShortOutput(false).
                            failFast(true).
                            maxRetryAttempts(0).
                            customOptions(customOptions).
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler
            );

            // --
            // Modifying the pulled data
            // Removing the folder under live
            Path liveFolderToRemove = Paths.get(absolutePath.toString(),"live","en-us",testSiteName,"folder3");

            //The folder also needs to be removed from the working branch of the folders tree. So it get removed
            //If we leave folders hanging under a different language or status the system isn't going to know what folder must be kept and what folders needs to be removed
            //If we want a folder to be removed from the remote instance it needs to be removed from all our folder branches for good
            Path workingFolderToRemove = Paths.get(absolutePath.toString(),"working","en-us",testSiteName,"folder3");
            // Removing an asset
            Path assetToRemove = Paths.get(absolutePath.toString(), "live", "en-us", testSiteName,
                    "folder2", "subFolder2-1", "subFolder2-1-1-子資料夾", "image2.png");

            FileUtils.deleteDirectory(liveFolderToRemove.toFile());
            FileUtils.deleteDirectory(workingFolderToRemove.toFile());
            FileUtils.delete(assetToRemove.toFile());

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
            var traverseResults = pushService.traverseLocalFolders(outputOptions, tempFolder.toFile(), tempFolder.toFile(),
                    true, true, true, true);

            Assertions.assertNotNull(traverseResults);
            Assertions.assertEquals(2, traverseResults.size());// Live and working folders
            Assertions.assertTrue(traverseResults.get(0).exceptions().isEmpty());// No errors should be found
            Assertions.assertTrue(traverseResults.get(1).exceptions().isEmpty());// No errors should be found

            var treeNode = traverseResults.get(0).treeNode();
            var treeNodePushInfo = treeNode.collectPushInfo();

            // Should be nothing to push as we are pushing the same folder we pull
            Assertions.assertEquals(3, treeNodePushInfo.assetsToPushCount());
            Assertions.assertEquals(2, treeNodePushInfo.assetsNewCount());
            Assertions.assertEquals(1, treeNodePushInfo.assetsModifiedCount());
            Assertions.assertEquals(2, treeNodePushInfo.assetsToDeleteCount());
            Assertions.assertEquals(6, treeNodePushInfo.foldersToPushCount());
            Assertions.assertEquals(1, treeNodePushInfo.foldersToDeleteCount());

            pushService.processTreeNodes(outputOptions, treeNodePushInfo,
                    PushTraverseParams.builder()
                            .workspacePath(tempFolder.toFile().getAbsolutePath())
                            .localPaths(traverseResults.get(0).localPaths())
                            .rootNode(traverseResults.get(0).treeNode())
                            .failFast(true)
                            .maxRetryAttempts(0)
                            .pushContext(pushContext)
                            .build());

            // Validate some pushed data, giving some time to the system to index the new data
            indexCheckAndWait(testSiteName,
                    "/folder5/subFolder5-1/subFolder5-1-1",
                    "image2.png");

            // ---
            // Validate we pushed the data properly
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

            Assertions.assertEquals(5, updatedTreeNode.children().size());

            // Sorting the children to make the test deterministic
            updatedTreeNode.sortChildren();
            updatedTreeNode.children().get(0).sortChildren();
            updatedTreeNode.children().get(1).sortChildren();
            updatedTreeNode.children().get(2).sortChildren();
            updatedTreeNode.children().get(3).sortChildren();
            updatedTreeNode.children().get(4).sortChildren();

            //Validating the tree
            // subFolder1-1-1 (has 2 asset)
            Assertions.assertEquals(2, updatedTreeNode.children().get(0).children().get(0).children().get(0).assets().size());
            // subFolder2-1-1-子資料夾 (has 2 assets) -> 1 was removed
            Assertions.assertEquals(2, updatedTreeNode.children().
                    get(1).children().get(0).children().get(0).assets().size());
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

        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Initially our code would explore the file system looking for folders that would exist remotely but not locally marking them as "to be deleted"
     * But it wouldn't take into account that the same folder could also be represented under different status or language see <a href="https://github.com/dotCMS/core/issues/26380">Bug</a>
     * So Given that the same folder can live under different lang/status branch. If we really want to delete it from the remote instance should imply we remove everywhere.
     * Expected Result: If the folder only gets removed from under "live" it shouldn't get marked for delete.
     * If the real intend if really removing the folder remotely. The folder needs to me removed  also from the "working" tree nodes branch
     * @throws IOException
     */
    @Test
    void Test_Delete_Folder() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = filesTestHelper.prepareData();

            final var folderPath = String.format("//%s", testSiteName);

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();
            final Path absolutePath = workspace.files().toAbsolutePath();

            Map<String, Object> customOptions = Map.of(
                    INCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    INCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    EXCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    EXCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    NON_RECURSIVE, false,
                    PRESERVE, false,
                    INCLUDE_EMPTY_FOLDERS, true
            );

            // Execute the pull
            pullService.pull(
                    PullOptions.builder().
                            destination(workspace.files().toAbsolutePath().toFile()).
                            contentKey(folderPath).
                            isShortOutput(false).
                            failFast(true).
                            maxRetryAttempts(0).
                            customOptions(customOptions).
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler
            );

            Files.find(absolutePath, Integer.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .forEach(System.out::println);


            Path liveFolderToRemove = Paths.get(absolutePath.toString(),"live","en-us",testSiteName,"folder3");
            Path workingFolderToRemove = Paths.get(absolutePath.toString(),"working","en-us",testSiteName,"folder3");

            // Here's where the actual test takes place:
            // So in order to delete the folder it  needs to be physically removed from the two locations where it appears
            // Otherwise the cli does not understand there's intention for remove

            FileUtils.deleteDirectory(liveFolderToRemove.toFile());

            // Now we are going to push the content
            var traversalResultLiveRemoved = pushService.traverseLocalFolders(outputOptions, tempFolder.toFile(), tempFolder.toFile(),
                    true, true, true, true);

            var treeNode1 = traversalResultLiveRemoved.get(0).treeNode();
            var treeNodePushInfo1 = treeNode1.collectPushInfo();

            //This is zero because there is still another folder hanging under the "working"  branch which needs to be removed
            Assertions.assertEquals(0, treeNodePushInfo1.foldersToDeleteCount());
            // so let's do it
            FileUtils.deleteDirectory(workingFolderToRemove.toFile());

            // Push again after deleting the working folder too
            var traversalResultWorkingRemoved = pushService.traverseLocalFolders(outputOptions, tempFolder.toFile(), tempFolder.toFile(),
                    true, true, true, true);

            var treeNode2 = traversalResultWorkingRemoved.get(0).treeNode();
            var treeNodePushInfo2 = treeNode2.collectPushInfo();

            //Now we should expect this to be 1, because both folder are removed
            Assertions.assertEquals(1, treeNodePushInfo2.foldersToDeleteCount());

        } finally {
            filesTestHelper.deleteTempDirectory(tempFolder);
        }
    }

            /**
             * This method checks and waits for the indexing of a specific asset in a given folder of a
             * site. It validates the existence of the site, folder, and asset and waits for a specified
             * time period for the system to index the new data.
             *
             * @param siteName   the name of the site
             * @param folderPath the path of the folder where the asset is located
             * @param assetName  the name of the asset
             */
    private void indexCheckAndWait(final String siteName, final String folderPath,
            final String assetName) {

        // Validate some pushed data, giving some time to the system to index the new data
        Assertions.assertTrue(filesTestHelper.siteExist(siteName),
                String.format("Site %s was not created", siteName));

        // Building the remote asset path
        final var remoteAssetPath = buildRemoteAssetURL(siteName, folderPath, assetName);
        Assertions.assertTrue(filesTestHelper.assetExist(remoteAssetPath),
                String.format("Asset %s was not created", remoteAssetPath));
    }

}
