package com.dotcms.storage.binary;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Phase 2 BinaryAssetStorageAPI migration.
 * Exercises migrated code paths (Contentlet.getBinary, handleBinaries,
 * content versioning, FileAsset flow) end-to-end with real storage.
 */
@Tag("BinaryStorage")
class BinaryAssetStorageIntegrationTest {

    private static User user;
    private static ContentletAPI contentletAPI;
    private static BinaryAssetStorageAPI binaryAssetStorageAPI;

    @BeforeAll
    static void setUp() throws Exception {
        IntegrationTestInitService.getInstance().init();
        user = APILocator.systemUser();
        contentletAPI = APILocator.getContentletAPI();
        binaryAssetStorageAPI = APILocator.getBinaryAssetStorageAPI();
    }

    /**
     * AC-1: Contentlet binary checkin + retrieval works through abstraction.
     * Creates a content type with a binary field, checks in a contentlet with a test file,
     * and verifies retrieval through both Contentlet.getBinary() and BinaryAssetStorageAPI.
     */
    @Test
    void test_checkin_contentlet_with_binary_stores_and_retrieves_file() throws Exception {

        final String testContent = "integration-test-content-" + System.currentTimeMillis();
        ContentType contentType = null;

        try {
            // Create content type with title + binary field
            contentType = new ContentTypeDataGen().nextPersisted();

            final Field titleField = new FieldDataGen()
                    .velocityVarName("title")
                    .contentTypeId(contentType.id())
                    .type(TextField.class)
                    .nextPersisted();
            ContentTypeDataGen.addField(titleField);

            final Field binaryField = new FieldDataGen()
                    .velocityVarName("testBinary")
                    .contentTypeId(contentType.id())
                    .type(BinaryField.class)
                    .nextPersisted();
            ContentTypeDataGen.addField(binaryField);

            // Create temp file with known content
            final File tempFile = File.createTempFile("binary-test-", ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), testContent);

            // Create and checkin contentlet with binary
            final ContentletDataGen dataGen = new ContentletDataGen(contentType);
            dataGen.setProperty("title", "Binary Test");
            dataGen.setProperty("testBinary", tempFile);
            final Contentlet checkedIn = dataGen.nextPersisted();

            // Verify via Contentlet.getBinary (migrated in 02-01)
            final File retrievedFile = checkedIn.getBinary("testBinary");
            assertNotNull(retrievedFile, "getBinary should return a file");
            assertTrue(retrievedFile.exists(), "Retrieved file should exist on disk");
            assertEquals(testContent, Files.readString(retrievedFile.toPath()),
                    "File content should match what was uploaded");

            // Verify via BinaryAssetStorageAPI directly (2-arg, filename-less)
            final File apiFile = binaryAssetStorageAPI.getBinaryFile(
                    checkedIn.getInode(), "testBinary");
            assertNotNull(apiFile, "BinaryAssetStorageAPI should find the file");
            assertEquals(retrievedFile.getName(), apiFile.getName(),
                    "API file name should match getBinary file name");

        } finally {
            if (contentType != null) {
                ContentTypeDataGen.remove(contentType);
            }
        }
    }

    /**
     * AC-2: Content versioning preserves binaries through abstraction.
     * Creates a contentlet with binary, checks out, modifies non-binary field,
     * checks back in, and verifies binary survives the version.
     */
    @Test
    void test_content_version_preserves_binary() throws Exception {

        final String testContent = "version-test-content-" + System.currentTimeMillis();
        ContentType contentType = null;

        try {
            contentType = new ContentTypeDataGen().nextPersisted();

            final Field titleField = new FieldDataGen()
                    .velocityVarName("title")
                    .contentTypeId(contentType.id())
                    .type(TextField.class)
                    .nextPersisted();
            ContentTypeDataGen.addField(titleField);

            final Field binaryField = new FieldDataGen()
                    .velocityVarName("testBinary")
                    .contentTypeId(contentType.id())
                    .type(BinaryField.class)
                    .nextPersisted();
            ContentTypeDataGen.addField(binaryField);

            // Create temp file and checkin
            final File tempFile = File.createTempFile("version-test-", ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), testContent);

            final ContentletDataGen dataGen = new ContentletDataGen(contentType);
            dataGen.setProperty("title", "Version Test v1");
            dataGen.setProperty("testBinary", tempFile);
            final Contentlet v1 = dataGen.nextPersisted();

            // Checkout, modify title only, checkin again (new version)
            final Contentlet checkout = contentletAPI.checkout(v1.getInode(), user, false);
            checkout.setStringProperty("title", "Version Test v2");
            final Contentlet v2 = contentletAPI.checkin(checkout, user, false);

            // Verify binary survives on the new version
            assertNotEquals(v1.getInode(), v2.getInode(),
                    "New version should have a different inode");

            final File v2File = v2.getBinary("testBinary");
            assertNotNull(v2File, "Binary should exist on new version");
            assertTrue(v2File.exists(), "Binary file should exist on disk");
            assertEquals(testContent, Files.readString(v2File.toPath()),
                    "Binary content should match original after versioning");

        } finally {
            if (contentType != null) {
                ContentTypeDataGen.remove(contentType);
            }
        }
    }

    /**
     * AC-3: FileAsset flow works end-to-end.
     * Creates a FileAsset via FileAssetDataGen and verifies binary retrieval
     * through both Contentlet.getBinary and BinaryAssetStorageAPI.
     */
    @Test
    void test_fileAsset_flow_end_to_end() throws Exception {

        final String testContent = "fileasset-test-content-" + System.currentTimeMillis();
        Folder folder = null;

        try {
            folder = new FolderDataGen().nextPersisted();

            // Create temp file with known content
            final File tempFile = File.createTempFile("fileasset-test-", ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), testContent);

            // Create FileAsset via data generator
            final Contentlet fileAsset = new FileAssetDataGen(folder, tempFile).nextPersisted();

            // Verify via Contentlet.getBinary (exercises migrated read path)
            final File retrievedFile = fileAsset.getBinary(FileAssetAPI.BINARY_FIELD);
            assertNotNull(retrievedFile, "FileAsset binary should be retrievable");
            assertTrue(retrievedFile.exists(), "FileAsset binary should exist on disk");
            assertEquals(testContent, Files.readString(retrievedFile.toPath()),
                    "FileAsset content should match what was uploaded");

            // Verify via BinaryAssetStorageAPI directly (2-arg, filename-less)
            final File apiFile = binaryAssetStorageAPI.getBinaryFile(
                    fileAsset.getInode(), FileAssetAPI.BINARY_FIELD);
            assertNotNull(apiFile, "BinaryAssetStorageAPI should find the FileAsset binary");
            assertTrue(apiFile.exists(), "API-retrieved file should exist");

        } finally {
            if (folder != null) {
                FolderDataGen.remove(folder);
            }
        }
    }

    /**
     * AC-1 (backward compat): Deprecated getRealAssetPath still returns valid paths.
     * Ensures the deprecated methods continue to work for callers that haven't migrated.
     */
    @SuppressWarnings("deprecation")
    @Test
    void test_deprecated_getRealAssetPath_still_works() throws Exception {

        final String testContent = "deprecated-test-content-" + System.currentTimeMillis();
        Folder folder = null;

        try {
            folder = new FolderDataGen().nextPersisted();

            final File tempFile = File.createTempFile("deprecated-test-", ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), testContent);

            final Contentlet fileAsset = new FileAssetDataGen(folder, tempFile).nextPersisted();

            // Call deprecated method — should still return a valid path
            final String realPath = APILocator.getFileAssetAPI()
                    .getRealAssetPath(fileAsset.getInode(),
                            fileAsset.getStringProperty(FileAssetAPI.UNDERLYING_FILENAME));

            assertNotNull(realPath, "Deprecated getRealAssetPath should return a path");

            final File fileFromPath = new File(realPath);
            assertTrue(fileFromPath.exists(),
                    "Path from deprecated method should point to an existing file");
            assertEquals(testContent, Files.readString(fileFromPath.toPath()),
                    "Content from deprecated path should match uploaded content");

        } finally {
            if (folder != null) {
                FolderDataGen.remove(folder);
            }
        }
    }

}
