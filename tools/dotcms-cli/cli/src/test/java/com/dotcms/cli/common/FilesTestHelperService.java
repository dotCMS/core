package com.dotcms.cli.common;

import static com.dotcms.common.AssetsUtils.buildRemoteAssetURL;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.FolderAPI;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.ByPathRequest;
import com.dotcms.model.asset.FileUploadData;
import com.dotcms.model.asset.FileUploadDetail;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

@ApplicationScoped
public class FilesTestHelperService {

    @Inject
    RestClientFactory clientFactory;

    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    /**
     * Prepares data by creating test folders, adding test files, and creating a new test site.
     *
     * @return The name of the newly created test site.
     * @throws IOException If an I/O error occurs.
     */
    public String prepareData() throws IOException {
        return prepareData(true);
    }

    /**
     * Prepares data by creating test folders, adding test files, and creating a new test site.
     *
     * @param includeAssets A boolean value indicating whether to include test assets or not.
     * @return The name of the newly created test site.
     * @throws IOException If an I/O error occurs.
     */
    public String prepareData(final boolean includeAssets) throws IOException {

        final FolderAPI folderAPI = clientFactory.getClient(FolderAPI.class);

        // root folders
        final String folder1 = "folder1";
        final String folder2 = "folder2";
        final String folder3 = "folder3";
        final String folder4 = "folder4 withSpace";

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
        final String subfolder2_1_1 = "subFolder2-1-1-子資料夾";
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
                String.format("/%s", folder3),
                String.format("/%s", folder4)
        );

        // Creating a new test site
        final String newSiteName = createSite();

        // Creating test folders
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse =
                folderAPI.makeFolders(paths, newSiteName);
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
                    String.format("/%s/%s/%s", folder2, subfolder2_1, subfolder2_1_1),
                    "這就是我的想像6.png");
            pushFile(true, "en-us", newSiteName,
                    String.format("/%s/%s/%s", folder2, subfolder2_1, subfolder2_1_1),
                    "image (7)+.png");
            pushFile(true, "en-us", newSiteName,
                    String.format("/%s", folder3),
                    "image 3.png");
            pushFile(true, "en-us", newSiteName,
                    String.format("/%s", folder4),
                    "image5.jpg");
        }

        return newSiteName;
    }

    /**
     * Creates a new site.
     *
     * @return The name of the newly created test site.
     */
    public String createSite() {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // Creating a new test site
        final String newSiteName = String.format("site-%s", UUID.randomUUID());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder()
                .siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = siteAPI.create(newSiteRequest);
        Assertions.assertNotNull(createSiteResponse);
        // Publish the new site
        siteAPI.publish(createSiteResponse.entity().identifier());
        Assertions.assertTrue(siteExist(newSiteName),
                String.format("Site %s was not created", newSiteName));

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
     * @throws IOException If there is an error reading the file or pushing it to the server
     */
    public void pushFile(final boolean live, final String language,
            final String siteName, String folderPath, final String assetName) throws IOException {

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        // Building the remote asset path
        final var remoteAssetPath = buildRemoteAssetURL(siteName, folderPath, assetName);

        // Reading the file and preparing the data to be pushed
        try (InputStream inputStream = getClass().getResourceAsStream(
                String.format("/%s", assetName))) {

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

            // Validate it is there giving some time to the system to index it
            Assertions.assertTrue(assetExist(remoteAssetPath),
                    String.format("Asset %s not created", remoteAssetPath));
        }
    }

    /**
     * Checks if a site with the given name exists.
     *
     * @param siteName the name of the site to check
     * @return true if the site exists, false otherwise
     */
    public Boolean siteExist(final String siteName) {

        try {

            await()
                    .atMost(MAX_WAIT_TIME)
                    .pollInterval(POLL_INTERVAL)
                    .until(() -> {
                        try {
                            var response = findSiteByName(siteName);
                            return (response != null && response.entity() != null) &&
                                    ((response.entity().isLive() != null &&
                                            response.entity().isLive()) &&
                                            (response.entity().isWorking() != null &&
                                                    response.entity().isWorking()));
                        } catch (NotFoundException e) {
                            return false;
                        }
                    });

            return true;
        } catch (ConditionTimeoutException ex) {
            return false;
        }
    }

    /**
     * Checks whether an asset exists at the given remote asset path.
     *
     * @param remoteAssetPath The path to the remote asset
     * @return {@code true} if the asset exists, {@code false} otherwise
     */
    public Boolean assetExist(final String remoteAssetPath) {

        try {

            await()
                    .atMost(MAX_WAIT_TIME)
                    .pollInterval(POLL_INTERVAL)
                    .until(() -> {
                        try {
                            var response = findAssetPath(remoteAssetPath);
                            Assertions.assertEquals(1, response.entity().versions().size());
                            return (response.entity().versions().get(0).live() &&
                                    response.entity().versions().get(0).working());
                        } catch (NotFoundException e) {
                            return false;
                        }
                    });

            return true;
        } catch (ConditionTimeoutException ex) {
            return false;
        }
    }

    /**
     * Retrieves a site by its name.
     *
     * @param siteName The name of the site.
     * @return The ResponseEntityView containing the SiteView object representing the site.
     */
    @ActivateRequestContext
    public ResponseEntityView<SiteView> findSiteByName(final String siteName) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // Execute the REST call to retrieve folder contents
        return siteAPI.findByName(
                GetSiteByNameRequest.builder().siteName(siteName).build()
        );
    }

    /**
     * Retrieves the asset at the given remote path.
     *
     * @param remoteAssetPath The path to the remote asset
     * @return The ResponseEntityView containing the AssetVersionsView object representing the
     * asset.
     */
    @ActivateRequestContext
    public ResponseEntityView<AssetVersionsView> findAssetPath(final String remoteAssetPath) {

        final AssetAPI assetAPI = clientFactory.getClient(AssetAPI.class);

        return assetAPI.assetByPath(
                ByPathRequest.builder().assetPath(remoteAssetPath).build()
        );
    }

    /**
     * Creates a temporary folder with a random name.
     *
     * @return The path to the newly created temporary folder
     * @throws IOException If an I/O error occurs while creating the temporary folder
     */
    public Path createTempFolder() throws IOException {

        String randomFolderName = "folder-" + UUID.randomUUID();
        return Files.createTempDirectory(randomFolderName);
    }

    /**
     * Deletes a temporary directory and all its contents recursively.
     *
     * @param folderPath The path to the temporary directory to delete
     * @throws IOException If an error occurs while deleting the directory or its contents
     */
    public void deleteTempDirectory(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file); // Deletes the file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir); // Deletes the directory after its content has been deleted
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
