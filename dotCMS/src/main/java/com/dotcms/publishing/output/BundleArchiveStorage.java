package com.dotcms.publishing.output;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.ChainableStoragePersistenceAPIBuilder;
import com.dotcms.storage.FileSystemStoragePersistenceAPIImpl;
import com.dotcms.storage.ObjectSnapshot;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.google.common.util.concurrent.Striped;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/** Completed publishing archives, including their manifest, share one durable object and local cache. */
public final class BundleArchiveStorage {
    public static final String GROUP = "publishing-bundles";
    private static final String SUFFIX = ".tar.gz";
    private StoragePersistenceAPI storage;
    /** The durable provider, when this class built the chain itself; used to list archives by age. */
    private StoragePersistenceAPI remote;
    private boolean initialized;
    private final Striped<Lock> archiveLocks = Striped.lock(64);

    private static class Holder {
        private static final BundleArchiveStorage INSTANCE = new BundleArchiveStorage(null);
    }

    public static BundleArchiveStorage getInstance() {
        return Holder.INSTANCE;
    }

    public BundleArchiveStorage(final StoragePersistenceAPI storage) {
        this.storage = storage;
    }

    /** Resolves a pathname without downloading, for producers and cleanup. */
    public static File localArchive(final String id) {
        final File file = new File(ConfigUtils.getBundlePath(), id + ".tar.gz");
        if (AssetStorageFeature.isEnabled()) {
            if (id == null || id.isBlank() || id.equals(".") || id.equals("..")
                    || id.contains("/") || id.contains("\\") || id.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("Invalid bundle id");
            }
            try {
                if (!file.getCanonicalFile().toPath().equals(new File(ConfigUtils.getBundlePath())
                        .getCanonicalFile().toPath().resolve(id + ".tar.gz"))) {
                    throw new IllegalArgumentException("Bundle archive is outside the bundle directory");
                }
            } catch (IOException e) {
                throw new DotRuntimeException(e);
            }
        }
        return file;
    }

    private synchronized StoragePersistenceAPI storage() throws DotDataException {
        if (storage == null) {
            remote = StoragePersistenceProvider.remoteObjectStorage();
            storage = new ChainableStoragePersistenceAPIBuilder()
                    .add(StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM))
                    .add(remote).get();
        }
        if (!initialized) {
            final File root = new File(ConfigUtils.getBundlePath());
            if (!root.isDirectory() && !root.mkdirs()) {
                throw new DotDataException("Unable to create bundle directory " + root);
            }
            storage.createGroup(GROUP, Map.of(FileSystemStoragePersistenceAPIImpl.GROUP_DIRECTORY, root));
            initialized = true;
        }
        return storage;
    }

    /** Missing objects retain the expected pathname; storage failures are never reported as absence. */
    public boolean exists(final String id) {
        final File file = localArchive(id);
        if (file.isFile() || !AssetStorageFeature.isEnabled()) return file.isFile();
        try {
            return storage().existsObject(GROUP, file.getName());
        } catch (DotDataException e) {
            throw new DotRuntimeException("Unable to check publishing archive " + id, e);
        }
    }

    /**
     * Tells a page whether to show a bundle's archive as available, for example to offer a download
     * link or a retry button. Unlike {@link #exists(String)}, a storage failure or an invalid id is
     * logged and reported as {@code false}, so an S3 outage cannot stop a bundle listing from
     * rendering. Publishing and retry decisions must keep using {@link #exists(String)}.
     *
     * @param id the bundle id
     * @return true if the archive is known to exist, false if it is missing or could not be checked
     */
    public boolean existsForDisplay(final String id) {
        try {
            return exists(id);
        } catch (RuntimeException e) {
            Logger.warn(BundleArchiveStorage.class,
                    "Unable to check publishing archive " + id + ", showing it as missing: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the lock that orders storing an archive against deleting it. Storing a bundle's archive
     * holds it, and the cleanup job holds it while it checks that the bundle row is gone and deletes
     * the archive, so on one node a bundle received again cannot lose its new archive between that
     * check and the delete. The lock is local to this server.
     *
     * @param id the bundle id
     * @return the lock for that bundle id
     */
    public Lock archiveLock(final String id) {
        return archiveLocks.get(id);
    }

    /** Restores only when the archive bytes are required. */
    public File get(final String id) {
        final File file = localArchive(id);
        if (!AssetStorageFeature.isEnabled()) return file;
        try {
            final File restored = storage().pullFile(GROUP, file.getName());
            return restored == null ? file : restored;
        } catch (DotDataException e) {
            throw new DotRuntimeException("Unable to retrieve publishing archive " + id, e);
        }
    }

    /**
     * Publishes a closed, complete archive as the bundle's archive. The chain stores it in S3 before
     * it replaces the local copy. Holds {@link #archiveLock(String)} while storing, so the cleanup
     * job cannot delete it between checking for the bundle row and deleting.
     *
     * @param id     the bundle id
     * @param source the complete archive to publish; the caller keeps ownership of this file
     * @throws IOException if the archive cannot be stored
     */
    public void store(final String id, final File source) throws IOException {
        if (!AssetStorageFeature.isEnabled()) return;
        final File archive = localArchive(id);
        final Lock lock = archiveLock(id);
        lock.lock();
        try {
            storage().pushFile(GROUP, archive.getName(), source, Map.of());
        } catch (DotDataException e) {
            throw new IOException("Unable to store publishing archive " + id, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Saves an uploaded bundle archive. The upload is copied to a private staging file first, so an
     * incomplete upload never replaces the last complete archive. The bundle id is the part of the
     * file name before the first {@code .tar.gz}, the same rule the receiving endpoints and
     * {@code BundlePublisher} use to find the archive again, so any name they accept is stored
     * where they will read it.
     *
     * @param fileName the uploaded file name, which must contain {@code .tar.gz}
     * @param input    the upload; the caller owns and closes it
     * @throws IOException if the upload cannot be read or stored
     */
    public void receive(final String fileName, final InputStream input) throws IOException {
        final int suffix = fileName.indexOf(SUFFIX);
        if (suffix < 0) throw new IllegalArgumentException("Expected a bundle archive");
        final String id = fileName.substring(0, suffix);
        final Path target = localArchive(id).toPath();
        if (!AssetStorageFeature.isEnabled()) {
            com.dotmarketing.util.FileUtil.writeToFile(input, target.toString());
            return;
        }
        Files.createDirectories(target.getParent());
        final Path stage = Files.createTempFile(target.getParent(), ".bundle-upload-", ".tmp");
        try {
            Files.copy(input, stage, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            store(id, stage.toFile());
        } finally {
            Files.deleteIfExists(stage);
        }
    }

    /** Called after committed bundle deletion; retains local bytes if remote deletion fails. */
    public void delete(final String id) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) return;
        final File archive = localArchive(id);
        storage().deleteObjectAndReferences(GROUP, archive.getName());
        final File extracted = new File(archive.getParentFile(), id);
        try {
            if (!extracted.getCanonicalFile().toPath().equals(archive.getCanonicalFile().getParentFile().toPath().resolve(id))) {
                throw new DotDataException("Bundle extraction directory is outside the bundle directory");
            }
            com.liferay.util.FileUtil.deltree(extracted);
            if (archive.exists() || extracted.exists()) {
                throw new DotDataException("Unable to remove bundle cache " + id);
            }
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    /**
     * Deletes durable archives last written before the cutoff, together with their local copy and
     * extraction directory. This is the S3 counterpart of the age-based bundle directory cleanup in
     * {@code BinaryCleanupJob}, so archives in S3 are kept for the same number of days as local
     * ones. An archive that cannot be deleted is logged and left for the next run.
     *
     * @param cutoff archives whose stored last-modified time is strictly before this are deleted
     * @return the number of archives deleted
     * @throws DotDataException if the stored archives cannot be listed
     */
    public int expireOlderThan(final Instant cutoff) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) return 0;
        final StoragePersistenceAPI chain = storage();
        int deleted = 0;
        for (final ObjectSnapshot object : (remote == null ? chain : remote).listObjectSnapshots(GROUP, "")) {
            final String name = object.path();
            if (object.modified() >= cutoff.toEpochMilli() || !name.endsWith(SUFFIX)) continue;
            final String id = name.substring(0, name.length() - SUFFIX.length());
            try {
                delete(id);
                deleted++;
            } catch (DotDataException | RuntimeException e) {
                Logger.warn(BundleArchiveStorage.class, "Unable to expire publishing archive " + name
                        + ", it will be retried on the next cleanup: " + e.getMessage());
            }
        }
        return deleted;
    }

}
