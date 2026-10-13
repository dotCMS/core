package com.dotcms.storage;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import io.vavr.control.Try;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * This API implements a chainable pattern that encapsulates a list of {@link StoragePersistenceAPI}
 * instances. This allows Sys Admins to implement chainable storages, for instance: The first
 * provider could be a Redis server, the second one could be NFS, third one could be the database,
 * and finally AWS S3.</li>
 * <p>Additionally, the API incorporates a 404 cache layer in front of all storage providers. The
 * goal of this cache is to avoid going through all layers when the path was previously searched for
 * and wasn't found.
 *
 * @author jsanca
 */
public class ChainableStoragePersistenceAPI implements StoragePersistenceAPI {

    private final List<StoragePersistenceAPI> storagePersistenceAPIList;
    private final ObjectWriterDelegate defaultWriterDelegate;
    private final Chainable404StorageCache cache;

    /**
     * Per-key locks used with S3 asset storage on, so reads, writes and deletes of one key do not
     * interleave. They are shared by the whole process because they protect the key, not a chain:
     * re-initializing the storage provider builds a new chain while the old one may still be in use.
     * Each stripe is a {@link java.util.concurrent.locks.ReentrantLock}, so a thread can take the
     * lock of a key it already holds. Code holding one of these locks must not take another one
     * (for example by calling another locked operation of a chain for a different key): two keys
     * can map to different stripes, and two threads taking them in opposite order deadlock until
     * {@link #ASSET_LOCK_TIMEOUT_MINUTES} runs out.
     */
    private static final Striped<Lock> ASSET_LOCKS = Striped.lock(256);

    /** How long to wait for a key's lock. Generous because large binaries are transferred under it. */
    private static final long ASSET_LOCK_TIMEOUT_MINUTES = 15;

    private static final String SUBMITTER_NAME = Config.getStringProperty("COMPOSITE_STORAGE_SUBMITTER_NAME", "SubmitterCompositeStoragePersistenceAPI");

    protected ChainableStoragePersistenceAPI(final List<StoragePersistenceAPI> storagePersistenceAPIList) {
        this(new JsonWriterDelegate(), storagePersistenceAPIList, CacheLocator.getChainable404StorageCache());
    }

    protected ChainableStoragePersistenceAPI(final ObjectWriterDelegate defaultWriterDelegate,
                                          final List<StoragePersistenceAPI> storagePersistenceAPIList,
                                          final Chainable404StorageCache cache) {

        this.defaultWriterDelegate     = defaultWriterDelegate;
        this.storagePersistenceAPIList = storagePersistenceAPIList;
        this.cache                     = cache;
    }

    /**
     * This is method is just for testing purposes
     * @return Chainable404StorageCache
     */
    @VisibleForTesting
    public Chainable404StorageCache getCache() {
        return cache;
    }

    /**
     * Takes the lock of a storage key, waiting at most {@link #ASSET_LOCK_TIMEOUT_MINUTES}. The key
     * is lowercased because the filesystem provider stores keys that differ only by case in the same
     * file; variants that a provider keeps apart merely wait for each other.
     *
     * @param groupName the group the key belongs to
     * @param path      the storage key
     * @return the held lock, which the caller must unlock
     * @throws DotDataException if the wait times out or the thread is interrupted
     */
    private static Lock lockKey(final String groupName, final String path) throws DotDataException {
        final Lock lock = ASSET_LOCKS.get((groupName + "/" + path).toLowerCase(Locale.ROOT));
        try {
            if (!lock.tryLock(ASSET_LOCK_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                throw new DotDataException("Timed out waiting for storage key " + groupName + "/" + path);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DotDataException("Interrupted waiting for storage key " + groupName + "/" + path, e);
        }
        return lock;
    }

    /**
     * Called when the provider at {@code failedIndex} rejects a write. Writes go from the last
     * provider to the first, so every provider after it already holds the new value while it and
     * the providers before it still hold the previous one, and reads, which start at the first
     * provider, would keep serving that stale copy. Deletes those copies, best effort, so the next
     * read restores the new value from a later provider. Does nothing when the last provider, the
     * first one written, failed, because then nothing has changed.
     *
     * @param groupName   the group of the key being written
     * @param path        the key being written
     * @param failedIndex the position in the chain of the provider whose write failed
     */
    private void dropCopiesOlderThanAcceptedWrite(final String groupName, final String path,
                                                  final int failedIndex) {
        if (failedIndex == storagePersistenceAPIList.size() - 1) {
            return;
        }
        for (int i = failedIndex; i >= 0; i--) {
            final StoragePersistenceAPI storage = storagePersistenceAPIList.get(i);
            Try.run(() -> storage.deleteObjectAndReferences(groupName, path))
                    .onFailure(e -> Logger.error(this, "Stale copy of " + groupName + "/" + path
                            + " left in " + storage + "; reads may serve it", e));
        }
        cache.remove(groupName, path);
    }

    @Override
    public boolean existsGroup(final String groupName) {

        return this.storagePersistenceAPIList.stream().allMatch(storage -> Try.of(()->storage.existsGroup(groupName)).getOrElse(false));
    }

    @Override
    public boolean existsObject(final String groupName, final String objectPath) {
        if (AssetStorageFeature.isEnabled()) {
            for (StoragePersistenceAPI storage : storagePersistenceAPIList) {
                try {
                    if (storage.existsObject(groupName, objectPath)) {
                        return true;
                    }
                } catch (DotDataException e) {
                    throw new DotRuntimeException("Unable to check stored object " + objectPath, e);
                }
            }
            return false;
        }
        if (this.cache.is404(groupName, objectPath)) {
            return false;
        }
        final boolean anyMatch = this.storagePersistenceAPIList.stream().anyMatch(storage -> Try.of(()->storage.existsObject(groupName, objectPath)).getOrElse(false));
        if (!anyMatch) {
            this.cache.put404(groupName, objectPath);
        }
        return anyMatch;
    }

    @Override
    public boolean createGroup(final String groupName) throws DotDataException {

        boolean created = !this.storagePersistenceAPIList.isEmpty();

        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // by default the strategy is to create all the storages, we can make it configurable in the future
            created &= storage.createGroup(groupName);
        }

        return created;
    }

    @Override
    public boolean createGroup(String groupName, Map<String, Object> extraOptions) throws DotDataException {

        boolean created = !this.storagePersistenceAPIList.isEmpty();

        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // by default the strategy is to create all the storages, we can make it configurable in the future
            created &= storage.createGroup(groupName, extraOptions);
        }

        return created;
    }

    @Override
    public int deleteGroup(final String groupName) throws DotDataException {

        int deleteCount = 0;

        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // it would be great to have atomic here, so if one storage fails, all fail, but it is ok by now
            deleteCount += storage.deleteGroup(groupName);
        }

        // since we do not know if the group was on cache or not, we clear the cache anyway
        cache.clearCache();

        return deleteCount;
    }

    /**
     * Deletes the object from every provider. With S3 asset storage on, this happens under the key's
     * lock, from the last provider to the first, so the local copy goes only after the durable
     * providers accepted the deletion.
     *
     * @param groupName the group of the object
     * @param path      the object's path
     * @return {@code true} if every provider reported the object deleted
     * @throws DotDataException if a provider fails, or the key's lock cannot be taken in time
     */
    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {

        if (AssetStorageFeature.isEnabled()) {
            final Lock lock = lockKey(groupName, path);
            try {
                // Keep the local copy until all durable providers accept the deletion.
                boolean deleted = !storagePersistenceAPIList.isEmpty();
                for (int i = storagePersistenceAPIList.size() - 1; i >= 0; i--) {
                    deleted &= storagePersistenceAPIList.get(i).deleteObjectAndReferences(groupName, path);
                }
                cache.remove(groupName, path);
                return deleted;
            } finally {
                lock.unlock();
            }
        }

        boolean deleted = !this.storagePersistenceAPIList.isEmpty();

        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // it would be great to have atomic here, so if one storage fails, all fail, but it is ok by now
            deleted &= storage.deleteObjectAndReferences(groupName, path);
        }

        cache.remove(groupName, path);

        return deleted;
    }

    @Override
    public boolean deleteObjectReference(String groupName, String path) throws DotDataException {

        boolean deleted = !this.storagePersistenceAPIList.isEmpty();

        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // it would be great to have atomic here, so if one storage fails, all fail, but it is ok by now
            deleted &= storage.deleteObjectReference(groupName, path);
        }

        cache.remove(groupName, path);

        return deleted;
    }

    @Override
    public List<String> listGroups() throws DotDataException {

        final Set<String> uniqueGroups = new LinkedHashSet<>();

        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            uniqueGroups.addAll(storage.listGroups());
        }

        return List.copyOf(uniqueGroups);
    }

    @Override
    public List<String> listObjectPaths(final String groupName,
                                        final String pathPrefix) throws DotDataException {

        final Set<String> paths = new LinkedHashSet<>();
        for (final StoragePersistenceAPI storage : storagePersistenceAPIList) {
            paths.addAll(storage.listObjectPaths(groupName, pathPrefix));
        }
        return List.copyOf(paths);
    }

    @Override
    public boolean hasDurableCopy(final String groupName, final String path,
                                  final File file) throws DotDataException {
        for (final StoragePersistenceAPI storage : storagePersistenceAPIList) {
            if (storage.hasDurableCopy(groupName, path, file)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean backfillFile(final String groupName, final String path, final File file) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return false;
        }
        boolean durable = false;
        for (final StoragePersistenceAPI storage : storagePersistenceAPIList) {
            durable |= storage.backfillFile(groupName, path, file);
        }
        return durable;
    }

    @Override
    public boolean backfillObject(final String groupName, final String path, final ObjectWriterDelegate writer,
            final ObjectReaderDelegate reader, final Serializable object) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return false;
        }
        boolean durable = false;
        for (final StoragePersistenceAPI storage : storagePersistenceAPIList) {
            durable |= storage.backfillObject(groupName, path, writer, reader, object);
        }
        return durable;
    }

    /**
     * Stores the file in every provider. With S3 asset storage on, this happens under the key's lock,
     * from the last provider to the first, so durable storage holds the file before the local copy
     * changes. If a provider fails after a later one accepted the file, the now-stale copies of it
     * and the providers before it are deleted, so the next read restores the new file, and the
     * failure is rethrown.
     *
     * @param groupName the group to store into
     * @param path      the file's key
     * @param file      the file to store
     * @param extraMeta extra metadata for providers that keep it
     * @return the result of the first provider in the chain
     * @throws DotDataException if a provider fails, or the key's lock cannot be taken in time
     */
    @Override
    public Object pushFile(final String groupName, final String path, final File file,
                           final Map<String, Serializable> extraMeta) throws DotDataException {

        if (AssetStorageFeature.isEnabled()) {
            final Lock lock = lockKey(groupName, path);
            try {
                Object result = null;
                // Publish durably before exposing the new local cache contents.
                for (int i = storagePersistenceAPIList.size() - 1; i >= 0; i--) {
                    try {
                        result = storagePersistenceAPIList.get(i).pushFile(groupName, path, file, extraMeta);
                    } catch (DotDataException | RuntimeException failure) {
                        dropCopiesOlderThanAcceptedWrite(groupName, path, i);
                        throw failure;
                    }
                }
                cache.remove(groupName, path);
                return result;
            } finally {
                lock.unlock();
            }
        }

        Object object = null;
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // since this is a composite and in theory the object should be the same, we just return the first one
            final Object localObject = storage.pushFile(groupName, path, file, extraMeta);
            object = null == object? localObject: object;
        }

        // if the file was previously not found, we remove it from the cache since it is now a valid path
        cache.remove(groupName, path);

        return object;
    }

    /**
     * Stores the object in every provider. With S3 asset storage on, this behaves like
     * {@link #pushFile(String, String, File, Map)}: under the key's lock, durable providers first,
     * and stale copies are deleted when a provider fails after a later one accepted the object.
     *
     * @param groupName      the group to store into
     * @param path           the object's key
     * @param writerDelegate serializes the object
     * @param objectIn       the object to store
     * @param extraMeta      extra metadata for providers that keep it
     * @return the result of the first provider in the chain
     * @throws DotDataException if a provider fails, or the key's lock cannot be taken in time
     */
    @Override
    public Object pushObject(final String groupName, final String path,
                             final ObjectWriterDelegate writerDelegate, final Serializable objectIn,
                             final Map<String, Serializable> extraMeta) throws DotDataException {
        if (AssetStorageFeature.isEnabled()) {
            final Lock lock = lockKey(groupName, path);
            try {
                Object result = null;
                for (int i = storagePersistenceAPIList.size() - 1; i >= 0; i--) {
                    try {
                        result = storagePersistenceAPIList.get(i).pushObject(groupName, path, writerDelegate, objectIn, extraMeta);
                    } catch (DotDataException | RuntimeException failure) {
                        dropCopiesOlderThanAcceptedWrite(groupName, path, i);
                        throw failure;
                    }
                }
                cache.remove(groupName, path);
                return result;
            } finally {
                lock.unlock();
            }
        }


        Object objectToReturn = null;
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // since this is a composite and in theory the object should be the same, we just return the first one
            final Object localObject = storage.pushObject(groupName, path, writerDelegate, objectIn, extraMeta);
            objectToReturn = null == objectToReturn? localObject: objectToReturn;
        }

        // if the object was previously not found, we remove it from the cache since it is now a valid path
        cache.remove(groupName, path);

        return objectToReturn;
    }

    @Override
    public Future<Object> pushFileAsync(final String groupName, final String path, final File file, final Map<String, Serializable> extraMeta) {
        if (AssetStorageFeature.isEnabled()) {
            return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(() -> this.pushFile(groupName, path, file, extraMeta));
        }


        final List<Future<Object>> futures = new ArrayList<>();
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // since this is a composite and in theory the object should be the same, we just return the first one
            final Future<Object> future = storage.pushFileAsync(groupName, path, file, extraMeta);
            futures.add(future);
        }

        if (futures.isEmpty()) {

            throw new DotRuntimeException("No storage persistence api found");
        }

        // if the object was previously not found, we remove it from the cache since it is now a valid path
        cache.remove(groupName, path);

        return futures.get(0); // well we return the first one at least
    }

    @Override
    public Future<Object> pushObjectAsync(final String bucketName, final String path,
                                          final ObjectWriterDelegate writerDelegate,
                                          final Serializable object, final Map<String, Serializable> extraMeta) {
        if (AssetStorageFeature.isEnabled()) {
            return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(() -> this.pushObject(bucketName, path, writerDelegate, object, extraMeta));
        }


        final List<Future<Object>> futures = new ArrayList<>();
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // since this is a composite and in theory the object should be the same, we just return the first one
            final Future<Object> future = storage.pushObjectAsync(bucketName, path, writerDelegate, object, extraMeta);
            futures.add(future);
        }

        if (futures.isEmpty()) {

            throw new DotRuntimeException("No storage persistence api found");
        }

        // if the object was previously not found, we remove it from the cache since it is now a valid path
        cache.remove(bucketName, path);

        return futures.get(0); // well we return the first one at least
    }

    /**
     * Returns the file from the first provider that has it. With S3 asset storage on, this happens
     * under the key's lock, providers before the one that had it are refilled with the file, and
     * read failures propagate instead of being reported as absence.
     *
     * @param groupName the group to read from
     * @param path      the file's key
     * @return the file, or {@code null} if no provider has it
     * @throws DotDataException if a provider fails, or the key's lock cannot be taken in time
     */
    @Override
    public File pullFile(final String groupName, final String path) throws DotDataException {

        if (AssetStorageFeature.isEnabled()) {
            final Lock lock = lockKey(groupName, path);
            try {
                final List<StoragePersistenceAPI> missing = new ArrayList<>();
                for (StoragePersistenceAPI storage : storagePersistenceAPIList) {
                    final File file = storage.pullFile(groupName, path);
                    if (file == null) {
                        missing.add(storage);
                        continue;
                    }
                    if (missing.isEmpty()) {
                        return file;
                    }
                    try {
                        for (StoragePersistenceAPI cacheStorage : missing) {
                            if (!cacheStorage.existsGroup(groupName)) {
                                cacheStorage.createGroup(groupName);
                            }
                            cacheStorage.pushFile(groupName, path, file, Map.of());
                        }
                        return missing.get(0).pullFile(groupName, path);
                    } finally {
                        storage.releaseRetrievedFile(file);
                    }
                }
                // Absence is not cached: another node can upload this key at any time.
                return null;
            } finally {
                lock.unlock();
            }
        }

        if (this.cache.is404(groupName, path)) {

            return null;
        }

        File fileToReturn = null;

        final List<StoragePersistenceAPI> missStorageList = new ArrayList<>();
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            final File localFile = Try.of(()->storage.pullFile(groupName, path)).getOrNull();
            if (null != localFile) {

                fileToReturn = localFile;
                break;
            }

            missStorageList.add(storage); // we will populate this list with the storages that does not have the file, so will be populated async at the end
        }

        // if the file is not found in all storages, we will populate the 404 cache
        if (fileToReturn == null && missStorageList.size() == this.storagePersistenceAPIList.size()) {

            this.cache.put404(groupName, path);
        } else {

            populateMissStorageChain(groupName, path, missStorageList, fileToReturn);
        }

        return fileToReturn;
    }

    private void populateMissStorageChain(final String groupName, final String path,
                                          final List<StoragePersistenceAPI> missStorageList, final File file) {

        if (!missStorageList.isEmpty() && Objects.nonNull(file)) {

            for(final StoragePersistenceAPI storage : missStorageList) {

                storage.pushFileAsync(groupName, path, file, null);
            }
        }
    }

    /**
     * Reads an object from the first provider that has it. With S3 asset storage on, providers
     * earlier in the chain that missed it are refilled with the value, under the same per-key lock
     * that writes and deletes take, so a restore cannot overwrite a newer write or bring back a
     * deleted object. A provider whose copy cannot be deserialized is refilled the same way when a
     * later provider holds a readable copy; if none does, the read fails rather than reporting the
     * object as absent. Read failures propagate instead of being reported as absence.
     *
     * @param groupName      the group to read from
     * @param path           the object's path
     * @param readerDelegate deserializes the stored value
     * @return the object, or {@code null} if no provider has it
     */
    @Override
    public Object pullObject(final String groupName,
                             final String path,
                             final ObjectReaderDelegate readerDelegate) {

        if (AssetStorageFeature.isEnabled()) {
            final List<StoragePersistenceAPI> missing = new ArrayList<>();
            UnreadableStoredObjectException unreadable = null;
            final Lock lock;
            try {
                lock = lockKey(groupName, path);
            } catch (DotDataException e) {
                throw new DotRuntimeException("Unable to retrieve stored object " + path, e);
            }
            try {
                for (StoragePersistenceAPI storage : storagePersistenceAPIList) {
                    Object object;
                    try {
                        object = storage.pullObject(groupName, path, readerDelegate);
                    } catch (final UnreadableStoredObjectException e) {
                        unreadable = e;
                        object = null;
                    }
                    if (object == null) {
                        missing.add(storage);
                    } else {
                        if (unreadable != null) {
                            Logger.warn(this, "Replacing an unreadable copy of (" + groupName + '|' + path
                                    + ") from a later storage provider: " + unreadable.getMessage());
                        }
                        if (object instanceof Serializable) {
                            for (StoragePersistenceAPI cacheStorage : missing) {
                                if (!cacheStorage.existsGroup(groupName)) {
                                    cacheStorage.createGroup(groupName);
                                }
                                cacheStorage.pushObject(groupName, path, defaultWriterDelegate,
                                        (Serializable) object, Map.of());
                            }
                        }
                        return object;
                    }
                }
                if (unreadable != null) {
                    throw unreadable;
                }
                return null;
            } catch (DotDataException e) {
                throw new DotRuntimeException("Unable to retrieve stored object " + path, e);
            } finally {
                lock.unlock();
            }
        }

        if (this.cache.is404(groupName, path)) {

            return null;
        }

        Object objectToReturn = null;

        final List<StoragePersistenceAPI> missStorageList = new ArrayList<>();
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            final Object localObject = Try.of(()->storage.pullObject(groupName, path, readerDelegate)).getOrNull();
            if (null != localObject) {

                objectToReturn = localObject;
                break;
            }

            missStorageList.add(storage); // we will populate this list with the storages that does not have the object, so will be populated async at the end
        }

        // if the object is not found in all storages, we will populate the 404 cache
        if (objectToReturn == null && missStorageList.size() == this.storagePersistenceAPIList.size()) {

            this.cache.put404(groupName, path);
        } else {
            populateMissStorageChain(groupName, path, missStorageList, objectToReturn);
        }

        return objectToReturn;
    }

    private void populateMissStorageChain(final String groupName, final String path,
                                          final List<StoragePersistenceAPI> missStorageList, final Object objectToReturn) {

        if (!missStorageList.isEmpty() && Objects.nonNull(objectToReturn) && objectToReturn instanceof Serializable) {

            for(final StoragePersistenceAPI storage : missStorageList) {

                storage.pushObjectAsync(groupName, path, defaultWriterDelegate, (Serializable) objectToReturn, null);
            }
        }
    }

    @Override
    public Future<File> pullFileAsync(final String groupName, final String path) {
        if (AssetStorageFeature.isEnabled()) {
            return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(() -> this.pullFile(groupName, path));
        }


        if (this.cache.is404(groupName, path)) {

            return null;
        }

        final List<Future<File>> futures = new ArrayList<>();
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            final Future<File> localFileFuture = storage.pullFileAsync(groupName, path);
            futures.add(localFileFuture);
        }

        // since we do not want to wait for all the futures to be completed, we do not which layer does not have the file, so we fire a new thread to call the sync method
        DotConcurrentFactory.getInstance().getSubmitter(SUBMITTER_NAME).submit(()-> this.pullFile(groupName, path));
        return DotConcurrentFactory.getInstance().toCompletableAnyFuture(futures);
    }

    @Override
    public Future<Object> pullObjectAsync(final String groupName, final String path,
                                          final ObjectReaderDelegate readerDelegate) {
        if (AssetStorageFeature.isEnabled()) {
            return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(() -> this.pullObject(groupName, path, readerDelegate));
        }


        if (this.cache.is404(groupName, path)) {

            return null;
        }

        final List<Future<Object>> futures = new ArrayList<>();
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            final Future<Object> localFileFuture = storage.pullObjectAsync(groupName, path, readerDelegate);
            futures.add(localFileFuture);
        }

        // since we do not want to wait for all the futures to be completed, we do not which layer does not have the file, so we fire a new thread to call the sync method
        DotConcurrentFactory.getInstance().getSubmitter(SUBMITTER_NAME).submit(()-> this.pullObject(groupName, path, readerDelegate));
        return DotConcurrentFactory.getInstance().toCompletableAnyFuture(futures);
    }

}
