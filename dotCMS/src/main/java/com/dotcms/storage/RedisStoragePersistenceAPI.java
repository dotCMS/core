package com.dotcms.storage;

import com.dotcms.cache.lettuce.RedisCache;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.io.Files;
import com.liferay.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Implements a storage based on Redis
 * This implementation is on remote cache and has a filter to avoid to store objects with
 * certain size.
 * @author jsanca
 */
public class RedisStoragePersistenceAPI implements StoragePersistenceAPI {

    private final long maxObjectSize;
    private final RedisCache redisCache = new RedisCache();
    private final Set<String> groups = ConcurrentHashMap.newKeySet();

    public RedisStoragePersistenceAPI() {
        this(Config.getLongProperty("REDIS_STORAGE_MAX_OBJECT_SIZE", FileUtil.KILO_BYTE*100));
    }

    public RedisStoragePersistenceAPI(final long maxObjectSize) {
        this.maxObjectSize = maxObjectSize;
    }

    @Override
    public boolean existsGroup(final String groupName) throws DotDataException {

        // if the group is not on the local, we try with the remote
        if (!this.groups.contains(groupName)) {
            groups.addAll(this.redisCache.getGroups()) ;
        }

        return this.groups.contains(groupName);
    }

    @Override
    public boolean existsObject(final String groupName, final String objectPath) throws DotDataException {

        return this.existsGroup(groupName)? null != this.redisCache.get(groupName, objectPath): false;
    }

    @Override
    public boolean createGroup(final String groupName) throws DotDataException {

        return this.groups.add(groupName);
    }

    @Override
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions) throws DotDataException {

        return createGroup(groupName);
    }

    @Override
    public int deleteGroup(final String groupName) throws DotDataException {

        this.redisCache.remove(groupName);
        this.groups.remove(groupName);
        return 1;
    }

    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {

        if (existsGroup(groupName)) {
            this.redisCache.remove(groupName, path);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteObjectReference(final String groupName, final String path) throws DotDataException {

        return this.deleteObjectReference(groupName, path);
    }

    @Override
    public List<String> listGroups() throws DotDataException {

        // before sending the list, we update the local list with the remote ones.
        groups.addAll(this.redisCache.getGroups()) ;
        return List.copyOf(this.groups);
    }

    @Override
    public Object pushFile(final String groupName,
                           final String path, final File file,
                           final Map<String, Serializable> extraMeta) throws DotDataException {

        if (this.existsGroup(groupName) && isSizeAllowed(file)) {

            try {
                this.redisCache.put(groupName, path, Files.toByteArray(file));
                return true;
            }catch (final Exception e) {
                Logger.error(this, e.getMessage());
            }
        }
        return false;
    }

    private boolean isSizeAllowed(final File file) {
        try {

            return this.maxObjectSize == -1? true: // if the user sets -1, means do not want any limitation
                    FileUtils.sizeOf(file) < this.maxObjectSize;
        } catch (final Exception e) {
            return false;
        }
    }

    private boolean isSizeAllowed(final Serializable object, final ObjectWriterDelegate writerDelegate) {

        /*
         * There is also an implementation based on instrumentation, but it seems so much complex to achieve, may be with byte buddy.
         */
        if(this.maxObjectSize == -1) { // if the user sets -1, means do not want any limitation
            return true;
        }
        try {

            if (object instanceof CharSequence) {
                return CharSequence.class.cast(object).length() < this.maxObjectSize;
            }

            if(object instanceof File) {
                return isSizeAllowed(File.class.cast(object));
            }

            // todo: we might need other cases for binaries or others
            // we convert to json, is not the best, but is the only way to know the size of the object
            // there may be instrumentation mechanism but I do not know if it is worth it.
            if (Objects.nonNull(object) && Objects.nonNull(writerDelegate)) {

                final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                writerDelegate.write(stream, object);
                return stream.size() < this.maxObjectSize;
            }
        } catch (final Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public Object pushObject(final String groupName, final String path,
                             final ObjectWriterDelegate writerDelegate, final Serializable object,
                             final Map<String, Serializable> extraMeta) throws DotDataException {

        if (this.existsGroup(groupName) && isSizeAllowed(object, writerDelegate)) {

            this.redisCache.put(groupName, path, object); // we do not need the writerDelegate, since redis support binary objects
            return true;
        }
        return false;
    }

    @Override
    public Future<Object> pushFileAsync(final String groupName, final String path,
                                        final File file, final Map<String, Serializable> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushFile(groupName, path, file, extraMeta)
        );
    }

    @Override
    public Future<Object> pushObjectAsync(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate,
                                          final Serializable object, final Map<String, Serializable> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushObject(bucketName, path, writerDelegate, object, extraMeta)
        );
    }

    @Override
    public File pullFile(final String groupName, final String path) throws DotDataException {

        final MutableObject<File> file = new MutableObject<>(null);

        if (this.existsGroup(groupName)) {

            try {

                final Object rawObject = this.redisCache.get(groupName, path);
                if (null != rawObject) {

                    if (rawObject instanceof byte[]) {
                        // todo: we have to test this to see if works fine or we need to storage a metadata
                        file.setValue(File.createTempFile("temp", "temp"));
                        FileUtils.writeByteArrayToFile(file.getValue(), byte[].class.cast(rawObject));
                    } else {
                        throw new DotDataException("The storage, group: " + groupName + ", path: " + path
                                + " is not a file");
                    }
                } else {
                    throw new DoesNotExistException(
                            "The storage, group: " + groupName + ", path: " + path
                                    + " does not exists");
                }

            } catch (final Exception e) {
                Logger.error(this, e.getMessage());
            }
        }

        return file.getValue();
    }

    @Override
    public Object pullObject(final String groupName, final String path,
                             final ObjectReaderDelegate readerDelegate) throws DotDataException {

        Object object = null;

        if (this.existsGroup(groupName)) {
            final Object rawObject = this.redisCache.get(groupName, path);
            if (null != rawObject) {

                object = rawObject;
            } else {
                throw new DoesNotExistException(
                        "The storage, group: " + groupName + ", path: " + path
                                + " does not exists");
            }
        }

        return object;
    }

    @Override
    public Future<File> pullFileAsync(final String groupName, final String path) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pullFile(groupName, path)
        );
    }

    @Override
    public Future<Object> pullObjectAsync(final String groupName, final String path,
                                          final ObjectReaderDelegate readerDelegate) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pullObject(groupName, path, readerDelegate)
        );
    }
}
