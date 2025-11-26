package com.dotcms.api.client.files;

import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_EMPTY_FOLDERS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.NON_RECURSIVE;
import static com.dotcms.api.client.pull.file.OptionConstants.PRESERVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.api.client.pull.PullService;
import com.dotcms.api.client.pull.file.FileFetcher;
import com.dotcms.api.client.pull.file.FilePullHandler;
import com.dotcms.api.client.pull.file.FileTraverseResult;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.FilesTestHelperService;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.SitesTestHelperService;
import com.dotcms.cli.exception.ForceSilentExitException;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.pull.PullOptions;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class PullServiceIT {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    ServiceManager serviceManager;

    @Inject
    PullService pullService;

    @Inject
    FileFetcher fileProvider;

    @Inject
    FilePullHandler filePullHandler;

    @Inject
    FilesTestHelperService filesTestHelper;

    @Inject
    SitesTestHelperService sitesTestHelper;

    @Inject
    WorkspaceManager workspaceManager;

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
     * This method is used to test the validation of a file system. It creates a temporary folder
     * for the pull and prepares the data for the test. It then traverses all existing sites and
     * pulls the content. After pulling the content, it validates the file system by comparing the
     * actual folders and files against the expected ones. At the end, it cleans up the temporary
     * folder.
     *
     * @throws IOException if there is an error creating the temporary folder or deleting it
     */
    @Test
    void Test_Sites_Check() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName1 = filesTestHelper.prepareData();
            final var testSiteName2 = filesTestHelper.prepareData();
            final var testSiteName3 = filesTestHelper.prepareData();

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
                            isShortOutput(false).
                            failFast(true).
                            maxRetryAttempts(0).
                            customOptions(customOptions).
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler
            );

            class Validator {

                void validateSite(final Path tempFolder, final String testSiteName)
                        throws IOException {

                    // Get the list of folders created inside the temporal folder
                    List<String> collectedFolders = new ArrayList<>();
                    List<String> collectedFiles = new ArrayList<>();

                    try (final Stream<Path> walk = Files.walk(tempFolder)) {
                        walk.filter(Files::isDirectory)
                                .forEach(path -> collectedFolders.add(
                                        tempFolder.relativize(path).toString()));
                    }
                    try (final Stream<Path> walk = Files.walk(tempFolder)) {
                        walk.filter(Files::isRegularFile)
                                .forEach(path -> collectedFiles.add(
                                        tempFolder.relativize(path).toString()
                                ));
                    }
                    //Let's remove the workspace folder from the list
                    List<String> existingFolders = collectedFolders.stream()
                            .map(folder -> folder.replaceAll(
                                    "^dot-workspace-\\d+/", "")).collect(Collectors.toList());
                    List<String> existingFiles = collectedFiles.stream()
                            .map(folder -> folder.replaceAll(
                                    "^dot-workspace-\\d+/", "")).collect(Collectors.toList());

                    var basePath = "files/live/en-us/" + testSiteName;
                    // Expected folder structure based on the treeNode object
                    Map<String, List<String>> expectedFolders = Map.ofEntries(
                            Map.entry(basePath,
                                    Arrays.asList("folder1", "folder2", "folder3",
                                            "folder4 withSpace")),
                            Map.entry(basePath + "/folder1",
                                    Arrays.asList("subFolder1-1", "subFolder1-2", "subFolder1-3")),
                            Map.entry(basePath + "/folder1/subFolder1-1",
                                    Arrays.asList("subFolder1-1-1", "subFolder1-1-2",
                                            "subFolder1-1-3")),
                            Map.entry(basePath + "/folder1/subFolder1-2",
                                    Arrays.asList("subFolder1-2-1", "subFolder1-2-2",
                                            "subFolder1-2-3")),
                            Map.entry(basePath + "/folder1/subFolder1-3",
                                    Collections.emptyList()),
                            Map.entry(basePath + "/folder2",
                                    Arrays.asList("subFolder2-1", "subFolder2-2", "subFolder2-3")),
                            Map.entry(basePath + "/folder2/subFolder2-1",
                                    Arrays.asList("subFolder2-1-1-子資料夾", "subFolder2-1-2",
                                            "subFolder2-1-3")),
                            Map.entry(basePath + "/folder2/subFolder2-2",
                                    Collections.emptyList()),
                            Map.entry(basePath + "/folder2/subFolder2-3",
                                    Collections.emptyList()),
                            Map.entry(basePath + "/folder3",
                                    Collections.emptyList()),
                            Map.entry(basePath + "/folder4 withSpace",
                                    Collections.emptyList())
                    );

                    // Expected folder structure based on the treeNode object
                    Map<String, List<String>> expectedFiles = Map.of(
                            basePath, Collections.emptyList(),
                            basePath + "/folder1/subFolder1-1/subFolder1-1-1",
                            Arrays.asList("image1.png", "image4.jpg"),
                            basePath + "/folder2/subFolder2-1/subFolder2-1-1-子資料夾",
                            Arrays.asList("image2.png", "這就是我的想像6.png", "image (7)+.png"),
                            basePath + "/folder3", Arrays.asList("image 3.png"),
                            basePath + "/folder4 withSpace", Arrays.asList("image5.jpg")
                    );

                    // Validate the actual folders against the expected folders
                    for (Map.Entry<String, List<String>> entry : expectedFolders.entrySet()) {

                        String parentPath = entry.getKey();
                        List<String> expectedSubFolders = entry.getValue();

                        // Get the actual subfolders under the parent path
                        List<String> existingSubFolders = existingFolders.stream().
                                filter(folder ->
                                        folder.startsWith(parentPath) &&
                                                folder.substring(parentPath.length())
                                                        .split("/").length == 2
                                ).
                                map(folder -> folder.substring(parentPath.length() + 1)).
                                collect(Collectors.toList());

                        // Validate the actual subfolders against the expected subfolders
                        Assertions.assertEquals(expectedSubFolders.size(),
                                existingSubFolders.size());
                        Assertions.assertTrue(existingSubFolders.containsAll(expectedSubFolders));
                    }

                    // Validate the actual files against the expected assets
                    for (Map.Entry<String, List<String>> entry : expectedFiles.entrySet()) {

                        final var parentPath = entry.getKey();
                        var expectedFilesList = entry.getValue();

                        // Get the actual files under the parent path
                        List<String> existingFilesList = existingFiles.stream().
                                filter(file ->
                                        file.startsWith(parentPath) &&
                                                file.substring(parentPath.length())
                                                        .split("/").length == 2
                                ).
                                map(file -> file.substring(parentPath.length() + 1)).
                                collect(Collectors.toList());

                        // Ordering expectedFilesList and existingFilesList
                        Collections.sort(expectedFilesList);
                        Collections.sort(existingFilesList);

                        // Validate the actual files against the expected files
                        Assertions.assertEquals(expectedFilesList.size(), existingFilesList.size());
                        Assertions.assertTrue(existingFilesList.containsAll(expectedFilesList));
                    }
                }
            }

            // ============================
            //Validating the file system
            // ============================
            Validator validator = new Validator();
            validator.validateSite(tempFolder, testSiteName1);
            validator.validateSite(tempFolder, testSiteName2);
            validator.validateSite(tempFolder, testSiteName3);

        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method is used to test the validation of a file system. It creates a temporary folder
     * for the pull and prepares the data for the test. It then traverses a remote folder and pulls
     * the content. After pulling the content, it validates the file system by comparing the actual
     * folders and files against the expected ones. At the end, it cleans up the temporary folder.
     *
     * @throws IOException if there is an error creating the temporary folder or deleting it
     */
    @Test
    void Test_Folders_Check() throws IOException {

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

            // ============================
            //Validating the file system
            // ============================
            // Get the list of folders created inside the temporal folder
            List<String> collectedFolders = new ArrayList<>();
            List<String> collectedFiles = new ArrayList<>();

            try (final Stream<Path> walk = Files.walk(tempFolder)) {
                walk.filter(Files::isDirectory)
                        .forEach(path -> collectedFolders.add(tempFolder.relativize(path).toString()));
            }
            try (final Stream<Path> walk = Files.walk(tempFolder)) {
                walk.filter(Files::isRegularFile)
                        .forEach(
                                path -> collectedFiles.add(tempFolder.relativize(path).toString()));
            }
            //Let's remove the workspace folder from the list
            List<String> existingFolders = collectedFolders.stream().map(folder -> folder.replaceAll(
                    "^dot-workspace-\\d+/", "")).collect(Collectors.toList());
            List<String> existingFiles = collectedFiles.stream().map(folder -> folder.replaceAll(
                    "^dot-workspace-\\d+/", "")).collect(Collectors.toList());

            var basePath = "files/live/en-us/" + testSiteName;
            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFolders = Map.ofEntries(
                    Map.entry(basePath,
                            Arrays.asList("folder1", "folder2", "folder3", "folder4 withSpace")),
                    Map.entry(basePath + "/folder1",
                            Arrays.asList("subFolder1-1", "subFolder1-2", "subFolder1-3")),
                    Map.entry(basePath + "/folder1/subFolder1-1",
                            Arrays.asList("subFolder1-1-1", "subFolder1-1-2", "subFolder1-1-3")),
                    Map.entry(basePath + "/folder1/subFolder1-2",
                            Arrays.asList("subFolder1-2-1", "subFolder1-2-2", "subFolder1-2-3")),
                    Map.entry(basePath + "/folder1/subFolder1-3",
                            Collections.emptyList()),
                    Map.entry(basePath + "/folder2",
                            Arrays.asList("subFolder2-1", "subFolder2-2", "subFolder2-3")),
                    Map.entry(basePath + "/folder2/subFolder2-1",
                            Arrays.asList("subFolder2-1-1-子資料夾", "subFolder2-1-2",
                                    "subFolder2-1-3")),
                    Map.entry(basePath + "/folder2/subFolder2-2",
                            Collections.emptyList()),
                    Map.entry(basePath + "/folder2/subFolder2-3",
                            Collections.emptyList()),
                    Map.entry(basePath + "/folder3",
                            Collections.emptyList()),
                    Map.entry(basePath + "/folder4 withSpace",
                            Collections.emptyList())
            );

            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFiles = Map.of(
                    basePath, Collections.emptyList(),
                    basePath + "/folder1/subFolder1-1/subFolder1-1-1", Arrays.asList("image1.png", "image4.jpg"),
                    basePath + "/folder2/subFolder2-1/subFolder2-1-1-子資料夾",
                    Arrays.asList("image2.png", "這就是我的想像6.png", "image (7)+.png"),
                    basePath + "/folder3", Arrays.asList("image 3.png"),
                    basePath + "/folder4 withSpace", Arrays.asList("image5.jpg")
            );

            // Validate the actual folders against the expected folders
            for (Map.Entry<String, List<String>> entry : expectedFolders.entrySet()) {

                String parentPath = entry.getKey();
                List<String> expectedSubFolders = entry.getValue();

                // Get the actual subfolders under the parent path
                List<String> existingSubFolders = existingFolders.stream().
                        filter(folder ->
                                folder.startsWith(parentPath) &&
                                        folder.substring(parentPath.length()).split("/").length == 2
                        ).
                        map(folder -> folder.substring(parentPath.length() + 1)).
                        collect(Collectors.toList());

                // Validate the actual subfolders against the expected subfolders
                Assertions.assertEquals(expectedSubFolders.size(), existingSubFolders.size());
                Assertions.assertTrue(existingSubFolders.containsAll(expectedSubFolders));
            }

            // Validate the actual files against the expected assets
            for (Map.Entry<String, List<String>> entry : expectedFiles.entrySet()) {

                final var parentPath = entry.getKey();
                var expectedFilesList = entry.getValue();

                // Get the actual files under the parent path
                List<String> existingFilesList = existingFiles.stream().
                        filter(file ->
                                file.startsWith(parentPath) &&
                                        file.substring(parentPath.length()).split("/").length == 2
                        ).
                        map(file -> file.substring(parentPath.length() + 1)).
                        collect(Collectors.toList());

                // Ordering expectedFilesList and existingFilesList
                Collections.sort(expectedFilesList);
                Collections.sort(existingFilesList);

                // Validate the actual files against the expected files
                Assertions.assertEquals(expectedFilesList.size(), existingFilesList.size());
                Assertions.assertTrue(existingFilesList.containsAll(expectedFilesList));
            }

        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method is used to test the validation of a file system for checking a specific asset
     * pull, specifically, an asset with spaces in its name.
     * <p>
     * It creates a temporary folder for the pull and prepares the data for the test. It then
     * traverses a remote folder and pulls the content of an asset. After pulling the content, it
     * validates the file system by comparing the actual folders and files against the expected
     * ones. At the end, it cleans up the temporary folder.
     *
     * @throws IOException if there is an error creating the temporary folder or deleting it
     */
    @Test
    void Test_Asset_Check() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = filesTestHelper.prepareData();

            final var folderPath = String.format("//%s/folder3/image 3.png", testSiteName);

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

            // ============================
            //Validating the file system
            // ============================
            // Get the list of folders created inside the temporal folder
            List<String> collectedFolders = new ArrayList<>();
            List<String> collectedFiles = new ArrayList<>();

            try (final Stream<Path> walk = Files.walk(tempFolder)) {
                walk.filter(Files::isDirectory)
                        .forEach(path -> collectedFolders.add(
                                tempFolder.relativize(path).toString()));
            }
            try (final Stream<Path> walk = Files.walk(tempFolder)) {
                walk.filter(Files::isRegularFile)
                        .forEach(
                                path -> collectedFiles.add(tempFolder.relativize(path).toString())
                        );
            }
            //Let's remove the workspace folder from the list
            List<String> existingFolders = collectedFolders.stream()
                    .map(folder -> folder.replaceAll(
                            "^dot-workspace-\\d+/", "")).collect(Collectors.toList());
            List<String> existingFiles = collectedFiles.stream().map(folder -> folder.replaceAll(
                    "^dot-workspace-\\d+/", "")).collect(Collectors.toList());

            var basePath = "files/live/en-us/" + testSiteName;
            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFolders = Map.of(
                    basePath, Arrays.asList("folder3"),
                    basePath + "/folder3", Collections.emptyList()
            );

            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFiles = Map.of(
                    basePath, Collections.emptyList(),
                    basePath + "/folder3", Arrays.asList("image 3.png")
            );

            // Validate the actual folders against the expected folders
            for (Map.Entry<String, List<String>> entry : expectedFolders.entrySet()) {

                String parentPath = entry.getKey();
                List<String> expectedSubFolders = entry.getValue();

                // Get the actual subfolders under the parent path
                List<String> existingSubFolders = existingFolders.stream().
                        filter(folder ->
                                folder.startsWith(parentPath) &&
                                        folder.substring(parentPath.length()).split("/").length == 2
                        ).
                        map(folder -> folder.substring(parentPath.length() + 1)).
                        collect(Collectors.toList());

                // Validate the actual subfolders against the expected subfolders
                Assertions.assertEquals(expectedSubFolders.size(), existingSubFolders.size());
                Assertions.assertTrue(existingSubFolders.containsAll(expectedSubFolders));
            }

            // Validate the actual files against the expected assets
            for (Map.Entry<String, List<String>> entry : expectedFiles.entrySet()) {

                final var parentPath = entry.getKey();
                var expectedFilesList = entry.getValue();

                // Get the actual files under the parent path
                List<String> existingFilesList = existingFiles.stream().
                        filter(file ->
                                file.startsWith(parentPath) &&
                                        file.substring(parentPath.length()).split("/").length == 2
                        ).
                        map(file -> file.substring(parentPath.length() + 1)).
                        collect(Collectors.toList());

                // Ordering expectedFilesList and existingFilesList
                Collections.sort(expectedFilesList);
                Collections.sort(existingFilesList);

                // Validate the actual files against the expected files
                Assertions.assertEquals(expectedFilesList.size(), existingFilesList.size());
                Assertions.assertTrue(existingFilesList.containsAll(expectedFilesList));
            }

        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method is used to test the validation of a file system for checking a specific asset
     * pull.
     * <p>
     * It creates a temporary folder for the pull and prepares the data for the test. It then
     * traverses a remote folder and pulls the content of an asset. After pulling the content, it
     * validates the file system by comparing the actual folders and files against the expected
     * ones. At the end, it cleans up the temporary folder.
     *
     * @throws IOException if there is an error creating the temporary folder or deleting it
     */
    @Test
    void Test_Asset_Check2() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = filesTestHelper.prepareData();

            final var folderPath = String.format(
                    "//%s/folder2/subFolder2-1/subFolder2-1-1-子資料夾/image2.png", testSiteName);

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

            // ============================
            //Validating the file system
            // ============================
            // Get the list of folders created inside the temporal folder
            List<String> collectedFolders = new ArrayList<>();
            List<String> collectedFiles = new ArrayList<>();

            try (final Stream<Path> walk = Files.walk(tempFolder)) {
                walk.filter(Files::isDirectory)
                        .forEach(path -> collectedFolders.add(
                                tempFolder.relativize(path).toString()));
            }
            try (final Stream<Path> walk = Files.walk(tempFolder)) {
                walk.filter(Files::isRegularFile)
                        .forEach(
                                path -> collectedFiles.add(tempFolder.relativize(path).toString())
                        );
            }
            //Let's remove the workspace folder from the list
            List<String> existingFolders = collectedFolders.stream()
                    .map(folder -> folder.replaceAll(
                            "^dot-workspace-\\d+/", "")).collect(Collectors.toList());
            List<String> existingFiles = collectedFiles.stream().map(folder -> folder.replaceAll(
                    "^dot-workspace-\\d+/", "")).collect(Collectors.toList());

            var basePath = "files/live/en-us/" + testSiteName;
            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFolders = Map.of(
                    basePath, Arrays.asList("folder2"),
                    basePath + "/folder2", Arrays.asList("subFolder2-1"),
                    basePath + "/folder2/subFolder2-1", Arrays.asList("subFolder2-1-1-子資料夾")
            );

            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFiles = Map.of(
                    basePath, Collections.emptyList(),
                    basePath + "/folder2/subFolder2-1/subFolder2-1-1-子資料夾",
                    Arrays.asList("image2.png")
            );

            // Validate the actual folders against the expected folders
            for (Map.Entry<String, List<String>> entry : expectedFolders.entrySet()) {

                String parentPath = entry.getKey();
                List<String> expectedSubFolders = entry.getValue();

                // Get the actual subfolders under the parent path
                List<String> existingSubFolders = existingFolders.stream().
                        filter(folder ->
                                folder.startsWith(parentPath) &&
                                        folder.substring(parentPath.length()).split("/").length == 2
                        ).
                        map(folder -> folder.substring(parentPath.length() + 1)).
                        collect(Collectors.toList());

                // Validate the actual subfolders against the expected subfolders
                Assertions.assertEquals(expectedSubFolders.size(), existingSubFolders.size());
                Assertions.assertTrue(existingSubFolders.containsAll(expectedSubFolders));
            }

            // Validate the actual files against the expected assets
            for (Map.Entry<String, List<String>> entry : expectedFiles.entrySet()) {

                final var parentPath = entry.getKey();
                var expectedFilesList = entry.getValue();

                // Get the actual files under the parent path
                List<String> existingFilesList = existingFiles.stream().
                        filter(file ->
                                file.startsWith(parentPath) &&
                                        file.substring(parentPath.length()).split("/").length == 2
                        ).
                        map(file -> file.substring(parentPath.length() + 1)).
                        collect(Collectors.toList());

                // Ordering expectedFilesList and existingFilesList
                Collections.sort(expectedFilesList);
                Collections.sort(existingFilesList);

                // Validate the actual files against the expected files
                Assertions.assertEquals(expectedFilesList.size(), existingFilesList.size());
                Assertions.assertTrue(existingFilesList.containsAll(expectedFilesList));
            }

        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Tests the validation of pulling empty sites with using the empty folders flag as false
     * and true.
     *
     * @throws IOException if there is an error creating the temporary folder or deleting it
     */
    @Test
    void Test_Empty_Folders_Check() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Creating a site, for this test we don't need to create any content
            final var testSiteName = sitesTestHelper.createSiteOnServer().siteName();

            final var folderPath = String.format("//%s", testSiteName);

            OutputOptionMixin outputOptions = new MockOutputOptionMixin();

            // ===========================================================
            // Pulling the content with the include empty folders as false
            // ===========================================================
            Map<String, Object> customOptions = Map.of(
                    INCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    INCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    EXCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    EXCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    NON_RECURSIVE, false,
                    PRESERVE, false,
                    INCLUDE_EMPTY_FOLDERS, false
            );

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

            var filesSitePath = workspace.files().toAbsolutePath() + "/live/en-us/" + testSiteName;
            Path sitePath = Path.of(filesSitePath);

            // ============================
            // Validating the file system
            // ============================
            Assertions.assertFalse(Files.exists(sitePath));

            // Cleaning up the files folder
            filesTestHelper.deleteTempDirectory(workspace.files().toAbsolutePath());

            // ==============================================================
            // Now pulling the content with the include empty folders as true
            // ==============================================================
            customOptions = Map.of(
                    INCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    INCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    EXCLUDE_FOLDER_PATTERNS, new HashSet<>(),
                    EXCLUDE_ASSET_PATTERNS, new HashSet<>(),
                    NON_RECURSIVE, false,
                    PRESERVE, false,
                    INCLUDE_EMPTY_FOLDERS, true
            );

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

            // ============================
            // Validating the file system
            // ============================
            Assertions.assertTrue(Files.exists(sitePath));

        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Here we want to test fetchByKeys method see if the pullService still throws a ForceSilentExitException carrying the ExitCode.SOFTWARE
     * First we use a non-existent site to force an exception
     * Expected behavior: The pullService should throw a ForceSilentExitException with the ExitCode.SOFTWARE
     * @throws IOException if there is an error creating the temporary folder or deleting it
     */
    @Test
    void Test_Handle_Early_Thrown_Exception_Expect_SOFTWARE_ExitCode() throws IOException {
        Exception exception = null;
        // Creating a site, for this test we don't need to create any content
        final var testSiteName = String.format("non-existent-site-%s", UUID.randomUUID());
        final var folderPath = String.format("//%s", testSiteName);
        OutputOptionMixin outputOptions = new MockOutputOptionMixin();
        FileFetcher mockedProvider = Mockito.mock(FileFetcher.class);
        //But here is where the real test lies. We are going to return a List of FileTraverseResult with multiple exceptions
        //They all should be logged and the first one should be the one that triggers the exit code
        when(mockedProvider.fetchByKey(anyString(), anyBoolean(),any())).thenReturn(
                        FileTraverseResult.builder().tree(new TreeNode(FolderView.builder().host(testSiteName).path("/test").name("name").build())).exceptions(
                                List.of(
                                        new UnauthorizedException(),
                                        new TraversalTaskException("Unexpected Error Traversing folders ")
                                )).build()
                );

        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);
        try {
            pullService.pull(
                    PullOptions.builder().
                            destination(workspace.files().toAbsolutePath().toFile()).
                            //pullService must be called with the contentKey param to trigger the fetchByKeys method
                            contentKey(folderPath).
                            isShortOutput(false).
                            failFast(false).
                            maxRetryAttempts(0).
                            build(),
                    outputOptions,
                    mockedProvider,
                    filePullHandler
            );
        } catch (Exception e) {
               exception = e;
        } finally {
                // Clean up the temporal folder
                filesTestHelper.deleteTempDirectory(tempFolder);
        }
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception instanceof ForceSilentExitException);
        ForceSilentExitException forceSilentExitException = (ForceSilentExitException) exception;
        Assertions.assertEquals(ExitCode.SOFTWARE, forceSilentExitException.getExitCode());
    }


    /**
     * Given scenario: Similarly to the previous test, we want to test the fetch Method and see if the pullService still throws a ForceSilentExitException carrying the ExitCode.SOFTWARE
     * This time we're going to feed the service with a FieldFetcher that throws brings mixed elements of FileTraverseResult with exceptions and without exceptions
     * Expected behavior: The elements with exceptions should be written to the output and the first exception should be the one that triggers the exit code
     * @throws IOException if there is an error creating the temporary folder or deleting it
     */
    @Test
    void Test_Handle_Multiple_Thrown_Exceptions() throws IOException {
        Exception exception = null;
        // Creating a site, for this test we don't need to create any content
        final var testSiteName = String.format("any-site-%s", UUID.randomUUID());

        MockOutputOptionMixin outputOptions = new MockOutputOptionMixin();
        FileFetcher mockedProvider = Mockito.mock(FileFetcher.class);
        //But here is where the real test lies. We are going to return a List of FileTraverseResult with multiple exceptions
        //They all should be logged and the first one should be the one that triggers the exit code
        when(mockedProvider.fetch(anyBoolean(), any())).thenReturn(
                List.of(
                     FileTraverseResult.builder().tree(new TreeNode(FolderView.builder().host(testSiteName).path("/test").name("name").build())).exceptions(
                     List.of(
                             new UnauthorizedException(),
                             new TraversalTaskException("Unexpected Error Traversing folders ")
                     )).build()
                ));

       //We're going to use PullHandler but this time we do not specify the contentKey so the fetch method gets called
        var tempFolder = filesTestHelper.createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);
        try {
            pullService.pull(
                    PullOptions.builder().
                            destination(workspace.files().toAbsolutePath().toFile()).
                            isShortOutput(false).
                            failFast(false).
                            maxRetryAttempts(0).
                            build(),
                    outputOptions,
                    mockedProvider,
                    filePullHandler
            );
        } catch (Exception e) {
            exception = e;
        } finally {
            // Clean up the temporal folder
            filesTestHelper.deleteTempDirectory(tempFolder);
        }
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception instanceof ForceSilentExitException);
        ForceSilentExitException forceSilentExitException = (ForceSilentExitException) exception;
        //Test that the exit code is the one from the first exception thrown
        Assertions.assertEquals(ExitCode.SOFTWARE, forceSilentExitException.getExitCode());
       // Here we simply test that the output contains the expected messages making sure they were written to the output
       // WebApplicationExceptions are transformed a bit so UnauthorizedException  gets rendered as Forbidden: You don't have permission to access this resource
        Assertions.assertTrue(outputOptions.getMockErr().contains("Forbidden: You don't have permission to access this resource"));
        Assertions.assertTrue(outputOptions.getMockErr().contains("Unexpected Error Traversing folders"));

    }


}
