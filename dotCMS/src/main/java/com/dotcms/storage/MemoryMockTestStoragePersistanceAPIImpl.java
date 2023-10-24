package com.dotcms.storage;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.exception.DotDataException;
import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * This implementation is only for testing, it isn't scalable and is not recommended to use in
 * production environments. Eventually, the Redis implementation would be a good replacement for
 * this one.
 *
 * @author jsanca
 */
@VisibleForTesting
public class MemoryMockTestStoragePersistanceAPIImpl implements StoragePersistenceAPI {

    private final Map<String, Map<String, Object>> storageMapByGroup = new ConcurrentHashMap<>();

    @Override
    public boolean existsGroup(final String groupName) {
        return storageMapByGroup.containsKey(groupName);
    }

    @Override
    public boolean existsObject(final String groupName, final String objectPath) {
        return existsGroup(groupName) && storageMapByGroup.get(groupName).containsKey(objectPath);
    }

    @Override
    public boolean createGroup(final String groupName) throws DotDataException {
        storageMapByGroup.computeIfAbsent(groupName, key -> new HashMap<>());
        return existsGroup(groupName);
    }

    @Override
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions) throws DotDataException {
        return createGroup(groupName);
    }

    @Override
    public int deleteGroup(final String groupName) {
        storageMapByGroup.remove(groupName);
        return !existsGroup(groupName) ? 1 : 0;
    }

    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) {
        return existsGroup(groupName) && null != storageMapByGroup.get(groupName).remove(path);
    }

    @Override
    public boolean deleteObjectReference(final String groupName, final String path) {
        return existsGroup(groupName) && null != storageMapByGroup.get(groupName).remove(path);
    }

    @Override
    public List<String> listGroups() throws DotDataException {
        return List.copyOf(storageMapByGroup.keySet());
    }

    @Override
    public Object pushFile(final String groupName, final String path,
                           final File file, final Map<String, Serializable> extraMeta) {

        if (this.existsGroup(groupName)) {

            storageMapByGroup.get(groupName).put(path, file);
            return true;
        }
        return false;
    }

    @Override
    public Object pushObject(final String groupName, final String path,
                             final ObjectWriterDelegate writerDelegate,
                             final Serializable object,
                             final Map<String, Serializable> extraMeta) {

        if (this.existsGroup(groupName)) {

            storageMapByGroup.get(groupName).put(path, object);
            return true;
        }
        return false;
    }

    @Override
    public Future<Object> pushFileAsync(final String groupName,
                                        final String path,
                                        final File file,
                                        final Map<String, Serializable> extraMeta) {
        return DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
            this.pushFile(groupName, path, file, extraMeta);
            return true;
        });
    }

    @Override
    public Future<Object> pushObjectAsync(String bucketName, String path, ObjectWriterDelegate writerDelegate, Serializable object, Map<String, Serializable> extraMeta) {
        return DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
            this.pushObject(bucketName, path, writerDelegate,object, extraMeta);
            return true;
        });
    }

    @Override
    public File pullFile(final String groupName, final String path) {
        if (this.existsGroup(groupName)) {
            return storageMapByGroup.get(groupName).get(path) instanceof File ?
                    (File) storageMapByGroup.get(groupName).get(path) : null;
        }
        return null;
    }

    @Override
    public Object pullObject(final String groupName, final String path, final ObjectReaderDelegate readerDelegate) {
        if (this.existsGroup(groupName)) {
            return storageMapByGroup.get(groupName).getOrDefault(path, null);
        }
        return null;
    }

    @Override
    public Future<File> pullFileAsync(final String groupName, final String path) {
        return DotConcurrentFactory.getInstance().getSubmitter().submit(() -> this.pullFile(groupName, path));
    }

    @Override
    public Future<Object> pullObjectAsync(final String groupName, final String path,
                                          final ObjectReaderDelegate readerDelegate) {
        return DotConcurrentFactory.getInstance().getSubmitter().submit(() -> this.pullObject(groupName, path, readerDelegate));
    }

}
