package com.dotcms.storage;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.enterprise.achecker.parsing.EmptyIterable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Represents a Storage Provider base on the File System. The groups used by this implementation are
 * folders that have been previously created. You can add more of them via the
 * {@link #addGroupMapping(String, File)}.
 * <p>By default, the API loads up and maps a root folder, which can be overridden by a
 * configuration property named {@code ROOT_GROUP_FOLDER_PATH}. Any new group created will result in
 * a new folder under such a root folder.</p>
 *
 * @author jsanca
 */
public class FileSystemStoragePersistenceAPIImpl implements StoragePersistenceAPI {

    private static final String DEFAULT_ROOT = "root";
    private static final String THE_BUCKET_NAME_S_DOES_NOT_HAVE_ANY_FILE_MAPPED = "The bucketName: `%s`, does not have any files mapped";
    private static final String STORAGE_POOL = "StoragePool";

    private final Map<String, File> groups = new ConcurrentHashMap<>();

    private final Lazy<String> contentMetadataCompressor = Lazy.of(()->Config.getStringProperty("CONTENT_METADATA_COMPRESSOR", "none"));

    private final IdentifierStripedLock lockManager;

    /**
     * default constructor
     */
    FileSystemStoragePersistenceAPIImpl() {
        final String rootGroupKey = getRootGroupKey();
        final File rootFolder = getRootFolder();
        groups.put(rootGroupKey, rootFolder);
        Logger.debug(FileSystemStoragePersistenceAPIImpl.class, () -> String
                .format("Default root group '%s' is currently mapped to folder '%s' ", rootGroupKey,
                        rootFolder));
        lockManager = DotConcurrentFactory.getInstance().getIdentifierStripedLock();
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
                    "Folder '%s' cannot be mapped to group '%s' as it is not a directory, doesn't exist, or doesn't have write permissions",
                    folder, groupName));
        }
        groups.put(groupName.toLowerCase(), folder);
        Logger.debug(this, () -> String.format("Registering new group '%s' mapped to folder '%s' ",groupName, folder));
    }

    @Override
    public boolean existsGroup(final String groupName) throws DotDataException{
        final String groupNameLC = groupName.toLowerCase();
        return groups.containsKey(groupNameLC) && this.groups.get(groupNameLC).exists();
    }

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

    @Override
    public boolean createGroup(final String groupName) throws DotDataException {
        return this.createGroup(groupName, ImmutableMap.of());
    }

    @Override
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        final File groupNamePath = this.groups.get(groupNameLC);
        if (null != groupNamePath && groupNamePath.exists() && groupNamePath.isAbsolute()) {
            return true;
        }
        // If the group name DOES NOT represent an absolute path, it must be
        // appended to the path of the root group
        final File rootGroup = this.groups.get(getRootGroupKey());
        final File destBucketFile = new File(rootGroup, groupNameLC);
        if (!destBucketFile.exists()) {
            final boolean bucketCreated = destBucketFile.mkdirs();
            if (bucketCreated) {
               this.groups.put(groupNameLC, destBucketFile);
            }
            return bucketCreated;
        }
        this.groups.put(groupNameLC, destBucketFile);
        return true; // the bucket already exist
    }

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

    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {
        return new File(groups.get(groupName.toLowerCase()), path.toLowerCase()).delete();
    }

    @Override
    public boolean deleteObjectReference(String groupName, String path) throws DotDataException {
        return deleteObjectAndReferences(groupName, path);
    }

    @Override
    public List<String> listGroups() {

        return new ImmutableList.Builder<String>().addAll(groups.keySet()).build();
    }

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
                return lockManager.tryLock("fs_" + groupName + path,
                        () -> {

                            final File destBucketFile = Paths.get(groupDir.getCanonicalPath(),path.toLowerCase()).toFile();

                            // someone else already wrote the file meanwhile waiting for the lock
                            // so, I do not need to write it again
                            if (!destBucketFile.exists()) {

                                try {

                                    final File tempDestBucketFile = com.dotmarketing.util.FileUtil.
                                            createTemporaryFile(String.valueOf((groupName+path).hashCode()));
                                    final String compressor = contentMetadataCompressor.get();
                                    try (OutputStream outputStream = FileUtil.createOutputStream(tempDestBucketFile.toPath(), compressor)) {
                                        writerDelegate.write(outputStream, object);
                                        outputStream.flush();
                                    }

                                    // copy from the temp file to the final destination
                                    this.prepareParent(destBucketFile);

                                    FileUtil.move(tempDestBucketFile, destBucketFile);
                                } catch (IOException e) {
                                    Logger.error(FileSystemStoragePersistenceAPIImpl.class, e.getMessage(), e);
                                    throw new DotDataException(e.getMessage(), e);
                                }
                            }

                            return true;
                        }
                );
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException(String.format("Bucket '%s' could not be created", groupName));
        }
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

    @Override
    public Future<Object> pushFileAsync(final String groupName, final String path,
            final File file, final Map<String, Serializable> extraMeta) {
        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushFile(groupName, path, file, extraMeta)
        );
    }

    @Override
    public Future<Object> pushObjectAsync(final String groupName, final String path,
            final ObjectWriterDelegate writerDelegate, final Serializable object,
            final Map<String, Serializable> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushObject(groupName, path, writerDelegate, object, extraMeta)
        );
    }

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
            File file = null;

            try {
                file = Paths.get(groupDir.getCanonicalPath(), path.toLowerCase()).toFile();
                if (file.exists()) {
                    final String compressor = Config.getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");
                    try (InputStream input = FileUtil.createInputStream(file.toPath(), compressor)) {
                        object = readerDelegate.read(input);
                    }
                } else {
                    throw new IllegalArgumentException("The file: " + path + ", does not exists.");
                }
            } catch (MismatchedInputException e) {
                final String msg = "error getting: (" + groupName + '|' + path +
                        "), probably the file is zero length or corrupted, msg:" + e.getMessage();
                Logger.warn(FileSystemStoragePersistenceAPIImpl.class, msg, e);
                if (null != file && file.exists()) {
                    Logger.info(this, "Deleting the file:" + file + ", because it is corrupted.");
                    file.delete();
                }
                object = Map.of(); // no object
            } catch (IOException e) {
                final String msg = "error getting: (" + groupName + '|' + path + "), msg:" + e.getMessage();
                Logger.error(FileSystemStoragePersistenceAPIImpl.class, msg, e);
                throw new DotRuntimeException(msg, e);
            }
        } else {
            throw new IllegalArgumentException(String.format("The folder: `%s` mapped by the bucket: `%s` could not be read.",groupDir.getAbsolutePath(), groupName));
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

    @Override
    public Iterable<? extends ObjectPath> toIterable(final String group) {

        final File destBucketFile = this.groups.get(group.toLowerCase());
        if (destBucketFile.exists() && destBucketFile.isDirectory()) {

            final Iterable<? extends ObjectPath> ite = Try.of(()->new FilesIterable(destBucketFile, group)).getOrNull();
            return ite != null ? ite : new EmptyIterable<>();
        }

        return new EmptyIterable<>();
    }

    public class FilesIterable implements Iterable<ObjectPath> {

        private final Stream<Path> stream;
        private final File destBucketFile;

        private final String groupName;

        public FilesIterable(final File destBucketFile, final String groupName) throws IOException {
            this.groupName      = groupName;
            this.destBucketFile = destBucketFile;
            this.stream = Files.walk(destBucketFile.toPath());
        }

        @NotNull
        @Override
        public Iterator<ObjectPath> iterator() {

            return new ObjectPathIterator(stream.iterator(), destBucketFile, groupName);
        }
    } // FilesIterable.

    /**
     * This is a simple Iterator-based class that allows you to traverse the files in a given
     * FS location or bucket.
     */
    class ObjectPathIterator implements Iterator<ObjectPath> {

        private final File destBucketFile;
        private final String groupName;
        private Path currentElement = null;
        private final Iterator<Path> iterator;

        public ObjectPathIterator(final Iterator<Path> iterator, final File destBucketFile, final String groupName) {
            this.groupName      = groupName;
            this.destBucketFile = destBucketFile;
            this.iterator       = iterator;
        }

        @Override
        public boolean hasNext() {
            return null != nextFile();
}

        @Override
        public ObjectPath next() {

            ObjectPath objectPath = null;

            if (null == currentElement) {
                nextFile();
            }

            if (null != currentElement) {

                final Path path = this.currentElement;
                final String absolutePath = path.toFile().getAbsolutePath();
                final String relativePath = absolutePath.substring(this.destBucketFile.getAbsolutePath().length() + 1);
                objectPath = Try.of(()->new ObjectPath(relativePath,
                        FileSystemStoragePersistenceAPIImpl.this.pullFile(groupName, relativePath))).getOrNull();
            }

            return objectPath;
        }

        private Path nextFile() {
            if (this.iterator.hasNext()) {

                Path path = iterator.next();
                // if we have to find the fist file
                while(path.toFile().isDirectory() && this.iterator.hasNext()) {
                    path = iterator.next();
                }
                currentElement = path.toFile().isFile()?path: null;
                return currentElement;
            }

            currentElement = null;
            return null;
        }

    } // ObjectPathIterator.

    @Override
    public String toString() {
        return "FileSystemStoragePersistenceAPIImpl{" +
                "groups=" + groups +
                '}';
    }

}
