package com.dotcms.cli.common;

import static com.dotcms.common.AssetsUtils.buildRemoteAssetURL;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.FolderAPI;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.model.ResponseEntityView;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.Assertions;

public class FilesTestHelper {

    @Inject
    RestClientFactory clientFactory;

    /**
     * Prepares data by creating test folders, adding test files, and creating a new test site.
     *
     * @return The name of the newly created test site.
     * @throws IOException If an I/O error occurs.
     */
    protected String prepareData() throws IOException {
        return prepareData(true);
    }

    /**
     * Prepares data by creating test folders, adding test files, and creating a new test site.
     *
     * @param includeAssets A boolean value indicating whether to include test assets or not.
     * @return The name of the newly created test site.
     * @throws IOException If an I/O error occurs.
     */
    protected String prepareData(final boolean includeAssets) throws IOException {

        final FolderAPI folderAPI = clientFactory.getClient(FolderAPI.class);
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

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
                String.format("/%s", folder3),
                String.format("/%s", folder4)
        );

        // Creating a new test site
        final String newSiteName = String.format("site-%d", System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder()
                .siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = siteAPI.create(newSiteRequest);
        Assertions.assertNotNull(createSiteResponse);
        // Publish the new site
        siteAPI.publish(createSiteResponse.entity().identifier());
        Assertions.assertTrue(siteExist(newSiteName),
                String.format("Site %s was not created", newSiteName));

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
                    "image 3.png");
            pushFile(true, "en-us", newSiteName,
                    String.format("/%s", folder4),
                    "image5.jpg");
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
     * @throws IOException If there is an error reading the file or pushing it to the server
     */
    protected void pushFile(final boolean live, final String language,
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
     * Checks whether an asset exists at the given remote asset path.
     *
     * @param remoteAssetPath The path to the remote asset
     * @return {@code true} if the asset exists, {@code false} otherwise
     */
    protected Boolean assetExist(final String remoteAssetPath) {

        long start = System.currentTimeMillis();
        long end = start + 15 * 1000; // 15 seconds * 1000 ms/sec
        while (System.currentTimeMillis() < end) {
            try {
                var response = clientFactory.getClient(AssetAPI.class).
                        assetByPath(
                                ByPathRequest.builder().assetPath(remoteAssetPath).build());
                Assertions.assertEquals(1, response.entity().versions().size());
                if (response.entity().versions().get(0).live() &&
                        response.entity().versions().get(0).working()) {
                    return true;
                }
            } catch (NotFoundException e) {
                // Do nothing
            }

            try {
                Thread.sleep(2000); // Sleep for 2 second
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        return false;
    }

    /**
     * Checks if a site with the given name exists.
     *
     * @param siteName the name of the site to check
     * @return true if the site exists, false otherwise
     */
    protected Boolean siteExist(final String siteName) {

        long start = System.currentTimeMillis();
        long end = start + 15 * 1000; // 15 seconds * 1000 ms/sec
        while (System.currentTimeMillis() < end) {
            try {
                var response = clientFactory.getClient(SiteAPI.class)
                        .findByName(GetSiteByNameRequest.builder().siteName(siteName).build());
                if ((response != null && response.entity() != null) &&
                        (response.entity().isLive() && response.entity().isWorking())) {
                    return true;
                }
            } catch (NotFoundException e) {
                // Do nothing
            }

            try {
                Thread.sleep(2000); // Sleep for 2 second
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        return false;
    }

    /**
     * Creates a temporary folder with a random name.
     *
     * @return The path to the newly created temporary folder
     * @throws IOException If an I/O error occurs while creating the temporary folder
     */
    protected synchronized Path createTempFolder() throws IOException {
        return Files.createTempDirectory(String.format("folder-%s", UUID.randomUUID()));
    }

    /**
     * Deletes a temporary directory and all its contents recursively.
     *
     * @param folderPath The path to the temporary directory to delete
     * @throws IOException If an error occurs while deleting the directory or its contents
     */
    protected void deleteTempDirectory(Path folderPath) throws IOException {
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
