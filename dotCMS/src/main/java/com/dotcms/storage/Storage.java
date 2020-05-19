package com.dotcms.storage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Encapsulates an abstract storage, it provide the API to interact with whatever is behind the real storage:
 * it could be file system, db, s3, etc.
 *
 * An Storage follow the concept of a bucket which is a conceptual storage for an space,
 * on file system it could be an specific folder, on db a specific type and on s3 an actual bucket.
 * @author jsanca
 */
public interface Storage {

    /**
     * Returns true if exists the bucket
     * @param bucketName {@link String} bucket name
     * @return boolean
     */
    boolean existsBucket(final String bucketName);

    /**
     * Returns true if the object on the path exists
     * @param bucket     {@link String}
     * @param objectPath {@link String}
     * @return boolean
     */
    boolean existsObject(String bucket, String objectPath);

    /**
     * Creates the bucket, returns true if ok
     * @param bucketName {@link String} bucket name
     * @return boolean, true if ok
     */
    boolean createBucket(final String bucketName);

    /**
     * Creates the bucket, returns true if ok
     * @param bucketName {@link String} bucket name
     * @param extraOptions {@link Map} depending on the implementation it might need extra options or not.
     * @return boolean, true if ok
     */
    boolean createBucket(final String bucketName, final Map<String, Object> extraOptions);

    /**
     * Deletes the bucket
     * @param bucketName {@link String} bucket name
     * @return boolean true if deletes was ok.
     */
    boolean deleteBucket(final String bucketName);

    /**
     * Deletes the object path on the bucket
     * @param bucket {@link String} bucket name
     * @param path   {@link String} object path
     * @return boolean true if deletes was ok.
     */
    boolean deleteObject(String bucket, String path);

    /**
     * List the buckets, the returns a list of object since the return would depend on the implementation.
     * @return List
     */
    List<Object> listBuckets();

    /**
     * Push a file to the storage, it will block until the operation is done
     * @param bucketName {@link String} the bucket to upload
     * @param path       {@link String} path to upload the file
     * @param file       {@link File}   the actual file
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Object pushFile(final String bucketName, final String path, final File file, final Map<String, Object> extraMeta);

    /**
     * Push an object to the storage, uses a delegate to write the actual object to their own outputstream, it will block until the operation is done
     * @param bucketName {@link String} the bucket to upload
     * @param path       {@link String} path to upload the file
     * @param writerDelegate     {@link ObjectWriterDelegate} stream to upload
     * @param object     {@link Object} object to write into the storage
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Object pushObject(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate, final Object object, final Map<String, Object> extraMeta);

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
     * @param object     {@link Object} object to write into the storage
     * @param extraMeta  {@link Map} optional metadata, this could be null but depending on the implementation it would need some meta info.
     * @return Object, returns an object since the result will depend
     */
    Future<Object> pushObjectAsync(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate, final Object object, final Map<String, Object> extraMeta);

    /**
     * Returns a local with the path contains on the storage, keep in mind that depending on the implementation it could be the actual file
     * or it could be a temporal file that will be deleted.
     * This will block until the file is pulled.
     * @param bucketName {@link String} bucket name
     * @param path {@link String}
     * @return File
     */
    File pullFile (final String bucketName, final String path);

    /**
     * Pull a stream from the storage and read as an object
     *
     * @param bucketName {@link String}  bucket name to pull
     * @param path {@link String} path to pull the file
     * @param readerDelegate {@link ObjectReaderDelegate} to reads the object
     */
    Object pullObject (final String bucketName, final String path, final ObjectReaderDelegate readerDelegate);

    /**
     * Returns a local with the path contains on the storage, keep in mind that depending on the implementation it could be the actual file
     * or it could be a temporal file that will be deleted.
     *
     * @param bucketName {@link String} bucket name
     * @param path {@link String}
     * @return Future File, the future will return the file when done
     */
    Future<File> pullFileAsync (final String bucketName, final String path);

    /**
     * Returns a local with the path contains on the storage, keep in mind that depending on the implementation it could be the actual file
     * or it could be a temporal file that will be deleted.
     *
     * @param bucketName {@link String} bucket name
     * @param path {@link String}
     * @param readerDelegate {@link ObjectReaderDelegate} to reads the object
     * @return Future File, the future will return the file when done
     */
    Future<Object> pullObjectAsync (final String bucketName, final String path, final ObjectReaderDelegate readerDelegate);



}
