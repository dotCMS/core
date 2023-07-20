package com.dotcms.api.client.files;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.FolderAPI;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.api.traversal.RemoteFolderTraversalService;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@QuarkusTest
public class PullFilesServiceTest {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @Inject
    RemoteFolderTraversalService folderTraversalService;

    @Inject
    PullService pullAssetsService;

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

        try {

            // Preparing the data for the test
            final var testSiteName = prepareData();

            final var folderPath = String.format("//%s", testSiteName);

            var treeNode = folderTraversalService.traverse(
                    folderPath,
                    null,
                    parsePatternOption(null),
                    parsePatternOption(null),
                    parsePatternOption(null),
                    parsePatternOption(null)
            );

            // Pulling the content
            OutputOptionMixin outputOptions = new MockOutputOptionMixin();
            pullAssetsService.pullTree(outputOptions, treeNode, tempFolder.toAbsolutePath().toString(), true, true);

            // ============================
            //Validating the file system
            // ============================
            // Get the list of folders created inside the temporal folder
            List<String> existingFolders = new ArrayList<>();
            Files.walk(tempFolder)
                    .filter(Files::isDirectory)
                    .forEach(path -> existingFolders.add(tempFolder.relativize(path).toString()));

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

        } finally {
            // Clean up the temporal folder
            deleteTempDirectory(tempFolder);
        }
    }

    private String prepareData() {

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

        // First we need to create a test folder
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(
                paths, newSiteName);
        Assertions.assertNotNull(makeFoldersResponse.entity());

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

    private Path createTempFolder() throws IOException {

        String randomFolderName = "folder-" + UUID.randomUUID().toString();
        return Files.createTempDirectory(randomFolderName);
    }

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
