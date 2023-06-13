package com.dotcms.storage;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
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
 * This API implements a chainable pattern, which encapsulates a list of StoragePersistenceAPI
 * This allows to implement chainable storages, for example:
 * - The first layer could be Redis Mem, second layer could be NFS, third layer could be DB and finally S3.
 * - Also, incorporates a 404 cache layer which is in front of all storage; the goal of this cache is to avoid to look for over all layers
 * when the path was previously searched on all layers and it wasn't found.
 * @author jsanca
 */
public class ChainableStoragePersistenceAPI implements StoragePersistenceAPI {

    private final List<StoragePersistenceAPI> storagePersistenceAPIList;
    private final ObjectWriterDelegate defaultWriterDelegate;

    private final static String SUBMITTER_NAME = Config.getStringProperty("COMPOSITE_STORAGE_SUBMITTER_NAME", "SubmitterCompositeStoragePersistenceAPI");

    public ChainableStoragePersistenceAPI(final List<StoragePersistenceAPI> storagePersistenceAPIList) {
        this(new JsonWriterDelegate(), storagePersistenceAPIList);
    }

    public ChainableStoragePersistenceAPI(final ObjectWriterDelegate defaultWriterDelegate,
                                          final List<StoragePersistenceAPI> storagePersistenceAPIList) {

        this.defaultWriterDelegate = defaultWriterDelegate;
        this.storagePersistenceAPIList = storagePersistenceAPIList;
    }

    @Override
    public boolean existsGroup(final String groupName) throws DotDataException {

        return this.storagePersistenceAPIList.stream().anyMatch(storage -> Try.of(()->storage.existsGroup(groupName)).getOrElse(false));
    }

    @Override
    public boolean existsObject(final String groupName, final String objectPath) throws DotDataException {

        return this.storagePersistenceAPIList.stream().anyMatch(storage -> Try.of(()->storage.existsObject(groupName, objectPath)).getOrElse(false));
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

        return deleteCount;
    }

    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {

        boolean deleted = !this.storagePersistenceAPIList.isEmpty();

        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // it would be great to have atomic here, so if one storage fails, all fail, but it is ok by now
            deleted &= storage.deleteObjectAndReferences(groupName, path);
        }

        return deleted;
    }

    @Override
    public boolean deleteObjectReference(String groupName, String path) throws DotDataException {

        boolean deleted = !this.storagePersistenceAPIList.isEmpty();

        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            // it would be great to have atomic here, so if one storage fails, all fail, but it is ok by now
            deleted &= storage.deleteObjectReference(groupName, path);
        }

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

        return futures.get(0); // well we return the first one at least
    }

    @Override
    public File pullFile(String groupName, String path) throws DotDataException {

        // todo: here we will look for on each storage the File, if an upper layer does not have the file, we will look for it in the next one
        // at the end of the process for each storage without the file, an async call will be execute in order to populate the storage with that particular file
        File fileToReturn = null;

        final List<StoragePersistenceAPI> missStorageList = new ArrayList<>();
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {

            final File localFile = Try.of(()->storage.pullFile(groupName, path)).getOrNull();
            if (null != localFile) {

                fileToReturn = localFile;
                break;
            } else {

                missStorageList.add(storage); // we will populate this list with the storages that does not have the file, so will be populated async at the end
            }
        }

        populateMissStorageChain(groupName, path, missStorageList, fileToReturn);

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
                             final ObjectReaderDelegate readerDelegate) throws DotDataException {
        // todo: here we will look for on each storage the Object, if an upper layer does not have the object, we will look for it in the next one
        // at the end of the process for each storage without the object, an async call will be execute in order to populate the storage with that particular object
        Object objectToReturn = null;

        final List<StoragePersistenceAPI> missStorageList = new ArrayList<>();
        for(final StoragePersistenceAPI storage : this.storagePersistenceAPIList) {
            // todo: handle the 404 MemCache404StorageApi
            final Object localObject = Try.of(()->storage.pullObject(groupName, path, readerDelegate)).getOrNull();
            if (null != localObject) {

                objectToReturn = localObject;
                break;
            } else {

                missStorageList.add(storage); // we will populate this list with the storages that does not have the object, so will be populated async at the end
            }
        }

        populateMissStorageChain(groupName, path, missStorageList, objectToReturn);

        return objectToReturn;
    }

    private void populateMissStorageChain(final String groupName, final String path,
                                          final List<StoragePersistenceAPI> missStorageList, final Object objectToReturn) {

        if (!missStorageList.isEmpty() && Objects.nonNull(objectToReturn) && objectToReturn instanceof Serializable) {

            for(final StoragePersistenceAPI storage : missStorageList) {

                storage.pushObjectAsync(groupName, path, defaultWriterDelegate, Serializable.class.cast(objectToReturn), null);
            }
        }
    }

    @Override
    public Future<File> pullFileAsync(final String groupName, final String path) {

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
