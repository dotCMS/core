package com.dotcms.content.elasticsearch.business;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for #37782: a content destroy inside a transaction removes the content's
 * binaries, resized-image cache and stored metadata only once the outermost transaction commits.
 * <p>
 * Every test checks the files on disk and the metadata in the configured storage directly, on the
 * calling thread, with no waiting: a removal that ran late or in the background fails the test
 * instead of being hidden by a poll.
 */
public class DeferredBinaryRemovalIT {

    private static User admin;
    private static Host site;
    private static ContentletAPI contentletAPI;
    private static FileMetadataAPI fileMetadataAPI;

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        contentletAPI = APILocator.getContentletAPI();
        fileMetadataAPI = APILocator.getFileMetadataAPI();
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#destroy(List, User, boolean)}
     * Given Scenario: A file asset destroyed inside a transaction that then rolls back
     * ExpectedResult: Every version keeps its binaries, resized-image cache and metadata
     */
    @Test
    void test_destroy_rolledBack_keepsFilesAndMetadata() throws Exception {
        final Versions versions = fileAssetWithTwoVersions();

        rollBackAfter(() -> contentletAPI.destroy(List.of(versions.working()), admin, false));

        assertKept(versions.older());
        assertKept(versions.working());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#delete(List, User, boolean, boolean)}
     * Given Scenario: A file asset's working version deleted inside a transaction that then rolls
     * back
     * ExpectedResult: The version keeps its binaries, resized-image cache and metadata
     */
    @Test
    void test_delete_rolledBack_keepsFilesAndMetadata() throws Exception {
        final Versions versions = fileAssetWithTwoVersions();

        rollBackAfter(() -> contentletAPI.delete(List.of(versions.working()), admin, false, true));

        assertKept(versions.working());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#deleteAllVersionsandBackup(List, User, boolean)}
     * Given Scenario: A file asset's versions deleted with a backup inside a transaction that then
     * rolls back. Called on the implementation directly: the public API's interceptor does not
     * forward this method ({@code ContentletAPIInterceptor} leaves it empty), so through
     * {@code APILocator} the call would do nothing and the test would pass vacuously.
     * ExpectedResult: Every version keeps its binaries, resized-image cache and metadata
     */
    @Test
    void test_deleteAllVersionsandBackup_rolledBack_keepsFilesAndMetadata() throws Exception {
        final Versions versions = fileAssetWithTwoVersions();

        rollBackAfter(() -> new ESContentletAPIImpl().deleteAllVersionsandBackup(
                List.of(versions.working()), admin, false));

        assertKept(versions.older());
        assertKept(versions.working());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#deleteVersion(Contentlet, User, boolean)}
     * Given Scenario: A file asset's older version deleted inside a transaction that then rolls
     * back. This path also removes the version's own metadata outside the shared removal.
     * ExpectedResult: The version keeps its binaries, resized-image cache and metadata
     */
    @Test
    void test_deleteVersion_rolledBack_keepsFilesAndMetadata() throws Exception {
        final Versions versions = fileAssetWithTwoVersions();

        rollBackAfter(() -> contentletAPI.deleteVersion(versions.older(), admin, false));

        assertKept(versions.older());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#destroy(List, User, boolean)}
     * Given Scenario: A file asset destroyed inside an outer transaction that commits
     * ExpectedResult: Every version's binaries, cache and metadata are gone as soon as the outer
     * transaction's commit returns, on the calling thread
     */
    @Test
    void test_destroy_insideCommittedTransaction_removedWhenTheCommitReturns() throws Exception {
        final Versions versions = fileAssetWithTwoVersions();

        LocalTransaction.wrap(() -> contentletAPI.destroy(List.of(versions.working()), admin,
                false));

        assertRemoved(versions.older());
        assertRemoved(versions.working());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#destroy(List, User, boolean)}
     * Given Scenario: A file asset destroyed with no transaction open
     * ExpectedResult: Every version's binaries, cache and metadata are gone when the call returns,
     * as before the fix
     */
    @Test
    void test_destroy_outsideAnyTransaction_removedWhenTheCallReturns() throws Exception {
        final Versions versions = fileAssetWithTwoVersions();

        contentletAPI.destroy(List.of(versions.working()), admin, false);

        assertRemoved(versions.older());
        assertRemoved(versions.working());
    }

    /**
     * Method to test: deleting a content type synchronously, which destroys its content in the
     * same transaction after the type's fields are already gone
     * Given Scenario: A content type with a binary field and one piece of content with a file
     * ExpectedResult: The content's binaries and metadata are gone once the delete returns, even
     * though the type no longer exists by the time the transaction commits
     */
    @Test
    void test_deleteContentType_removesItsContentsFilesAndMetadata() throws Exception {
        final ContentType type = new ContentTypeDataGen().host(site).nextPersisted();
        final Field binaryField = new FieldDataGen().type(BinaryField.class)
                .contentTypeId(type.id()).nextPersisted();
        final Contentlet content = new ContentletDataGen(type.id()).host(site)
                .setProperty(binaryField.variable(), newTempFile())
                .nextPersisted();
        prepareFilesAndMetadata(content, binaryField.variable());
        assertKept(content, binaryField.variable());

        APILocator.getContentTypeAPI(admin).deleteSync(type);

        assertFalse(assetDirectory(content).exists(),
                "the content's binary directory must be gone once the type delete returns");
        assertFalse(metadataExists(content, binaryField.variable()),
                "the content's metadata must be gone once the type delete returns");
    }

    /**
     * Method to test: the deferred removal's error handling
     * Given Scenario: In one transaction, a removal part that throws, a destroy, and then another
     * after-commit listener
     * ExpectedResult: The transaction commits without an error reaching the caller, the later
     * listener still runs, and the destroyed content's files and metadata are gone
     */
    @Test
    void test_failingRemovalPart_isLogged_destroyAndLaterListenerStillComplete()
            throws Exception {
        final Versions versions = fileAssetWithTwoVersions();
        final AtomicBoolean laterListenerRan = new AtomicBoolean();

        assertDoesNotThrow(() -> LocalTransaction.wrap(() -> {
            BinaryRemovalAfterCommit.schedule("a part that fails on purpose", () -> {
                throw new IllegalStateException("removal failure forced by the test");
            });
            contentletAPI.destroy(List.of(versions.working()), admin, false);
            HibernateUtil.addSyncCommitListener(() -> laterListenerRan.set(true));
        }));

        assertTrue(laterListenerRan.get(),
                "a listener registered after the failing removal must still run");
        assertRemoved(versions.older());
        assertRemoved(versions.working());
    }

    /**
     * Method to test: the deferred removal's error handling, on a real I/O failure
     * Given Scenario: A file asset destroyed inside a transaction, with its binary directory made
     * read-only so the files under it cannot be removed after the commit
     * ExpectedResult: The commit returns without an error reaching the caller, the binary
     * directory is left in place (and its path logged as left behind), and the rest of the
     * removal still runs: the resized-image cache is gone and the destroy committed
     */
    @Test
    void test_directoryThatCannotBeRemoved_isLeftInPlace_restOfRemovalStillRuns()
            throws Exception {
        final Versions versions = fileAssetWithTwoVersions();
        final File assetDirectory = assetDirectory(versions.working());
        assertTrue(assetDirectory.setWritable(false, false),
                "precondition: the binary directory can be made read-only");
        try {
            assertDoesNotThrow(() -> LocalTransaction.wrap(() -> contentletAPI.destroy(
                    List.of(versions.working()), admin, false)));

            assertTrue(assetDirectory.exists(),
                    "a directory that could not be removed is left in place");
            // The metadata is not asserted: with file system storage it is kept under the same
            // read-only directory, so it is left in place too. The cache is a separate directory.
            assertFalse(cacheDirectory(versions.working()).exists(),
                    "the rest of the removal still runs: the cache must be gone");
            assertNull(contentletAPI.find(versions.working().getInode(), admin, false),
                    "the destroy itself committed");
        } finally {
            assetDirectory.setWritable(true, false);
            com.liferay.util.FileUtil.deltree(assetDirectory);
        }
    }

    /** The two versions of one file asset, re-read from the database. */
    private record Versions(Contentlet older, Contentlet working) { }

    /**
     * A file asset with two versions, each with its binary directory, a resized-image cache
     * directory and stored metadata in place.
     */
    private Versions fileAssetWithTwoVersions() throws Exception {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final Contentlet older = new FileAssetDataGen(folder, "first version").nextPersisted();
        final Contentlet working = ContentletDataGen.checkin(ContentletDataGen.checkout(older));

        final Contentlet olderReRead = contentletAPI.find(older.getInode(), admin, false);
        final Contentlet workingReRead = contentletAPI.find(working.getInode(), admin, false);
        prepareFilesAndMetadata(olderReRead, FileAssetAPI.BINARY_FIELD);
        prepareFilesAndMetadata(workingReRead, FileAssetAPI.BINARY_FIELD);
        assertKept(olderReRead);
        assertKept(workingReRead);
        return new Versions(olderReRead, workingReRead);
    }

    /** Makes sure the version's metadata is stored and it has a resized-image cache directory. */
    private static void prepareFilesAndMetadata(final Contentlet contentlet,
            final String binaryField) throws Exception {
        fileMetadataAPI.generateContentletMetadata(contentlet);
        final File cacheDirectory = new File(cacheDirectory(contentlet), binaryField);
        assertTrue(cacheDirectory.mkdirs() || cacheDirectory.isDirectory());
        Files.writeString(new File(cacheDirectory, "resized.txt").toPath(), "resized");
    }

    /** Runs the work inside a transaction that is then rolled back. */
    private static void rollBackAfter(final VoidDelegate work) {
        assertThrows(ForcedRollback.class, () -> LocalTransaction.wrap(() -> {
            work.execute();
            throw new ForcedRollback();
        }));
    }

    private static void assertKept(final Contentlet contentlet) throws DotDataException {
        assertKept(contentlet, FileAssetAPI.BINARY_FIELD);
    }

    private static void assertKept(final Contentlet contentlet, final String binaryField)
            throws DotDataException {
        assertTrue(assetDirectory(contentlet).exists(),
                "binary directory missing for " + contentlet.getInode());
        assertTrue(cacheDirectory(contentlet).exists(),
                "resized-image cache directory missing for " + contentlet.getInode());
        assertTrue(metadataExists(contentlet, binaryField),
                "metadata missing for " + contentlet.getInode());
    }

    private static void assertRemoved(final Contentlet contentlet) throws DotDataException {
        assertFalse(assetDirectory(contentlet).exists(),
                "binary directory still on disk for " + contentlet.getInode());
        assertFalse(cacheDirectory(contentlet).exists(),
                "resized-image cache directory still on disk for " + contentlet.getInode());
        assertFalse(metadataExists(contentlet, FileAssetAPI.BINARY_FIELD),
                "metadata still stored for " + contentlet.getInode());
    }

    private static File assetDirectory(final Contentlet contentlet) {
        final String inode = contentlet.getInode();
        return new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator
                + inode.charAt(0) + File.separator + inode.charAt(1) + File.separator + inode);
    }

    private static File cacheDirectory(final Contentlet contentlet) {
        final String inode = contentlet.getInode();
        return new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator
                + "cache" + File.separator + inode.charAt(0) + File.separator + inode.charAt(1)
                + File.separator + inode);
    }

    /** Whether the version's metadata for a binary field is in the configured storage. */
    private static boolean metadataExists(final Contentlet contentlet, final String binaryField)
            throws DotDataException {
        final String group = Config.getStringProperty(FileMetadataAPI.METADATA_GROUP_NAME,
                FileMetadataAPI.DOT_METADATA);
        return StoragePersistenceProvider.INSTANCE.get()
                .getStorage(StoragePersistenceProvider.getStorageType())
                .existsObject(group, fileMetadataAPI.getFileName(contentlet, binaryField));
    }

    private static File newTempFile() throws Exception {
        final File file = Files.createTempFile("deferred-removal", ".txt").toFile();
        Files.writeString(file.toPath(), "binary content");
        return file;
    }

    /** Thrown inside a transaction only to make it roll back. */
    private static final class ForcedRollback extends DotDataException {

        ForcedRollback() {
            super("rollback forced by the test");
        }
    }
}
