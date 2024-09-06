package com.dotcms.api;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.asset.AssetRequest;
import com.dotcms.model.asset.ByPathRequest;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.config.ServiceBean;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.net.URL;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class AssetAPIT {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

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
     * Check for the proper response when a folder is not found
     */
    @Test
    void Test_Asset_By_Path_Not_Found() {

        final AssetAPI assetAPI = clientFactory.getClient(AssetAPI.class);

        var folderByPath = ByPathRequest.builder().
                assetPath(String.format("//%s/%s", siteName, "folderDoesNotExist")).build();

        try {
            assetAPI.folderByPath(folderByPath);
            Assertions.fail(" 404 Exception should have been thrown here.");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    /**
     * Check for the proper response when a file is not found
     */
    @Test
    void Test_Download_Not_Found() {

        final AssetAPI assetAPI = clientFactory.getClient(AssetAPI.class);

        var folderByPath = AssetRequest.builder().
                assetPath(String.format("//%s/%s/%s", siteName, "folderDoesNotExist", "image.png")).
                language("es-CR").
                live(true).
                build();

        try {
            assetAPI.download(folderByPath);
            Assertions.fail(" 404 Exception should have been thrown here.");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    /**
     * Request and check for a folder three levels deep with no folders on it
     */
    @Test
    void Test_Asset_By_Path_Inner_Folders_Three_Levels() {

        final FolderAPI folderAPI = clientFactory.getClient(FolderAPI.class);

        final String randomFolderName1 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        final String randomFolderName2 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        final String randomFolderName3 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        final String path1 = String.format("/%s/%s/%s", randomFolderName1, randomFolderName2,
                randomFolderName3);

        // First we need to create a test folder
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(
                ImmutableList.of(path1),
                siteName);
        Assertions.assertNotNull(makeFoldersResponse.entity());

        // Request the folder and check the data is correct
        var folderByPath = ByPathRequest.builder().
                assetPath(String.format("//%s/%s", siteName, path1)).build();
        executeAndTest(folderByPath, true);
    }

    /**
     * Request and check for a folder with two children on it
     */
    @Test
    void Test_Asset_By_Path_Inner_Folders() {

        final FolderAPI folderAPI = clientFactory.getClient(FolderAPI.class);

        final String randomFolderName1 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        final String randomFolderName2 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        final String randomFolderName3 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        final String path1 = String.format("/%s/%s", randomFolderName1, randomFolderName2);
        final String path2 = String.format("/%s/%s", randomFolderName1, randomFolderName3);

        // First we need to create a test folder
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(
                ImmutableList.of(path1, path2),
                siteName);
        Assertions.assertNotNull(makeFoldersResponse.entity());

        // Request the folder and check the data is correct
        var folderByPath = ByPathRequest.builder().
                assetPath(String.format("//%s/%s", siteName, randomFolderName1)).build();
        executeAndTest(folderByPath, true, randomFolderName2, randomFolderName3);
    }

    /**
     * Request and check for the root of the site
     */
    @Test
    void Test_Asset_By_Path_Root() {

        final FolderAPI folderAPI = clientFactory.getClient(FolderAPI.class);

        final String randomFolderName1 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        final String randomFolderName2 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        final String randomFolderName3 = String.format("folder-%s",
                RandomStringUtils.randomAlphabetic(10));

        // First we need to create a test folder
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(
                ImmutableList.of(randomFolderName1, randomFolderName2, randomFolderName3),
                siteName);
        Assertions.assertNotNull(makeFoldersResponse.entity());

        // Request the folder and check the data is correct
        var folderByPath = ByPathRequest.builder().
                assetPath(String.format("//%s/", siteName)).build();
        executeAndTest(folderByPath, false, randomFolderName1, randomFolderName2, randomFolderName3);
    }

    void executeAndTest(ByPathRequest request, boolean exactMatch, String... folderNames) {

        final AssetAPI assetAPI = clientFactory.getClient(AssetAPI.class);

        // Now, lets try to request the requested folders using the asset API
        final ResponseEntityView<FolderView> byPathResponse = assetAPI.folderByPath(request);
        Assertions.assertNotNull(byPathResponse.entity());

        // Make sure the roots folder has the basic info
        Assertions.assertNotNull(byPathResponse.entity().identifier());
        Assertions.assertNotNull(byPathResponse.entity().inode());
        Assertions.assertNotNull(byPathResponse.entity().path());
        Assertions.assertNotNull(byPathResponse.entity().name());

        // Make sure we have all the folders we created
        if (folderNames.length > 0) {
            Assertions.assertNotNull(byPathResponse.entity().subFolders());
        }

        var subFoldersSize = byPathResponse.entity().subFolders() == null ? 0
                : byPathResponse.entity().subFolders().size();

        if (exactMatch) {
            Assertions.assertEquals(subFoldersSize, folderNames.length);
        } else {
            Assertions.assertTrue(subFoldersSize >= folderNames.length);
        }

        for (String folderName : folderNames) {
            Assertions.assertTrue(exists(byPathResponse.entity().subFolders(), folderName));
        }
    }

    boolean exists(final List<FolderView> folders, final String name) {
        return folders.stream().anyMatch(f -> f.name().equals(name));
    }
}
