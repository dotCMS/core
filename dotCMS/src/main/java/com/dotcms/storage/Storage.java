package com.dotcms.storage;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Encapsulates an abstract storage, it provide the API to interact with whatever is behind the real storage:
 * it could be file system, db, s3, etc.
 *
 * An Storage follow the concept of a group which is a conceptual storage for an space (such as folder, bucket, etc, depending on the storage),
 * on file system it could be an specific folder, on db a specific type and on s3 an actual bucket.
 * @author jsanca
 */
public interface Storage {

    /**
     * Returns true if exists the group
     * @param groupName {@link String} group name
     * @return boolean
     */
    boolean existsGroup(final String groupName);

    /**
     * Returns true if the object on the path exists
     * @param groupName  {@link String}
     * @param objectPath {@link String}
     * @return boolean
     */
    boolean existsObject(String groupName, String objectPath);

    /**
     * Creates the group, returns true if ok
     * @param groupName {@link String} group name
     * @return boolean, true if ok
     */
    boolean createGroup(final String groupName);

    /**
     * Creates the group, returns true if ok
     * @param groupName    {@link String} group name
     * @param extraOptions {@link Map} depending on the implementation it might need extra options or not.
     * @return boolean, true if ok
     */
    boolean createGroup(final String groupName, final Map<String, Object> extraOptions);

    /**
     * Deletes the group
     * @param groupName {@link String} group name
     * @return int number of storages deleted
     */
    int deleteGroup(final String groupName);

    /**
     * Deletes the object path on the group
     * @param groupName {@link String} group name
     * @param path   {   @link String} object path
     * @return boolean true if deletes was ok.
     */
    boolean deleteObject(String groupName, String path);

    /**
     * List the groups, the returns a list of object since the return would depend on the implementation.
     * @return List
     */
    List<Object> listGroups();

    /**
     * Push a file to the storage, it will block until the operation is done
     * @param groupName  {@link String} the group to upload
     * @param path       {@link String} path to upload the file
     * @param file       {@link File}   the actual file
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Object pushFile(final String groupName, final String path, final File file, final Map<String, Object> extraMeta);

    /**
     * Push an object to the storage, uses a delegate to write the actual object to their own outputstream, it will block until the operation is done
     * @param groupName {@link String} the group to upload
     * @param path       {@link String} path to upload the file
     * @param writerDelegate     {@link ObjectWriterDelegate} stream to upload
     * @param object     {@link Serializable} object to write into the storage
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Object pushObject(final String groupName, final String path, final ObjectWriterDelegate writerDelegate, final Serializable object, final Map<String, Object> extraMeta);

    /**
     * Push a file to the storage, it will block until the operation is done
     * @param bucketName {@link String} the bucket to push
     * @param path       {@link String} path to push the file
     * @param file       {@link File}   the actual file
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Future<Object> pushFileAsync(final String bucketName, final String path, final File file, final Map<String, Object> extraMeta);

    /**
     * Push a stream to the storage, it will block until the operation is done
     * @param bucketName {@link String} the bucket to upload
     * @param path       {@link String} path to upload the file
     * @param writerDelegate     {@link ObjectWriterDelegate} stream to upload
     * @param object     {@link Serializable} object to write into the storage
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Future<Object> pushObjectAsync(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate, final Serializable object, final Map<String, Object> extraMeta);

    /**
     * Returns a local with the path contains on the storage, keep in mind that depending on the implementation it could be the actual file
     * or it could be a temporal file that will be deleted.
     * This will block until the file is pulled.
     * @param groupName {@link String} group name
     * @param path {@link String}
     * @return File
     */
    File pullFile (final String groupName, final String path);

    /**
     * Pull a stream from the storage and read as an object
     *
     * @param groupName {@link String}  group name to pull
     * @param path {@link String} path to pull the file
     * @param readerDelegate {@link ObjectReaderDelegate} to reads the object
     */
    Object pullObject (final String groupName, final String path, final ObjectReaderDelegate readerDelegate);

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



}
