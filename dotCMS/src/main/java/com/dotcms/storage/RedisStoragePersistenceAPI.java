package com.dotcms.storage;

import com.dotcms.cache.lettuce.RedisCache;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.EnterpriseFeature;
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
 * Provides a Metadata Provider implementation that uses Redis to persist the metadata files. This
 * implementation uses a remote cache, and has a filter to avoid to store objects with certain
 * size.
 * <p>This provider can ONLY be used with an Enterprise License.</p>
 *
 * @author jsanca
 */
public class RedisStoragePersistenceAPI implements StoragePersistenceAPI {

    private final long maxObjectSize;
    private final RedisCache redisCache = new RedisCache();
    private final Set<String> groups = ConcurrentHashMap.newKeySet();
    private static final String INVALID_LICENSE = "The Redis Metadata Provider is an Enterprise only feature";

    public RedisStoragePersistenceAPI() {
        this(Config.getLongProperty("REDIS_STORAGE_MAX_OBJECT_SIZE", FileUtil.KILO_BYTE * 100));
    }

    public RedisStoragePersistenceAPI(final long maxObjectSize) {
        this.maxObjectSize = maxObjectSize;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean existsGroup(final String groupName) {
        // If the group is NOT in the local cache, we try with the remote
        if (!this.groups.contains(groupName)) {
            groups.addAll(this.redisCache.getGroups());
        }

        return this.groups.contains(groupName);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean existsObject(final String groupName, final String objectPath) throws DotDataException {
        return this.existsGroup(groupName) && null != this.redisCache.get(groupName, objectPath);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean createGroup(final String groupName) throws DotDataException {
        return this.groups.add(groupName);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions) throws DotDataException {
        return createGroup(groupName);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public int deleteGroup(final String groupName) throws DotDataException {
        this.redisCache.remove(groupName);
        this.groups.remove(groupName);
        return 1;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean deleteObjectAndReferences(final String groupName, final String path) {
        if (existsGroup(groupName)) {
            this.redisCache.remove(groupName, path);
            return true;
        }
        return false;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean deleteObjectReference(final String groupName, final String path) {
        return this.deleteObjectAndReferences(groupName, path);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public List<String> listGroups() {
        // Before sending the list, we need to update the local list with the remote data
        groups.addAll(this.redisCache.getGroups());
        return List.copyOf(this.groups);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Object pushFile(final String groupName,
                           final String path, final File file,
                           final Map<String, Serializable> extraMeta) throws DotDataException {
        if (this.existsGroup(groupName) && isSizeAllowed(file)) {
            try {
                this.redisCache.put(groupName, path, Files.toByteArray(file));
                return true;
            } catch (final Exception e) {
                Logger.error(this, String.format("Failed to push File '%s' to group '%s': %s",
                        path, groupName, ExceptionUtil.getErrorMessage(e)), e);
            }
        }
        return false;
    }

    private boolean isSizeAllowed(final File file) {
        try {
            // if the user sets -1, means do not want any limitation
            return this.maxObjectSize == -1 || FileUtils.sizeOf(file) < this.maxObjectSize;
        } catch (final Exception e) {
            return false;
        }
    }

    private boolean isSizeAllowed(final Serializable object,
                                  final ObjectWriterDelegate writerDelegate) {
        // There is also an implementation based on instrumentation, but it seems to be very complex
        // to achieve, maybe with byte buddy.
        if (this.maxObjectSize == -1) { // if the user sets -1, means do not want any limitation
            return true;
        }
        try {
            if (object instanceof CharSequence) {
                return ((CharSequence) object).length() < this.maxObjectSize;
            }

            if (object instanceof File) {
                return isSizeAllowed((File) object);
            }

            // todo: we might need other cases for binaries or other types
            // We convert to JSON first. It's not the best, but it's the only way to know the size
            // of the object. There may be an instrumentation mechanism, but I do not know if it is
            // worth it.
            if (Objects.nonNull(object) && Objects.nonNull(writerDelegate)) {
                final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                writerDelegate.write(stream, object);
                return stream.size() < this.maxObjectSize;
            }
        } catch (final Exception e) {
            Logger.error(this, String.format("Failed to determine size for object '%s': %s",
                    object, ExceptionUtil.getErrorMessage(e)), e);
            return false;
        }
        return false;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Object pushObject(final String groupName, final String path,
                             final ObjectWriterDelegate writerDelegate, final Serializable object,
                             final Map<String, Serializable> extraMeta) {
        if (this.existsGroup(groupName) && isSizeAllowed(object, writerDelegate)) {
            // We do not need the writerDelegate since Redis support binary serializable objects
            this.redisCache.put(groupName, path, object);
            return true;
        }
        return false;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Future<Object> pushFileAsync(final String groupName, final String path,
                                        final File file,
                                        final Map<String, Serializable> extraMeta) {
        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushFile(groupName, path, file, extraMeta)
        );
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Future<Object> pushObjectAsync(final String bucketName, final String path,
                                          final ObjectWriterDelegate writerDelegate,
                                          final Serializable object, final Map<String,
            Serializable> extraMeta) {
        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushObject(bucketName, path, writerDelegate, object, extraMeta)
        );
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public File pullFile(final String groupName, final String path) {
        final MutableObject<File> file = new MutableObject<>(null);

        if (this.existsGroup(groupName)) {
            try {
                final Object rawObject = this.redisCache.get(groupName, path);
                if (null != rawObject) {
                    if (rawObject instanceof byte[]) {
                        // todo: we have to test this to see if it works fine or we need to store
                        //  a metadata
                        file.setValue(File.createTempFile("temp", "temp"));
                        FileUtils.writeByteArrayToFile(file.getValue(), (byte[]) rawObject);
                    } else {
                        throw new DotDataException("The storage group: " + groupName + ", path: " + path + " is not a file");
                    }
                } else {
                    throw new DoesNotExistException(
                            "The storage group: " + groupName + ", path: " + path + " does not " +
                                    "exist");
                }
            } catch (final Exception e) {
                Logger.error(this, String.format("Failed to pull File '%s' from group '%s': %s",
                        path, groupName, ExceptionUtil.getErrorMessage(e)), e);
            }
        }

        return file.getValue();
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Object pullObject(final String groupName, final String path,
                             final ObjectReaderDelegate readerDelegate) {
        Object object = null;

        if (this.existsGroup(groupName)) {
            final Object rawObject = this.redisCache.get(groupName, path);
            if (null != rawObject) {

                object = rawObject;
            } else {
                throw new DoesNotExistException(
                        "The storage group: " + groupName + ", path: " + path + " does not exist");
            }
        }

        return object;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Future<File> pullFileAsync(final String groupName, final String path) {
        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pullFile(groupName, path)
        );
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Future<Object> pullObjectAsync(final String groupName, final String path,
                                          final ObjectReaderDelegate readerDelegate) {
        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pullObject(groupName, path, readerDelegate)
        );
    }

    @Override
    public String toString() {
        return "RedisStoragePersistenceAPI{" +
                "groups=" + groups +
                '}';
    }

}
