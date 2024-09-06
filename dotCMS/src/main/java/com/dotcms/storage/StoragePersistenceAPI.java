package com.dotcms.storage;

import com.dotcms.enterprise.achecker.parsing.EmptyIterable;
import com.dotmarketing.exception.DotDataException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This class represents an abstract storage. It provides the API a way to interact with whatever is
 * behind the real storage: The File System, a database, an Amazon S3 bucket, a Redis server, etc.
 * <p>A Storage Provider follows the concept of a group, which is a conceptual storage for a space
 * -- i.e.;  a folder, bucket, etc., depending on the specific storage. In the File System, it could
 * be a specific folder. In database, it could be a specific type. And in an Amazon AWS S3, it could
 * be the actual bucket.</p>
 *
 * @author jsanca
 */
public interface StoragePersistenceAPI {

    String HASH_OBJECT = "hashObject";
    String HASH_REF = "hashRef";
    String STORAGE_POOL = "StoragePool";

    /**
     * Returns true if the group exists on all storages
     * @param groupName {@link String} group name
     * @return boolean
     */
    boolean existsGroup(final String groupName) throws DotDataException;

    /**
     * Returns true if the object on the path exists
     * @param groupName  {@link String}
     * @param objectPath {@link String}
     * @return boolean
     */
    boolean existsObject(String groupName, String objectPath) throws DotDataException;

    /**
     * Creates the group, returns true if ok
     * @param groupName {@link String} group name
     * @return boolean, true if ok
     */
    boolean createGroup(final String groupName) throws DotDataException;

    /**
     * Creates the group, returns true if ok
     * @param groupName    {@link String} group name
     * @param extraOptions {@link Map} depending on the implementation it might need extra options or not.
     * @return boolean, true if ok
     */
    boolean createGroup(final String groupName, final Map<String, Object> extraOptions)
            throws DotDataException;

    /**
     * Deletes the group
     * @param groupName {@link String} group name
     * @return int number of storages deleted
     */
    int deleteGroup(final String groupName) throws DotDataException;

    /**
     * Deletes the object and all references bound to the path on the group
     * @param groupName {@link String} group name
     * @param path   {   @link String} object path
     * @return boolean true if deletes was ok.
     */
    boolean deleteObjectAndReferences(String groupName, String path) throws DotDataException;

    /**
     * if the Repo handles multiple references to an object via different paths this will only remove the current reference.
     * @param groupName
     * @param path
     * @return
     * @throws DotDataException
     */
    boolean deleteObjectReference(String groupName, String path) throws DotDataException;

    /**
     * List the groups, the returns a list of object since the return would depend on the implementation.
     * @return List
     */
    List<String> listGroups() throws DotDataException;

    /**
     * Push a file to the storage, it will block until the operation is done
     * @param groupName  {@link String} the group to upload
     * @param path       {@link String} path to upload the file
     * @param file       {@link File}   the actual file
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Object pushFile(final String groupName, final String path, final File file, final Map<String, Serializable> extraMeta)
            throws DotDataException;

    /**
     * Push an object to the storage, uses a delegate to write the actual object to their own outputstream, it will block until the operation is done
     * @param groupName {@link String} the group to upload
     * @param path       {@link String} path to upload the file
     * @param writerDelegate     {@link ObjectWriterDelegate} stream to upload
     * @param object     {@link Serializable} object to write into the storage
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Object pushObject(final String groupName, final String path, final ObjectWriterDelegate writerDelegate, final Serializable object, final Map<String, Serializable> extraMeta)
            throws DotDataException;

    /**
     * Pushes a file to this storage provider. It will NOT block the current operation as a
     * different thread will be used to do the push.
     *
     * @param groupName {@link String} the bucket to push
     * @param path      {@link String} path to push the file
     * @param file      {@link File}   the actual file
     * @param extraMeta {@link Map} optional metadata, this could be null but depending on the
     *                  implementation it would need some meta info.
     *
     * @return Object, returns an object since the result will depend
     */
    Future<Object> pushFileAsync(final String groupName, final String path, final File file, final Map<String, Serializable> extraMeta);

    /**
     * Pushes a stream to this storage provider. It will NOT block the current operation as a
     * different thread will be used to do the push.
     *
     * @param bucketName     {@link String} the bucket to upload
     * @param path           {@link String} path to upload the file
     * @param writerDelegate {@link ObjectWriterDelegate} stream to upload
     * @param object         {@link Serializable} object to write into the storage
     * @param extraMeta      {@link Map} optional metadata, this could be null but depending on the
     *                       implementation it would need some meta info.
     *
     * @return Object, returns an object since the result will depend
     */
    Future<Object> pushObjectAsync(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate, final Serializable object, final Map<String, Serializable> extraMeta);

    /**
     * Returns a local with the path contains on the storage, keep in mind that depending on the implementation it could be the actual file
     * or it could be a temporal file that will be deleted.
     * This will block until the file is pulled.
     * @param groupName {@link String} group name
     * @param path {@link String}
     * @return File
     */
    File pullFile (final String groupName, final String path) throws DotDataException;

    /**
     * Pull a stream from the storage and read as an object
     *
     * @param groupName {@link String}  group name to pull
     * @param path {@link String} path to pull the file
     * @param readerDelegate {@link ObjectReaderDelegate} to reads the object
     */
    Object pullObject (final String groupName, final String path, final ObjectReaderDelegate readerDelegate)
            throws DotDataException;

    /**
     * Returns a local with the path contains on the storage, keep in mind that depending on the implementation it could be the actual file
     * or it could be a temporal file that will be deleted.
     *
     * @param groupName {@link String} group name
     * @param path {@link String}
     * @return Future File, the future will return the file when done
     */
    Future<File> pullFileAsync (final String groupName, final String path);

    /**
     * Returns a local with the path contains on the storage, keep in mind that depending on the implementation it could be the actual file
     * or it could be a temporal file that will be deleted.
     *
     * @param groupName {@link String} group name
     * @param path {@link String}
     * @param readerDelegate {@link ObjectReaderDelegate} to reads the object
     * @return Future File, the future will return the file when done
     */
    Future<Object> pullObjectAsync (final String groupName, final String path, final ObjectReaderDelegate readerDelegate);

    /**
     * Returns an iterable object with all the paths and objects from the specified group. This is
     * completely specific to the implementation of the Storage Provider, so it needs to be
     * developed as required. If your Provider doesn't implement it, an empty Iterable will be
     * returned.
     *
     * @param group The group name.
     *
     * @return The {@link Iterable} with the group objects.
     */
    default Iterable<? extends ObjectPath> toIterable(String group) {
        return new EmptyIterable<>();
    }

    /**
     * Takes the information from the object and writes it to the specified file using the Writer
     * Delegate to do so. This way, there's no in-memory loading.
     *
     * @param writerDelegate The {@link ObjectWriterDelegate} to writes the contents of the object.
     * @param object         The {@link Serializable} object to write.
     * @param file           The {@link File} that will contain the contents of the object.
     *
     * @throws IOException An error occurred when writing the file.
     */
    default void writeToFile(final ObjectWriterDelegate writerDelegate,
                             final Serializable object, File file) throws IOException {
        try (final OutputStream outputStream = Files.newOutputStream(file.toPath())) {
            writerDelegate.write(outputStream, object);
        }
    }

}
