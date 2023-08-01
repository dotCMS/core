package com.dotcms.api.traversal;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.FolderAPI;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
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
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@QuarkusTest
public class FolderTraversalServiceIT {

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
    void Test_Not_Found() {

        var folderPath = String.format("//%s/%s", siteName, "folderDoesNotExist");

        try {

            folderTraversalService.traverse(
                    folderPath,
                    0,
                    parsePatternOption(null),
                    parsePatternOption(null),
                    parsePatternOption(null),
                    parsePatternOption(null)
            );

            Assertions.fail(" 404 Exception should have been thrown here.");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    void Test_Folders_Check() {

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

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

        // Folder1
        Assertions.assertEquals(3, treeNode.children().get(0).children().size());
        // SubFolder1-1
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(0).children().size());
        // SubFolder1-2
        Assertions.assertEquals(3, treeNode.children().get(0).children().get(1).children().size());
        // SubFolder1-3
        Assertions.assertEquals(0, treeNode.children().get(0).children().get(2).children().size());

        // Folder2
        Assertions.assertEquals(3, treeNode.children().get(1).children().size());
        // SubFolder2-1
        Assertions.assertEquals(3, treeNode.children().get(1).children().get(0).children().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
    }

    @Test
    void Test_Folders_Depth_Zero() {

        // Preparing the data for the test
        final var testSiteName = prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var treeNode = folderTraversalService.traverse(
                folderPath,
                0,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());
        // Folder1
        Assertions.assertEquals(0, treeNode.children().get(0).children().size());
        // Folder2
        Assertions.assertEquals(0, treeNode.children().get(1).children().size());
        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
    }

    @Test
    void Test_Include() {

        // Preparing the data for the test
        final var testSiteName = prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var treeNode = folderTraversalService.traverse(
                folderPath,
                null,
                parsePatternOption("**/subFolder1-1/**"),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());
        for (var child : treeNode.children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }

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
    }

    @Test
    void Test_Include2() {

        // Preparing the data for the test
        final var testSiteName = prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var treeNode = folderTraversalService.traverse(
                folderPath,
                null,
                parsePatternOption("**/subFolder1-1"),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());
        for (var child : treeNode.children()) {
            Assertions.assertFalse(child.folder().implicitGlobInclude());
        }

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
    }

    @Test
    void Test_Include3() {

        // Preparing the data for the test
        final var testSiteName = prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var treeNode = folderTraversalService.traverse(
                folderPath,
                null,
                parsePatternOption("**/folder1,**/folder3"),
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption(null)
        );

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
    void Test_Exclude() {

        // Preparing the data for the test
        final var testSiteName = prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var treeNode = folderTraversalService.traverse(
                folderPath,
                null,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("**/subFolder1-1/**"),
                parsePatternOption(null)
        );

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());
        for (var child : treeNode.children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }

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
    }

    @Test
    void Test_Exclude2() {

        // Preparing the data for the test
        final var testSiteName = prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var treeNode = folderTraversalService.traverse(
                folderPath,
                null,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("**/subFolder1-1"),
                parsePatternOption(null)
        );

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());
        for (var child : treeNode.children()) {
            Assertions.assertTrue(child.folder().implicitGlobInclude());
        }

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
    }

    @Test
    void Test_Exclude3() {

        // Preparing the data for the test
        final var testSiteName = prepareData();

        final var folderPath = String.format("//%s", testSiteName);

        var treeNode = folderTraversalService.traverse(
                folderPath,
                null,
                parsePatternOption(null),
                parsePatternOption(null),
                parsePatternOption("**/folder1,**/folder3"),
                parsePatternOption(null)
        );

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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

}
