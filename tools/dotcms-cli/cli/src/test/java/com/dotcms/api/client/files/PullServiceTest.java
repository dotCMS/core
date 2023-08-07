package com.dotcms.api.client.files;

import static com.dotcms.common.AssetsUtils.BuildRemoteAssetURL;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.FolderAPI;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.asset.FileUploadData;
import com.dotcms.model.asset.FileUploadDetail;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PullServiceTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @Inject
    RemoteTraversalService remoteTraversalService;

    @Inject
    PullService pullService;

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

    @Test
    void Test_Folders_Check() throws IOException {

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
                    parsePatternOption(null),
                    parsePatternOption(null),
                    parsePatternOption(null),
                    parsePatternOption(null)
            );

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();
            pullService.pullTree(outputOptions, result.getRight(), workspace.files().toAbsolutePath().toFile(),
                    true, true, true, 0);

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
                        .forEach(path -> collectedFiles.add(tempFolder.relativize(path).toString()));
            }
            //Let's remove the workspace folder from the list
            List<String> existingFolders = collectedFolders.stream().map(folder -> folder.replaceAll(
                    "^dot-workspace-\\d+/", "")).collect(Collectors.toList());
            List<String> existingFiles = collectedFiles.stream().map(folder -> folder.replaceAll(
                    "^dot-workspace-\\d+/", "")).collect(Collectors.toList());

            var basePath = "files/live/en-us/" + testSiteName;
            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFolders = Map.of(
                    basePath,
                    Arrays.asList("folder1", "folder2", "folder3"),
                    basePath + "/folder1",
                    Arrays.asList("subFolder1-1", "subFolder1-2", "subFolder1-3"),
                    basePath + "/folder1/subFolder1-1",
                    Arrays.asList("subFolder1-1-1", "subFolder1-1-2", "subFolder1-1-3"),
                    basePath + "/folder1/subFolder1-2",
                    Arrays.asList("subFolder1-2-1", "subFolder1-2-2", "subFolder1-2-3"),
                    basePath + "/folder1/subFolder1-3",
                    Collections.emptyList(),
                    basePath + "/folder2",
                    Arrays.asList("subFolder2-1", "subFolder2-2", "subFolder2-3"),
                    basePath + "/folder2/subFolder2-1",
                    Arrays.asList("subFolder2-1-1", "subFolder2-1-2", "subFolder2-1-3"),
                    basePath + "/folder2/subFolder2-2",
                    Collections.emptyList(),
                    basePath + "/folder2/subFolder2-3",
                    Collections.emptyList(),
                    basePath + "/folder3",
                    Collections.emptyList()
            );

            // Expected folder structure based on the treeNode object
            Map<String, List<String>> expectedFiles = Map.of(
                    basePath, Collections.emptyList(),
                    basePath + "/folder1/subFolder1-1/subFolder1-1-1", Arrays.asList("image1.png", "image4.jpg"),
                    basePath + "/folder2/subFolder2-1/subFolder2-1-1", Arrays.asList("image2.png"),
                    basePath + "/folder3", Arrays.asList("image3.png")
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
     * Prepares data by creating test folders, adding test files, and creating a new test site.
     *
     * @return The name of the newly created test site.
     * @throws IOException If an I/O error occurs.
     */
    private String prepareData() throws IOException {

        final FolderAPI folderAPI = clientFactory.getClient(FolderAPI.class);
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // root folders
        final String folder1 = "folder1";
        final String folder2 = "folder2";
        final String folder3 = "folder3";

        // folder1 children
        final String subfolder1_1 = "subFolder1-1";
        final String subfolder1_2 = "subFolder1-2";
        final String subfolder1_3 = "subFolder1-3";

        // folder2 children
        final String subfolder2_1 = "subFolder2-1";
        final String subfolder2_2 = "subFolder2-2";
        final String subfolder2_3 = "subFolder2-3";

        // subfolder1_1 children
        final String subfolder1_1_1 = "subFolder1-1-1";
        final String subfolder1_1_2 = "subFolder1-1-2";
        final String subfolder1_1_3 = "subFolder1-1-3";

        // subfolder1_2 children
        final String subfolder1_2_1 = "subFolder1-2-1";
        final String subfolder1_2_2 = "subFolder1-2-2";
        final String subfolder1_2_3 = "subFolder1-2-3";

        // subfolder2_1 children
        final String subfolder2_1_1 = "subFolder2-1-1";
        final String subfolder2_1_2 = "subFolder2-1-2";
        final String subfolder2_1_3 = "subFolder2-1-3";

        var paths = ImmutableList.of(
                String.format("/%s/%s/%s", folder1, subfolder1_1, subfolder1_1_1),
                String.format("/%s/%s/%s", folder1, subfolder1_1, subfolder1_1_2),
                String.format("/%s/%s/%s", folder1, subfolder1_1, subfolder1_1_3),
                String.format("/%s/%s/%s", folder1, subfolder1_2, subfolder1_2_1),
                String.format("/%s/%s/%s", folder1, subfolder1_2, subfolder1_2_2),
                String.format("/%s/%s/%s", folder1, subfolder1_2, subfolder1_2_3),
                String.format("/%s/%s", folder1, subfolder1_3),
                String.format("/%s/%s/%s", folder2, subfolder2_1, subfolder2_1_1),
                String.format("/%s/%s/%s", folder2, subfolder2_1, subfolder2_1_2),
                String.format("/%s/%s/%s", folder2, subfolder2_1, subfolder2_1_3),
                String.format("/%s/%s", folder2, subfolder2_2),
                String.format("/%s/%s", folder2, subfolder2_3),
                String.format("/%s", folder3)
        );

        // Creating a new test site
        final String newSiteName = String.format("site-%d", System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder()
                .siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = siteAPI.create(newSiteRequest);
        Assertions.assertNotNull(createSiteResponse);
        // Publish the new site
        siteAPI.publish(createSiteResponse.entity().identifier());

        // Creating test folders
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(
                paths, newSiteName);
        Assertions.assertNotNull(makeFoldersResponse.entity());

        // Adding some test files
        pushFile(true, "en-us", newSiteName,
                String.format("/%s/%s/%s", folder1, subfolder1_1, subfolder1_1_1), "image1.png");
        pushFile(true, "en-us", newSiteName,
                String.format("/%s/%s/%s", folder1, subfolder1_1, subfolder1_1_1), "image4.jpg");
        pushFile(true, "en-us", newSiteName,
                String.format("/%s/%s/%s", folder2, subfolder2_1, subfolder2_1_1), "image2.png");
        pushFile(true, "en-us", newSiteName,
                String.format("/%s", folder3), "image3.png");

        return newSiteName;
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

    /**
     * Pushes a file asset to the given site and folder path.
     *
     * @param live       Whether the asset should be published live
     * @param language   The language of the asset
     * @param siteName   The name of the site to push the asset to
     * @param folderPath The folder path where the asset will be pushed
     * @param assetName  The name of the asset file
     * @throws IOException If there is an error reading the file or pushing
     *                     it to the server
     */
    private void pushFile(final boolean live, final String language,
                         final String siteName, String folderPath, final String assetName) throws IOException {

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        // Building the remote asset path
        final var remoteAssetPath = BuildRemoteAssetURL(siteName, folderPath, assetName);

        // Reading the file and preparing the data to be pushed
        try (InputStream inputStream = getClass().getResourceAsStream(String.format("/%s", assetName))) {

            var uploadForm = new FileUploadData();
            uploadForm.setAssetPath(remoteAssetPath);
            uploadForm.setDetail(new FileUploadDetail(
                    remoteAssetPath,
                    language,
                    live
            ));
            uploadForm.setFile(inputStream);

            // Pushing the file
            assetAPI.push(uploadForm);
        }
    }

    /**
     * Creates a temporary folder with a random name.
     *
     * @return The path to the newly created temporary folder
     * @throws IOException If an I/O error occurs while creating the temporary folder
     */
    private Path createTempFolder() throws IOException {

        String randomFolderName = "folder-" + UUID.randomUUID();
        return Files.createTempDirectory(randomFolderName);
    }

    /**
     * Deletes a temporary directory and all its contents recursively.
     *
     * @param folderPath The path to the temporary directory to delete
     * @throws IOException If an error occurs while deleting the directory or its contents
     */
    private void deleteTempDirectory(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); // Deletes the file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir); // Deletes the directory after its content has been deleted
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
