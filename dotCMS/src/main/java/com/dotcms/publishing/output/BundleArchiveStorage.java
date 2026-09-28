package com.dotcms.publishing.output;

import com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.ChainableStoragePersistenceAPIBuilder;
import com.dotcms.storage.FileSystemStoragePersistenceAPIImpl;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Completed publishing archives, including their manifest, share one durable object and local cache. */
public final class BundleArchiveStorage {
    public static final String GROUP = "publishing-bundles";
    private StoragePersistenceAPI storage;
    private boolean initialized;

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
            storage = new ChainableStoragePersistenceAPIBuilder()
                    .add(StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM))
                    .add(AmazonS3StoragePersistenceAPIImpl.withPlainPaths()).get();
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

    /** The caller supplies a closed, complete archive. The chain publishes remotely before replacing local bytes. */
    public void store(final String id, final File source) throws IOException {
        if (!AssetStorageFeature.isEnabled()) return;
        final File archive = localArchive(id);
        try {
            storage().pushFile(GROUP, archive.getName(), source, Map.of());
        } catch (DotDataException e) {
            throw new IOException("Unable to store publishing archive " + id, e);
        }
    }

    /** Own staging file keeps incomplete uploads away from the last complete archive. Caller owns the input stream. */
    public void receive(final String fileName, final InputStream input) throws IOException {
        if (!fileName.endsWith(".tar.gz")) throw new IllegalArgumentException("Expected a bundle archive");
        final String id = fileName.substring(0, fileName.length() - ".tar.gz".length());
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

}
