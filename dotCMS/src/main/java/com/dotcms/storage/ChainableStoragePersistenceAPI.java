package com.dotcms.storage;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;

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

    @Override
    public boolean existsGroup(final String groupName) {

        return this.storagePersistenceAPIList.stream().allMatch(storage -> Try.of(()->storage.existsGroup(groupName)).getOrElse(false));
    }

    @Override
    public boolean existsObject(final String groupName, final String objectPath) {
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

    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {

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
    public Object pushFile(final String groupName, final String path, final File file,
                           final Map<String, Serializable> extraMeta) throws DotDataException {

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

    @Override
    public Object pushObject(final String groupName, final String path,
                             final ObjectWriterDelegate writerDelegate, final Serializable objectIn,
                             final Map<String, Serializable> extraMeta) throws DotDataException {

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

    @Override
    public File pullFile(final String groupName, final String path) {

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

    @Override
    public Object pullObject(final String groupName,
                             final String path,
                             final ObjectReaderDelegate readerDelegate) {

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
