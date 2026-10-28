package com.dotmarketing.portlets.folders.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.jobs.business.processor.impl.FolderBulkDeleteProcessor;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for #37782: a folder delete that fails partway through its tree rolls back the
 * database and must leave every file it already reached on disk, with its metadata.
 * <p>
 * The tree is the spec's reproduction: a top folder with two subfolders walked in title order, a
 * <em>walked</em> one holding a file asset with two versions, and a second one on which the delete
 * fails. The deleter is a non-admin Back-end User, since an administrator passes every check.
 */
public class FolderDeleteKeepsFilesOnRollbackIT {

    private static User admin;
    private static Host site;
    private static FileMetadataAPI fileMetadataAPI;

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        fileMetadataAPI = APILocator.getFileMetadataAPI();
    }

    /**
     * Method to test: {@link FolderAPIImpl#delete(Folder, User, boolean)}
     * Given Scenario: The second subfolder refuses the deleter Edit Permissions, so the delete
     * fails after the walked subfolder's file asset was destroyed
     * ExpectedResult: The delete fails and every version of the walked file keeps its binaries
     * and metadata
     */
    @Test
    void test_singleFolderDelete_refusedPartway_keepsTheWalkedFile() throws Exception {
        final Tree tree = tree(false);

        final Exception failure = assertThrows(Exception.class,
                () -> APILocator.getFolderAPI().delete(tree.top(), tree.deleter(), false));

        assertTrue(messagesOf(failure).contains("b-second"),
                "the delete must fail on the second subfolder, after the walked file: "
                        + messagesOf(failure));
        assertFileKept(tree);
    }

    /** Every message in an exception's cause chain, joined, to tell where a delete failed. */
    private static String messagesOf(final Throwable failure) {
        final StringBuilder messages = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append(" | ");
        }
        return messages.toString();
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: The same tree, deleted through the bulk folder delete
     * ExpectedResult: The top folder is FAILED/PERMISSION_DENIED and every version of the walked
     * file keeps its binaries and metadata
     */
    @Test
    void test_bulkDelete_refusedPartway_keepsTheWalkedFile() throws Exception {
        final Tree tree = tree(false);
        final String topPath = String.format("//%s/%s/", site.getHostname(), tree.top().getName());

        final Job job = jobFor(topPath, tree.deleter());
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        final BatchItemResult result = resultFor(processor.getResultMetadata(job), topPath);
        assertEquals(BatchItemStatus.FAILED, result.status(),
                "reason: " + result.reason() + ", message: " + result.message());
        assertEquals(BatchFailureReason.PERMISSION_DENIED, result.reason().orElseThrow());
        assertFileKept(tree);
    }

    /**
     * Method to test: {@link FolderAPIImpl#delete(Folder, User, boolean)}
     * Given Scenario: No permission is restricted, but the second subfolder holds content locked
     * by another user, so the delete fails on the lock after the walked file was destroyed
     * ExpectedResult: The delete fails and every version of the walked file keeps its binaries
     * and metadata
     */
    @Test
    void test_singleFolderDelete_lockedContentPartway_keepsTheWalkedFile() throws Exception {
        final Tree tree = tree(true);

        assertThrows(Exception.class,
                () -> APILocator.getFolderAPI().delete(tree.top(), tree.deleter(), false));

        assertFileKept(tree);
    }

    /**
     * Method to test: {@link FolderAPIImpl#delete(Folder, User, boolean)}
     * Given Scenario: A folder holding a file asset with two versions, deleted successfully
     * ExpectedResult: As soon as the delete returns, every version's binary directory,
     * resized-image cache directory and metadata are gone (AC-003)
     */
    @Test
    void test_singleFolderDelete_succeeds_clearsFilesWhenItReturns() throws Exception {
        final DeletableFolder deletable = deletableFolder();

        APILocator.getFolderAPI().delete(deletable.folder(), admin, false);

        assertFilesRemoved(deletable);
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: The same folder, deleted through the bulk folder delete
     * ExpectedResult: The folder is SUCCESS and every version's binary directory, resized-image
     * cache directory and metadata are gone once the run returns: the folder's transaction
     * commits, and the removal runs, before its outcome is recorded (AC-003)
     */
    @Test
    void test_bulkDelete_succeeds_clearsFilesBeforeTheOutcomeIsRecorded() throws Exception {
        final DeletableFolder deletable = deletableFolder();
        final String path = String.format("//%s/%s/", site.getHostname(),
                deletable.folder().getName());

        final Job job = jobFor(path, admin);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        assertEquals(BatchItemStatus.SUCCESS,
                resultFor(processor.getResultMetadata(job), path).status());
        assertFilesRemoved(deletable);
    }

    /** A folder holding one file asset with two versions, each with a resized-image cache. */
    private record DeletableFolder(Folder folder, List<Contentlet> versions) { }

    private DeletableFolder deletableFolder() throws Exception {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final Contentlet older = new FileAssetDataGen(folder, "first version").nextPersisted();
        final Contentlet working = ContentletDataGen.checkin(ContentletDataGen.checkout(older));
        final List<Contentlet> versions = List.of(
                APILocator.getContentletAPI().find(older.getInode(), admin, false),
                APILocator.getContentletAPI().find(working.getInode(), admin, false));
        for (final Contentlet version : versions) {
            fileMetadataAPI.generateContentletMetadata(version);
            final File cache = new File(cacheDirectory(version), FileAssetAPI.BINARY_FIELD);
            assertTrue(cache.mkdirs() || cache.isDirectory());
            Files.writeString(new File(cache, "resized.txt").toPath(), "resized");
            assertTrue(assetDirectory(version).exists() && metadataExists(version),
                    "precondition: files and metadata in place for " + version.getInode());
        }
        return new DeletableFolder(folder, versions);
    }

    private static void assertFilesRemoved(final DeletableFolder deletable) throws Exception {
        for (final Contentlet version : deletable.versions()) {
            assertFalse(assetDirectory(version).exists(),
                    "binary directory still on disk for " + version.getInode());
            assertFalse(cacheDirectory(version).exists(),
                    "resized-image cache directory still on disk for " + version.getInode());
            assertFalse(metadataExists(version),
                    "metadata still stored for " + version.getInode());
        }
    }

    private static File cacheDirectory(final Contentlet contentlet) {
        final String inode = contentlet.getInode();
        return new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator
                + "cache" + File.separator + inode.charAt(0) + File.separator + inode.charAt(1)
                + File.separator + inode);
    }

    /** The reproduction tree, the walked file's two versions and the user deleting it. */
    private record Tree(Folder top, Contentlet olderVersion, Contentlet workingVersion,
            User deleter) { }

    /**
     * Builds the reproduction tree.
     *
     * @param lockInsteadOfRefuse when true, the second subfolder keeps every right and holds
     *                            content locked by another user; when false, the second subfolder
     *                            refuses the deleter Edit Permissions
     */
    private Tree tree(final boolean lockInsteadOfRefuse) throws Exception {
        final Role backEndUserRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final User deleter = new UserDataGen().roles(backEndUserRole).nextPersisted();
        final String deleterRoleId = roleIdOf(deleter);
        final User locker = new UserDataGen().roles(backEndUserRole).nextPersisted();
        final String lockerRoleId = roleIdOf(locker);
        grant(site, List.of(individual(site, deleterRoleId, PermissionAPI.PERMISSION_READ),
                individual(site, lockerRoleId, PermissionAPI.PERMISSION_READ)));

        final String suffix = UUID.randomUUID().toString().substring(0, 8);
        final Folder top = new FolderDataGen().site(site).name("top-" + suffix)
                .title("top-" + suffix).nextPersisted();
        final Folder walked = new FolderDataGen().parent(top).name("a-walked")
                .title("a-walked").nextPersisted();
        final Folder second = new FolderDataGen().parent(top).name("b-second")
                .title("b-second").nextPersisted();

        final int folderRights = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS | PermissionAPI.PERMISSION_PUBLISH;
        final int contentRights = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                | PermissionAPI.PERMISSION_PUBLISH;
        final String folderType = Folder.class.getCanonicalName();
        final String contentType = Contentlet.class.getCanonicalName();
        grant(top, List.of(
                individual(top, deleterRoleId, folderRights),
                new Permission(folderType, top.getPermissionId(), deleterRoleId, folderRights,
                        true),
                new Permission(contentType, top.getPermissionId(), deleterRoleId, contentRights,
                        true),
                new Permission(contentType, top.getPermissionId(), lockerRoleId,
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true)));

        // Created after the grants: the folder delete finds content through the index, whose
        // documents carry the permissions they had when indexed.
        final Contentlet older = new FileAssetDataGen(walked, "first version").nextPersisted();
        final Contentlet working = ContentletDataGen.checkin(ContentletDataGen.checkout(older));
        // A checkin leaves the content locked by whoever saved it; unlocked, so the delete fails
        // on the second subfolder and not on the walked file itself.
        APILocator.getContentletAPI().unlock(working, admin, false);
        assertFalse(APILocator.getVersionableAPI().getLockedBy(working).isPresent(),
                "the walked file must not be locked, or the delete fails on it first");
        assertFalse(APILocator.getContentletAPI()
                        .findContentletsByFolder(walked, deleter, false).isEmpty(),
                "the deleter must find the walked file, or the delete never destroys it and the "
                        + "test proves nothing");

        if (lockInsteadOfRefuse) {
            final ContentType type = new ContentTypeDataGen().nextPersisted();
            final Contentlet lockedContent = new ContentletDataGen(type.id()).host(site)
                    .folder(second).nextPersisted();
            APILocator.getContentletAPI().lock(lockedContent, locker, false);
        } else {
            grant(second, List.of(individual(second, deleterRoleId,
                    PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                            | PermissionAPI.PERMISSION_PUBLISH)));
        }

        final Contentlet olderReRead = APILocator.getContentletAPI()
                .find(older.getInode(), admin, false);
        final Contentlet workingReRead = APILocator.getContentletAPI()
                .find(working.getInode(), admin, false);
        fileMetadataAPI.generateContentletMetadata(olderReRead);
        fileMetadataAPI.generateContentletMetadata(workingReRead);
        final Tree tree = new Tree(top, olderReRead, workingReRead, deleter);
        assertFileKept(tree);
        return tree;
    }

    private static String roleIdOf(final User user) throws DotDataException {
        return APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId();
    }

    private static Permission individual(final com.dotmarketing.business.Permissionable target,
            final String roleId, final int rights) {
        return new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, target.getPermissionId(),
                roleId, rights, true);
    }

    private static void grant(final com.dotmarketing.business.Permissionable target,
            final List<Permission> permissions) throws Exception {
        APILocator.getPermissionAPI().save(permissions, target, admin, false);
    }

    /** Both versions of the walked file are still in the database, on disk and in storage. */
    private static void assertFileKept(final Tree tree) throws Exception {
        for (final Contentlet version : List.of(tree.olderVersion(), tree.workingVersion())) {
            assertNotNull(APILocator.getContentletAPI().find(version.getInode(), admin, false),
                    "the version's row must still be there: " + version.getInode());
            assertTrue(assetDirectory(version).exists(),
                    "binary directory missing for " + version.getInode());
            assertTrue(metadataExists(version),
                    "metadata missing for " + version.getInode());
        }
    }

    private static File assetDirectory(final Contentlet contentlet) {
        final String inode = contentlet.getInode();
        return new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator
                + inode.charAt(0) + File.separator + inode.charAt(1) + File.separator + inode);
    }

    private static boolean metadataExists(final Contentlet contentlet) throws DotDataException {
        final String group = Config.getStringProperty(FileMetadataAPI.METADATA_GROUP_NAME,
                FileMetadataAPI.DOT_METADATA);
        return StoragePersistenceProvider.INSTANCE.get()
                .getStorage(StoragePersistenceProvider.getStorageType())
                .existsObject(group,
                        fileMetadataAPI.getFileName(contentlet, FileAssetAPI.BINARY_FIELD));
    }

    private static Job jobFor(final String path, final User submittingUser) {
        final Map<String, Object> pathParam = new HashMap<>();
        pathParam.put("path", path);
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", submittingUser.getUserId());
        parameters.put("paths", List.of(pathParam));
        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName(FolderBulkDeleteHelper.QUEUE_NAME)
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(new DefaultProgressTracker())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static BatchItemResult resultFor(final Map<String, Object> metadata,
            final String key) {
        return ((List<BatchItemResult>) metadata.get("results")).stream()
                .filter(r -> r.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no result recorded for " + key));
    }
}
