package com.dotcms.storage;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
import com.dotcms.enterprise.publishing.storage.Storage;
import com.dotcms.storage.repository.FileRepositoryManager;
import com.dotcms.storage.repository.HashedLocalFileRepositoryManager;
import com.dotcms.storage.repository.LocalFileRepositoryManager;
import com.dotcms.storage.repository.TempFileRepositoryManager;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a Storage on the Amazon S3
 *  *
 *  * @author jsanca
 */
public class AmazonS3StoragePersistenceAPIImpl implements StoragePersistenceAPI {

    public static final String AWS3_REGION = "region";
    private final Storage storage;

    private final FileRepositoryManager fileRepositoryManager = this.getFileRepository();
    private static final String S3_BASE_STORAGE_FILE_REPO_TYPE = Config.getStringProperty(
            "S3_STORAGE_FILE_REPO_TYPE", FileRepositoryManager.TEMP_REPO).toUpperCase();

    private FileRepositoryManager getFileRepository() {

        switch (S3_BASE_STORAGE_FILE_REPO_TYPE) {

            case FileRepositoryManager.HASH_LOCAL_REPO:
                return new HashedLocalFileRepositoryManager();
            case FileRepositoryManager.LOCAL_REPO:
                return new LocalFileRepositoryManager();
        }

        return new TempFileRepositoryManager();
    }
    public AmazonS3StoragePersistenceAPIImpl() {
        // todo: this should be from an app, just need to pass the host name fallback to system
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
    public Future<Object> pushFileAsync(final String groupName, final String path, final File file, final Map<String, Serializable> extraMeta) {

        final Upload upload = this.storage.uploadFile(groupName, path, file);
        return new UploadFuture(upload);
    }

    @Override
    public Future<Object> pushObjectAsync(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate,
                                          final Serializable object, final Map<String, Serializable> extraMeta) {

        final File file = Try.of(()->FileUtil.createTemporaryFile("aws3", "tmp"  )).getOrElseThrow(e-> new DotRuntimeException(e.getMessage(), e));
        try (final OutputStream  outputStream = Files.newOutputStream(file.toPath())) {
            Try.run(() -> writerDelegate.write(outputStream, object)).getOrElseThrow(e -> new DotRuntimeException(e.getMessage(), e));
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
        return this.pushFileAsync(bucketName, path, file, extraMeta);
    }

    @Override
    public File pullFile(final String groupName, final String path) throws DotDataException {

        final File file = fileRepositoryManager.getOrCreateFile(path);
        final Download download = this.storage.downloadFile(groupName, path, file);
        Try.run(()->download.waitForCompletion()).getOrElseThrow(e-> new DotDataException(e.getMessage(), e));
        return file;
    }

    @Override
    public Object pullObject(final String groupName, final String path, final ObjectReaderDelegate readerDelegate) throws DotDataException {

        Object object = null;
        final File file = pullFile(groupName, path);

        if (null != file) {
            object =
                    Try.of(() -> {
                                try (InputStream inputStream = Files.newInputStream(file.toPath())) {

                                    return readerDelegate.read(inputStream);
                                }
                            }
                    ).getOrNull();
        }

        return object;
    }

    @Override
    public Future<File> pullFileAsync(final String groupName, final String path) {

        final File file = fileRepositoryManager.getOrCreateFile(path);
        final Download download = this.storage.downloadFile(groupName, path, file);
        return new DownloadFuture<>(download, file, aFile -> aFile);
    }

    @Override
    public Future<Object> pullObjectAsync(final String groupName, final String path, final ObjectReaderDelegate readerDelegate) {

        final File file = fileRepositoryManager.getOrCreateFile(path);
        final Download download = this.storage.downloadFile(groupName, path, file);
        final Function<File, Object> toObjectFunction = (aFile) -> {
            try (InputStream inputStream = Files.newInputStream(aFile.toPath())) {

                return readerDelegate.read(inputStream);
            } catch (Exception e) {
                throw new DotRuntimeException(e.getMessage(), e);
            }
        };

        return new DownloadFuture<>(download, file, toObjectFunction);
    }



    private class UploadFuture<T> implements Future<T> {

        private final Upload upload;

        public UploadFuture(final Upload upload) {
            this.upload = upload;
        }
        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            this.upload.abort();
            return true;
        }

        @Override
        public boolean isCancelled() {
            return this.upload.getState() == Transfer.TransferState.Canceled;
        }

        @Override
        public boolean isDone() {
            return this.upload.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {

            final UploadResult result = Try.of(()->upload.waitForUploadResult()).getOrElseThrow(e -> new ExecutionException(e.getMessage(), e));
            return (T)result.getETag();
        }

        @Override
        public T get(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            final Callable<T> objectCallable = () -> {
                if (upload.getState() == Transfer.TransferState.Completed) {
                    return (T)upload.waitForUploadResult().getETag();
                }
                throw new TimeoutException("S3 Upload timed out");
            };
            return DotConcurrentFactory.getScheduledThreadPoolExecutor().schedule(objectCallable, timeout, unit).get();
        }
    }

    private class DownloadFuture<T> implements Future<T> {

        private final Download download;
        private final File file;
        private final Function<File, T> futureFunctionResult;

        public DownloadFuture(final Download download, final File file, final Function<File, T> futureResult) {
            this.download = download;
            this.file     = file;
            this.futureFunctionResult = futureResult;
        }
        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            Try.run(()->this.download.abort()).getOrElseThrow(e -> new DotRuntimeException(e.getMessage(), e));
            return true;
        }

        @Override
        public boolean isCancelled() {
            return this.download.getState() == Transfer.TransferState.Canceled;
        }

        @Override
        public boolean isDone() {
            return this.download.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {

            Try.run(()-> download.waitForCompletion()).getOrElseThrow(e -> new ExecutionException(e.getMessage(), e));
            return this.futureFunctionResult.apply(file);
        }

        @Override
        public T get(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            final Callable<T> objectCallable = () -> {
                if (download.getState() == Transfer.TransferState.Completed) {
                    return this.futureFunctionResult.apply(file);
                }
                throw new TimeoutException("S3 Upload timed out");
            };

            return DotConcurrentFactory.getScheduledThreadPoolExecutor().schedule(objectCallable, timeout, unit).get();
        }
    }
}
