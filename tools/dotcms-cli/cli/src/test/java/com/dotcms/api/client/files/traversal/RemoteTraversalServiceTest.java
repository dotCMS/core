package com.dotcms.api.client.files.traversal;

import static com.dotcms.common.AssetsUtils.buildRemoteAssetURL;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.FolderAPI;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class RemoteTraversalServiceTest {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @Inject
    RemoteTraversalService remoteTraversalService;

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
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Folders_Depth_Zero() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData();

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
        var treeNode = result.getRight();

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
    void Test_Include() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData();

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
        var treeNode = result.getRight();

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
    void Test_Include2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData();

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
        var treeNode = result.getRight();

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
    void Test_Include3() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData();

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
        var treeNode = result.getRight();

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
    void Test_Include_Assets() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Include_Assets2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Include_Assets3() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Include_Assets4() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Include_Assets5() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Exclude() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData();

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
        var treeNode = result.getRight();

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
    void Test_Exclude2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData();

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
        var treeNode = result.getRight();

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
    void Test_Exclude3() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData();

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
        var treeNode = result.getRight();

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

    @Test
    void Test_Exclude_Assets() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has no asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Exclude_Assets2() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Exclude_Assets3() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 0 asset)
        Assertions.assertEquals(0, treeNode.children().get(2).assets().size());
    }

    @Test
    void Test_Exclude_Assets4() throws IOException {

        // Preparing the data for the test
        final var testSiteName = prepareData(true);

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
        var treeNode = result.getRight();

        // ============================
        //Validating the tree
        // ============================
        // Root
        Assertions.assertEquals(3, treeNode.children().size());

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
        // subFolder2-1-1 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(1).children().get(0).children().get(0).assets().size());
        // SubFolder2-2
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(1).children().size());
        // SubFolder2-3
        Assertions.assertEquals(0, treeNode.children().get(1).children().get(2).children().size());

        // Folder 3
        Assertions.assertEquals(0, treeNode.children().get(2).children().size());
        // Folder 3 (has 1 asset)
        Assertions.assertEquals(1, treeNode.children().get(2).assets().size());
    }

    private String prepareData() throws IOException {
        return prepareData(false);
    }

    private String prepareData(final boolean includeAssets) throws IOException {

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

        if (includeAssets) {
            // Adding some test files
            pushFile(true, "en-us", newSiteName,
                    String.format("/%s/%s/%s", folder1, subfolder1_1, subfolder1_1_1),
                    "image1.png");
            pushFile(true, "en-us", newSiteName,
                    String.format("/%s/%s/%s", folder1, subfolder1_1, subfolder1_1_1),
                    "image4.jpg");
            pushFile(true, "en-us", newSiteName,
                    String.format("/%s/%s/%s", folder2, subfolder2_1, subfolder2_1_1),
                    "image2.png");
            pushFile(true, "en-us", newSiteName,
                    String.format("/%s", folder3),
                    "image3.png");
        }

        return newSiteName;
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
        final var remoteAssetPath = buildRemoteAssetURL(siteName, folderPath, assetName);

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
