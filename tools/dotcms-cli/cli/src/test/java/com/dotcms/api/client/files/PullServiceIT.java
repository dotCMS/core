package com.dotcms.api.client.files;

import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_EMPTY_FOLDERS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.NON_RECURSIVE;
import static com.dotcms.api.client.pull.file.OptionConstants.PRESERVE;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.api.client.pull.PullService;
import com.dotcms.api.client.pull.file.FileFetcher;
import com.dotcms.api.client.pull.file.FilePullHandler;
import com.dotcms.cli.common.FilesTestHelper;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.pull.PullOptions;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class PullServiceIT extends FilesTestHelper {

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
        var tempFolder = createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName1 = prepareData();
            final var testSiteName2 = prepareData();
            final var testSiteName3 = prepareData();

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
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler,
                    customOptions
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
                                .forEach(path -> {
                                    String decodedPath = URLDecoder.decode(
                                            tempFolder.relativize(path).toString(),
                                            StandardCharsets.UTF_8);
                                    collectedFiles.add(decodedPath);
                                });
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
                                    Arrays.asList("subFolder2-1-1", "subFolder2-1-2",
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
                            basePath + "/folder2/subFolder2-1/subFolder2-1-1",
                            Arrays.asList("image2.png"),
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
            deleteTempDirectory(tempFolder);
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
        var tempFolder = createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = prepareData();

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
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler,
                    customOptions
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
                        .forEach(path -> {
                            String decodedPath = URLDecoder.decode(
                                    tempFolder.relativize(path).toString(), StandardCharsets.UTF_8);
                            collectedFiles.add(decodedPath);
                        });
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
                            Arrays.asList("subFolder2-1-1", "subFolder2-1-2", "subFolder2-1-3")),
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
                    basePath + "/folder2/subFolder2-1/subFolder2-1-1", Arrays.asList("image2.png"),
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
            deleteTempDirectory(tempFolder);
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
        var tempFolder = createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = prepareData();

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
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler,
                    customOptions
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
                        .forEach(path -> {
                            String decodedPath = URLDecoder.decode(
                                    tempFolder.relativize(path).toString(), StandardCharsets.UTF_8);
                            collectedFiles.add(decodedPath);
                        });
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
            deleteTempDirectory(tempFolder);
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
        var tempFolder = createTempFolder();
        var workspace = workspaceManager.getOrCreate(tempFolder);

        try {

            // Preparing the data for the test
            final var testSiteName = prepareData();

            final var folderPath = String.format(
                    "//%s/folder2/subFolder2-1/subFolder2-1-1/image2.png", testSiteName);

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
                            build(),
                    outputOptions,
                    fileProvider,
                    filePullHandler,
                    customOptions
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
                        .forEach(path -> {
                            String decodedPath = URLDecoder.decode(
                                    tempFolder.relativize(path).toString(), StandardCharsets.UTF_8);
                            collectedFiles.add(decodedPath);
                        });
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
                    basePath + "/folder2/subFolder2-1", Arrays.asList("subFolder2-1-1")
            );

            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFiles = Map.of(
                    basePath, Collections.emptyList(),
                    basePath + "/folder2/subFolder2-1/subFolder2-1-1", Arrays.asList("image2.png")
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
            deleteTempDirectory(tempFolder);
        }
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
