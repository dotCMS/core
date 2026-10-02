package com.dotcms.storage.binary;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.StorageKey;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Resumable copy of referenced originals and metadata; never removes the local source. */
public final class BinaryAssetBackfill {
    private BinaryAssetBackfill() { }

    /**
     * The outcome of one batch.
     *
     * @param afterInode    the last inode visited, which is the cursor for the next batch
     * @param binaries      the number of binaries verified in durable storage
     * @param complete      whether the scan reached the end of the contentlet table
     * @param skippedInodes inodes left incomplete because a referenced binary or metadata record
     *                      exists neither locally nor in S3, or because the content row could not
     *                      be read; each one was logged
     */
    public record Result(String afterInode, int binaries, boolean complete, List<String> skippedInodes) { }

    /**
     * Verifies one page of content rows in inode order. Problems in the data itself, which a retry
     * cannot fix, are logged and reported in {@link Result#skippedInodes()} instead of failing the
     * batch: a binary or metadata record that exists neither locally nor in S3, and a row whose JSON
     * cannot be parsed or whose legacy content cannot be found. This matches the legacy tolerance of
     * starter import for starters that do not carry every referenced binary. Storage and database
     * errors still fail the batch, so a broken bucket is never mistaken for a missing file.
     *
     * @param afterInode     exclusive inode cursor; an empty string starts from the beginning
     * @param limit          the number of content rows to visit, from 1 to 1000
     * @param afterEachInode called after each visited inode, for example to send a job heartbeat
     * @return the batch outcome
     * @throws DotDataException if storage or the database fails; the message names the inode
     */
    @CloseDBIfOpened
    public static Result runBatch(final String afterInode, final int limit, final Runnable afterEachInode)
            throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new IllegalStateException("S3 asset storage is disabled");
        }
        if (afterInode == null || limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Provide an inode cursor and a batch size between 1 and 1000");
        }
        // The limit must be in the SQL: DotConnect.setMaxRows only trims rows that were already fetched.
        final var rows = new DotConnect().setSQL("select inode from contentlet where inode > ? order by inode", limit)
                .addParam(afterInode).loadObjectResults();
        String cursor = afterInode;
        int binaries = 0;
        final Set<String> skipped = new LinkedHashSet<>();
        for (final var row : rows) {
            final String inode = row.get("inode").toString();
            binaries += copyInode(inode, skipped);
            cursor = inode;
            afterEachInode.run();
        }
        return new Result(cursor, binaries, rows.size() < limit, List.copyOf(skipped));
    }

    /**
     * Verifies every content row; starter import calls this before its commit. Skipped inodes are
     * logged individually by {@link #runBatch} and counted in a summary at the end.
     *
     * @throws DotDataException if storage or the database fails
     */
    public static void runAll() throws DotDataException {
        String cursor = "";
        int skipped = 0;
        while (true) {
            final Result result = runBatch(cursor, 250, () -> { });
            skipped += result.skippedInodes().size();
            Logger.info(BinaryAssetBackfill.class, "S3 backfill verified " + result.binaries()
                    + " binaries through inode " + result.afterInode());
            if (result.complete()) {
                if (skipped > 0) {
                    Logger.warn(BinaryAssetBackfill.class, "S3 backfill skipped " + skipped + " content versions"
                            + " whose binaries or metadata exist neither locally nor in S3, or whose rows could"
                            + " not be read; the preceding warnings name each inode");
                }
                return;
            }
            cursor = result.afterInode();
        }
    }

    /**
     * Verifies the originals, metadata and renditions of one content row while holding its row lock.
     * Data problems that a retry cannot fix are logged and add the inode to {@code skipped}; the other
     * fields of the row are still copied.
     *
     * @param inode   the content version to copy
     * @param skipped receives the inode when part of it had to be skipped
     * @return the number of binaries verified
     * @throws DotDataException if storage or the database fails
     */
    private static int copyInode(final String inode, final Set<String> skipped) throws DotDataException {
        final boolean localTransaction = HibernateUtil.startLocalTransactionIfNeeded();
        try {
            // Coordinate with content updates/deletion, including the durable cleanup job.
            final var rows = new DotConnect().setSQL("select contentlet_as_json from contentlet where inode = ? for update")
                    .addParam(inode).loadObjectResults();
            int copied = 0;
            if (!rows.isEmpty()) {
                final Object json = rows.get(0).get("contentlet_as_json");
                final Contentlet content = readContent(inode, json);
                if (content == null) {
                    skip(inode, null, "its content row could not be read", skipped);
                } else {
                    final var metadataAPI = APILocator.getFileMetadataAPI();
                    final var binaryAPI = APILocator.getBinaryAssetStorageAPI();
                    final String snapshot = json == null || json.toString().isBlank()
                            ? APILocator.getContentletJsonAPI().toJson(content) : json.toString();
                    try (var lease = binaryAPI.acquireCacheLease()) {
                        for (final var field : BinaryAssetReference.fromContentJson(snapshot, inode).entrySet()) {
                            final var reference = field.getValue();
                            final File file = reference.localFile(inode, field.getKey());
                            if (!file.isFile()) {
                                // Restores from S3. A storage error propagates; absence everywhere is skipped.
                                try (var ignored = binaryAPI.openLocalFile(file)) {
                                } catch (NoSuchFileException absent) {
                                    skip(inode, field.getKey(), "the binary exists neither locally nor in S3", skipped);
                                    continue;
                                }
                            }
                            if (!binaryAPI.backfillBinary(inode, field.getKey(), file)) {
                                throw new DotDataException("No durable copy for " + inode + "/" + field.getKey());
                            }
                            content.getMap().put(field.getKey(), file);
                            final boolean metadataCopied = APILocator.getFileStorageAPI().backfillMetadata(new StorageKey.Builder()
                                    .group(Config.getStringProperty(StoragePersistenceProvider.METADATA_GROUP_NAME, FileMetadataAPI.DOT_METADATA))
                                    .path(metadataAPI.getFileName(content, field.getKey()))
                                    .storage(StoragePersistenceProvider.getStorageType()).build());
                            if (!metadataCopied && BinaryAssetReference.metadataKeyOf(file) != null) {
                                skip(inode, field.getKey(), "its referenced metadata exists neither locally nor in S3", skipped);
                            }
                            copied++;
                        }
                        binaryAPI.backfillGeneratedFiles(inode);
                    }
                }
            }
            if (localTransaction) {
                HibernateUtil.commitTransaction();
            }
            return copied;
        } catch (Exception failure) {
            if (localTransaction) {
                HibernateUtil.rollbackTransaction();
            }
            throw new DotDataException("Unable to backfill content " + inode, failure);
        }
    }

    /**
     * Reads a locked content row.
     *
     * @param inode the content version
     * @param json  the persisted {@code contentlet_as_json}, or null for a legacy row
     * @return the content, or null when the JSON cannot be parsed or a legacy row has no content
     * @throws DotDataException     if the database fails
     * @throws DotSecurityException if the system user cannot read the content
     */
    private static Contentlet readContent(final String inode, final Object json)
            throws DotDataException, DotSecurityException {
        if (json == null || json.toString().isBlank()) {
            final Contentlet found = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
            return found == null ? null : new Contentlet(found);
        }
        try {
            return APILocator.getContentletJsonAPI().toMutableContentlet(
                    ContentletJsonHelper.INSTANCE.get().immutableFromJson(json.toString()));
        } catch (JsonProcessingException unreadable) {
            Logger.warn(BinaryAssetBackfill.class, "Invalid content JSON for " + inode + ": " + unreadable.getMessage());
            return null;
        }
    }

    /**
     * Logs a data problem that a retry cannot fix and records its inode for the caller's report.
     *
     * @param inode   the affected content version
     * @param field   the affected field, or null for the whole row
     * @param reason  why it was skipped, phrased to follow "because"
     * @param skipped receives the inode
     */
    private static void skip(final String inode, final String field, final String reason, final Set<String> skipped) {
        Logger.warn(BinaryAssetBackfill.class, "S3 backfill skipped " + inode
                + (field == null ? "" : "/" + field) + " because " + reason);
        skipped.add(inode);
    }
}
