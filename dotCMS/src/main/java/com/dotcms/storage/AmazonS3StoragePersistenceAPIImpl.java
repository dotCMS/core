package com.dotcms.storage;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
import com.dotcms.enterprise.publishing.storage.Storage;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Represents a Storage on the Amazon S3
 *  *
 *  * @author jsanca
 */
public class AmazonS3StoragePersistenceAPIImpl implements StoragePersistenceAPI {

    public static final String AWS3_REGION = "region";
    private final Storage storage;

    public AmazonS3StoragePersistenceAPIImpl() {

        final String token      = Config.getStringProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_TOKEN, null);
        final String secret     = Config.getStringProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_SECRET, null);
        final String s3Endpoint = Config.getStringProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_ENDPOINT, null);
        final String s3Region   = Config.getStringProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_REGION, null);

        this.storage  = !UtilMethods.isSet(token) || !UtilMethods.isSet(secret)?
                    new AWSS3Storage(new DefaultAWSCredentialsProviderChain()):
                    new AWSS3Storage(new AWSS3Configuration.Builder().accessKey(token).secretKey(secret)
                            .endPoint(s3Endpoint).region(s3Region).build());
    }

    public AmazonS3StoragePersistenceAPIImpl(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public boolean existsGroup(final String groupName) throws DotDataException {

        return this.storage.existsBucket(groupName);
    }

    @Override
    public boolean existsObject(final String groupName, final String objectPath) throws DotDataException {

        return this.storage.existsBucket(groupName);
    }

    @Override
    public boolean createGroup(final String groupName) throws DotDataException {

        this.storage.createBucket(groupName);
        return true;
    }

    @Override
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions) throws DotDataException {

         if (null != extraOptions && extraOptions.containsKey(AWS3_REGION)) {

             this.storage.createBucket(groupName, extraOptions.get(AWS3_REGION).toString());
         } else {

             this.storage.createBucket(groupName);
         }

        return true;
    }

    @Override
    public int deleteGroup(final String groupName) throws DotDataException {

        this.storage.deleteBucket(groupName);
        return 0; // todo: check if there is a way to determine the number of deleted objects
    }

    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {

        this.storage.deleteFile(groupName, path);
        return true;
    }

    @Override
    public boolean deleteObjectReference(final String groupName, final String path) throws DotDataException {

        this.storage.deleteFile(groupName, path);
        return true;
    }

    @Override
    public List<String> listGroups() throws DotDataException {

        return this.storage.listBuckets().stream().map(bucket -> bucket.getName()).collect(Collectors.toList());
    }

    @Override
    public Object pushFile(final String groupName, final String path, final File file, final Map<String, Serializable> extraMeta) throws DotDataException {

        final Upload upload = this.storage.uploadFile(groupName, path, file);
        final UploadResult result = Try.of(()->upload.waitForUploadResult()).getOrElseThrow(e -> new DotDataException(e.getMessage(), e));
        return result.getETag();
    }

    @Override
    public Object pushObject(final String groupName, final String path, final ObjectWriterDelegate writerDelegate,
                             final Serializable object, final Map<String, Serializable> extraMeta) throws DotDataException {

        final File file = Try.of(()->FileUtil.createTemporaryFile("aws3", "tmp"  )).getOrElseThrow(e-> new DotDataException(e.getMessage(), e));
        try (final OutputStream  outputStream = Files.newOutputStream(file.toPath())) {
            Try.run(() -> writerDelegate.write(outputStream, object)).getOrElseThrow(e -> new DotDataException(e.getMessage(), e));
        } catch (IOException e) {
            throw new DotDataException(e);
        }
        return this.pushFile(groupName, path, file, extraMeta);
    }

    @Override
    public Future<Object> pushFileAsync(String groupName, String path, File file, Map<String, Serializable> extraMeta) {
        return null;
    }

    @Override
    public Future<Object> pushObjectAsync(String bucketName, String path, ObjectWriterDelegate writerDelegate, Serializable object, Map<String, Serializable> extraMeta) {
        return null;
    }

    @Override
    public File pullFile(String groupName, String path) throws DotDataException {
        return null;
    }

    @Override
    public Object pullObject(String groupName, String path, ObjectReaderDelegate readerDelegate) throws DotDataException {
        return null;
    }

    @Override
    public Future<File> pullFileAsync(String groupName, String path) {
        return null;
    }

    @Override
    public Future<Object> pullObjectAsync(String groupName, String path, ObjectReaderDelegate readerDelegate) {
        return null;
    }
}
