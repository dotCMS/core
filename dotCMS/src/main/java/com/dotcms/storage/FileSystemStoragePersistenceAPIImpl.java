package com.dotcms.storage;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.repackage.com.bradmcevoy.common.Path;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.FileUtil;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * Represents a Storage on the file system The groups here are folder previously registered, you can
 * subscribe more by using {@link #addGroupMapping(String, File)}
 * By default the API loads up and maps a root folder. Which can be override by a property.
 * Any new group created will result in a new folder under that root folder.
 * @author jsanca
 */
public class FileSystemStoragePersistenceAPIImpl implements StoragePersistenceAPI {

    private static final String DEFAULT_ROOT = "root";
    private static final String THE_BUCKET_NAME_S_DOES_NOT_HAVE_ANY_FILE_MAPPED = "The bucketName: `%s`, does not have any files mapped";
    private static final String STORAGE_POOL = "StoragePool";

    private final Map<String, File> groups = new ConcurrentHashMap<>();

    /**
     * default constructor
     */
    FileSystemStoragePersistenceAPIImpl() {
        final String rootGroupKey = getRootGroupKey();
        final File rootFolder = getRootFolder();
        groups.put(rootGroupKey, rootFolder);
        Logger.debug(FileSystemStoragePersistenceAPIImpl.class, () -> String
                .format("Default group key is `%s` currently mapped to folder `%s` ", rootGroupKey,
                        rootFolder));
    }

    /**
     * Adds a mapping between a bucket name and a file
     *
     * @param groupName {@link String} bucket name
     * @param folder {@link File}
     */
    void addGroupMapping(final String groupName, final File folder) {

        if (!folder.isDirectory() || !folder.exists() || !folder.canWrite()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid attempt of mapping an non existing or writable folder. Argument`%s` must be a valid. ",
                    folder));
        }
        groups.put(groupName.toLowerCase(), folder);
        Logger.debug(FileSystemStoragePersistenceAPIImpl.class, () -> String.format("Registering New Group with key is `%s` mapped to folder `%s` ",groupName, folder));
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} group name
     * @return
     */
    @Override
    public boolean existsGroup(final String groupName) throws DotDataException{
        final String groupNameLC = groupName.toLowerCase();
        return groups.containsKey(groupNameLC) && this.groups.get(groupNameLC).exists();
    }

    /**
     * {@inheritDoc}
     * @param groupName  {@link String}
     * @param path {@link String}
     * @return
     */
    @Override
    public boolean existsObject(final String groupName, final String path) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();

        if(!existsGroup(groupNameLC)){
            return false;
        }
        final File groupDir = groups.get(groupNameLC);
        try {
           return Paths.get(groupDir.getCanonicalPath(), path.toLowerCase()).toFile().exists();
        }catch(IOException e){
            throw new DotDataException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} group name
     * @return
     */
    @Override
    public boolean createGroup(final String groupName) throws DotDataException {
        return this.createGroup(groupName, ImmutableMap.of());
    }

    /**
     * {@inheritDoc}
     * @param groupName    {@link String} group name
     * @param extraOptions {@link Map} depending on the implementation it might need extra options or not.
     * @return
     */
    @Override
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        final File rootGroup = groups.get(getRootGroupKey());
        final File destBucketFile = new File(rootGroup, groupNameLC);
        final boolean mkdirs = destBucketFile.mkdirs();
        if(mkdirs) {
           groups.put(groupNameLC, destBucketFile);
        }
        return mkdirs;
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} group name
     * @return
     */
    @Override
    public int deleteGroup(final String groupName) throws DotDataException {
        final File rootGroup = groups.get(getRootGroupKey());
        final File destBucketFile = new File(rootGroup, groupName.toLowerCase());
        if (!rootGroup.equals(destBucketFile)) {
            final int count = countFiles(destBucketFile);
            FileUtil.deltree(destBucketFile, true);
            return count;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} group name
     * @param path   {   @link String} object path
     * @return
     */
    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {
        return new File(groups.get(groupName.toLowerCase()), path.toLowerCase()).delete();
    }

    @Override
    public boolean deleteObjectReference(String groupName, String path) throws DotDataException {
        return deleteObjectAndReferences(groupName, path);
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public List<String> listGroups() throws DotDataException {

        return new ImmutableList.Builder<String>().addAll(groups.keySet()).build();
    }

    /**
     * {@inheritDoc}
     * @param groupName  {@link String} the group to upload
     * @param path       {@link String} path to upload the file
     * @param file       {@link File}   the actual file
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return
     */
    @Override
    public Object pushFile(final String groupName,
            final String path,
            final File file,
            final Map<String, Serializable> extraMeta) throws DotDataException{

        if (!this.existsGroup(groupName)) {

            throw new IllegalArgumentException(String.format(
                    THE_BUCKET_NAME_S_DOES_NOT_HAVE_ANY_FILE_MAPPED,groupName));
        }

        final File groupDir = groups.get(groupName.toLowerCase());

        if (null != file && file.exists() && file.canRead() && groupDir.canWrite()) {

            try {
                final File destBucketFile = Paths.get(groupDir.getCanonicalPath(), path.toLowerCase()).toFile();
                FileUtils.copyFile(file, destBucketFile);
            } catch (IOException e) {
                Logger.error(FileSystemStoragePersistenceAPIImpl.class, e.getMessage(), e);
                throw new DotDataException(e.getMessage(), e);
            }
        } else {

            throw new IllegalArgumentException("The file: " + file +
                    ", is null, does not exist can not be read or bucket: " + groupName +
                    " could not be written");
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} the group to upload
     * @param path       {@link String} path to upload the file
     * @param writerDelegate     {@link ObjectWriterDelegate} stream to upload
     * @param object     {@link Serializable} object to write into the storage
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return
     */
    @Override
    public Object pushObject(final String groupName, final String path,
            final ObjectWriterDelegate writerDelegate,
            final Serializable object, final Map<String, Serializable> extraMeta) throws DotDataException {

        if (!this.existsGroup(groupName)) {

            throw new IllegalArgumentException(String.format(
                    THE_BUCKET_NAME_S_DOES_NOT_HAVE_ANY_FILE_MAPPED,groupName));
        }

        final File groupDir = groups.get(groupName.toLowerCase());

        if (groupDir.canWrite()) {

            try {
                final File destBucketFile = Paths.get(groupDir.getCanonicalPath(),path.toLowerCase()).toFile();
                this.prepareParent(destBucketFile);

                final String compressor = Config.getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");
                try (OutputStream outputStream = FileUtil.createOutputStream(destBucketFile.toPath(), compressor)) {
                    writerDelegate.write(outputStream, object);
                    outputStream.flush();
                }
            } catch (IOException e) {
                Logger.error(FileSystemStoragePersistenceAPIImpl.class, e.getMessage(), e);
                throw new  DotDataException(e.getMessage(),e);
            }
        } else {
            throw new IllegalArgumentException("The bucket: " + groupName + " could not write");
        }

        return true;
    }

    /**
     * makes parent dir if doesn't exist
     * @param file
     */
    private void prepareParent(final File file) {

        if (!file.getParentFile().exists()) {

            file.getParentFile().mkdirs();
        }
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} the bucket to push
     * @param path       {@link String} path to push the file
     * @param file       {@link File}   the actual file
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return
     */
    @Override
    public Future<Object> pushFileAsync(final String groupName, final String path,
            final File file, final Map<String, Serializable> extraMeta) {
        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushFile(groupName, path, file, extraMeta)
        );
    }

    /**
     * {@inheritDoc}
     * @param groupName
     * @param path       {@link String} path to upload the file
     * @param writerDelegate     {@link ObjectWriterDelegate} stream to upload
     * @param object     {@link Serializable} object to write into the storage
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return
     */
    @Override
    public Future<Object> pushObjectAsync(final String groupName, final String path,
            final ObjectWriterDelegate writerDelegate, final Serializable object,
            final Map<String, Serializable> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushObject(groupName, path, writerDelegate, object, extraMeta)
        );
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} group name
     * @param path {@link String}
     * @return
     */

    @Override
    public File pullFile(final String groupName, final String path) throws DotDataException {

        if (!this.existsGroup(groupName)) {

            throw new IllegalArgumentException(String.format(
                    THE_BUCKET_NAME_S_DOES_NOT_HAVE_ANY_FILE_MAPPED, groupName));
        }
        final File clientFile;
        try {
            final File groupDir = groups.get(groupName.toLowerCase());
            if (groupDir.canRead()) {
                final File destBucketFile = Paths.get(groupDir.getCanonicalPath(), path.toLowerCase()).toFile();
                if (destBucketFile.exists()) {
                    clientFile = destBucketFile;
                } else {
                    throw new IllegalArgumentException(
                            "The group: " + destBucketFile + ", does not exists.");
                }
            } else {
                throw new IllegalArgumentException(
                        "The bucket: " + groupName + " could not be read");
            }
        } catch (IOException e) {
            Logger.error(FileSystemStoragePersistenceAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
        return clientFile;
    }
    /**
     * {@inheritDoc}
     * @param groupName {@link String}  group name to pull
     * @param path {@link String} path to pull the file
     * @param readerDelegate {@link ObjectReaderDelegate} to reads the object
     * @return
     */
    @Override
    public Object pullObject(final String groupName, final String path,
            final ObjectReaderDelegate readerDelegate) throws DotDataException {

        Object object;
        if (!this.existsGroup(groupName)) {

            throw new IllegalArgumentException(String.format(
                    THE_BUCKET_NAME_S_DOES_NOT_HAVE_ANY_FILE_MAPPED, groupName));
        }

        final File groupDir = groups.get(groupName.toLowerCase());

        if (groupDir.canRead()) {
            try {
                final File file = Paths.get(groupDir.getCanonicalPath(), path.toLowerCase()).toFile();
                if (file.exists()) {
                    final String compressor = Config.getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");
                    try (InputStream input = FileUtil.createInputStream(file.toPath(), compressor)) {
                        object = readerDelegate.read(input);
                    }
                } else {
                    throw new IllegalArgumentException("The file: " + path + ", does not exists.");
                }
            } catch (IOException e) {
                Logger.error(FileSystemStoragePersistenceAPIImpl.class, e.getMessage(), e);
                throw new DotRuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("The bucket: " + groupName + " could not read");
        }

        return object;
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} group name
     * @param path {@link String}
     * @return
     */
    @Override
    public Future<File> pullFileAsync(final String groupName, final String path) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pullFile(groupName, path)
        );
    }

    /**
     * {@inheritDoc}
     * @param groupName {@link String} group name
     * @param path {@link String}
     * @param readerDelegate {@link ObjectReaderDelegate} to reads the object
     * @return
     */
    @Override
    public Future<Object> pullObjectAsync(final String groupName, final String path,
            final ObjectReaderDelegate readerDelegate) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pullObject(groupName, path, readerDelegate)
        );
    }

    /**
     * the default root folder key
     * @return
     */
    private String getRootGroupKey(){
        return Config.getStringProperty("ROOT_GROUP_NAME", DEFAULT_ROOT).toLowerCase();
    }

    /**
     * the default root folder
     * @return
     */
    private File getRootFolder() {
        final String rootFolderPath = Config.getStringProperty("ROOT_GROUP_FOLDER_PATH",
                Paths.get(System.getProperty("user.home") + File.separator + "storage")
                        .normalize().toString());
        final File rootFolder = new File(rootFolderPath);
        if(!rootFolder.exists()){
          rootFolder.mkdirs();
        }
        return rootFolder;
    }

    /**
     * recursive files count
     * @param dirPath
     * @return
     */
    private int countFiles(final File dirPath) {
        final MutableInt count = new MutableInt(0);
        countFiles(dirPath, count);
        return count.intValue();
    }

    /**
     * recursive files count
     * @param dirPath
     * @param count
     */
    private void countFiles(final File dirPath, final MutableInt count) {
        final File[] files = dirPath.listFiles();
        if (files != null) {
            for (final File value : files) {
                count.increment();
                if (value.isDirectory()) {
                    countFiles(value, count);
                }
            }
        }
    }
}
