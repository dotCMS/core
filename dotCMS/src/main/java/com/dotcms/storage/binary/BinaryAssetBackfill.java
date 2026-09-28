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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.File;

/** Resumable copy of referenced originals and metadata; never removes the local source. */
public final class BinaryAssetBackfill {
    private BinaryAssetBackfill() { }

    public record Result(String afterInode, int binaries, boolean complete) { }

    @CloseDBIfOpened
    public static Result runBatch(final String afterInode, final int limit) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new IllegalStateException("S3 asset storage is disabled");
        }
        if (afterInode == null || limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Provide an inode cursor and a batch size between 1 and 1000");
        }
        final var rows = new DotConnect().setSQL("select inode from contentlet where inode > ? order by inode")
                .addParam(afterInode).setMaxRows(limit).loadObjectResults();
        String cursor = afterInode;
        int binaries = 0;
        for (final var row : rows) {
            final String inode = row.get("inode").toString();
            binaries += copyInode(inode);
            cursor = inode;
        }
        return new Result(cursor, binaries, rows.size() < limit);
    }

    public static void runAll() throws DotDataException {
        String cursor = "";
        while (true) {
            final Result result = runBatch(cursor, 250);
            Logger.info(BinaryAssetBackfill.class, "S3 backfill verified " + result.binaries()
                    + " binaries through inode " + result.afterInode());
            if (result.complete()) {
                return;
            }
            cursor = result.afterInode();
        }
    }

    private static int copyInode(final String inode) throws DotDataException {
        final boolean localTransaction = HibernateUtil.startLocalTransactionIfNeeded();
        try {
            // Coordinate with content updates/deletion, including the durable cleanup job.
            final var rows = new DotConnect().setSQL("select contentlet_as_json from contentlet where inode = ? for update")
                    .addParam(inode).loadObjectResults();
            int copied = 0;
            if (!rows.isEmpty()) {
                final Object json = rows.get(0).get("contentlet_as_json");
                final var content = json == null || json.toString().isBlank()
                        ? new com.dotmarketing.portlets.contentlet.model.Contentlet(
                                APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false))
                        : APILocator.getContentletJsonAPI().toMutableContentlet(
                                ContentletJsonHelper.INSTANCE.get().immutableFromJson(json.toString()));
                final var metadataAPI = APILocator.getFileMetadataAPI();
                final var binaryAPI = APILocator.getBinaryAssetStorageAPI();
                final String snapshot = json == null || json.toString().isBlank()
                        ? APILocator.getContentletJsonAPI().toJson(content) : json.toString();
                try (var lease = binaryAPI.acquireCacheLease()) {
                    for (final var field : BinaryAssetReference.fromContentJson(snapshot, inode).entrySet()) {
                        final var reference = field.getValue();
                        final File file = reference.localFile(inode, field.getKey());
                        if (!file.isFile()) {
                            try (var ignored = binaryAPI.openLocalFile(file)) { }
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
                            throw new DotDataException("Missing referenced metadata for " + inode + "/" + field.getKey());
                        }
                        copied++;
                    }
                    binaryAPI.backfillGeneratedFiles(inode);
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
}
