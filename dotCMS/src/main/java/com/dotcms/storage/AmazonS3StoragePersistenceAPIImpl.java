package com.dotcms.storage;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
import com.dotcms.enterprise.publishing.storage.Storage;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.storage.repository.FileRepositoryManager;
import com.dotcms.storage.repository.HashedLocalFileRepositoryManager;
import com.dotcms.storage.repository.LocalFileRepositoryManager;
import com.dotcms.storage.repository.TempFileRepositoryManager;
import com.dotcms.util.EnterpriseFeature;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.Encryptor;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.FORWARD_SLASH;

/**
 * Provides a Metadata Provider implementation that uses AWS S3 to persist the metadata files.
 * <p>It's very important to take into consideration that any request to check for the existence of
 * a bucket or a file might take a considerable time to complete. Any sort of local caching
 * mechanism is crucial to keep the performance of this provider implementation.<p/>
 * <p>This provider can ONLY be used with an Enterprise License.</p>
 *
 * @author jsanca
 */
public class AmazonS3StoragePersistenceAPIImpl implements StoragePersistenceAPI {

    /**
     * Defines the different ways that can be used to store metadata files in the AWS S3 bucket. By
     * default, folder paths are hashed via SHA-256, but can be persisted using the same folder
     * pattern used in the dotCMS assets folder, i.e., {@code "assets/1/2/1234-1234/"}.
     */
    enum PathEncryptionMode {
        NONE, SHA256
    }

    private final Storage storage;
    private final FileRepositoryManager fileRepositoryManager = this.getFileRepository();
    private MessageDigest sha256;
    private final Set<String> groups = ConcurrentHashMap.newKeySet();
    private IdentifierStripedLock lockManager;

    private String bucketName;
    private PathEncryptionMode pathEncryptionMode;
    private static final String INVALID_LICENSE = "The Amazon S3 Metadata Provider is an Enterprise only feature";
    public static final String AWS_S3_BUCKET_NAME_PROP = "storage.file-metadata.s3.bucket-name";
    public static final String AWS_S3_REGION_PROP = "storage.file-metadata.s3.bucket-region";
    public static final String AWS_S3_ACCESS_KEY_PROP = "storage.file-metadata.s3.access-key";
    public static final String AWS_S3_SECRET_ACCESS_KEY_PROP = "storage.file-metadata.s3.secret-access-key";
    public static final String AWS_S3_PATH_ENCRYPTION_MODE_PROP = "storage.file-metadata.s3.path-encryption-mode";

    private static final String S3_BASE_STORAGE_FILE_REPO_TYPE = Config.getStringProperty(
            "S3_STORAGE_FILE_REPO_TYPE", FileRepositoryManager.TEMP_REPO).toUpperCase();

    private static final String METADATA_GROUP_NAME = Config
            .getStringProperty(StoragePersistenceProvider.METADATA_GROUP_NAME, FileMetadataAPI.DOT_METADATA);

    /**
     * Returns the file repository manager based on the configuration.
     *
     * @return The selected {@link FileRepositoryManager}.
     */
    private FileRepositoryManager getFileRepository() {
        if (S3_BASE_STORAGE_FILE_REPO_TYPE.equals(FileRepositoryManager.HASH_LOCAL_REPO)) {
            return new HashedLocalFileRepositoryManager();
        } else if (S3_BASE_STORAGE_FILE_REPO_TYPE.equals(FileRepositoryManager.LOCAL_REPO)) {
            return new LocalFileRepositoryManager();
        } else {
            return new TempFileRepositoryManager();
        }
    }

    public AmazonS3StoragePersistenceAPIImpl() {
        this.lockManager = DotConcurrentFactory.getInstance().getIdentifierStripedLock();
        // todo: this should be from an app, just need to pass the host name fallback to system
        final String accessKey = Config.getStringProperty(AWS_S3_ACCESS_KEY_PROP, null);
        final String secretAccessKey = Config.getStringProperty(AWS_S3_SECRET_ACCESS_KEY_PROP, null);
        final String region = Config.getStringProperty(AWS_S3_REGION_PROP, null);
        this.bucketName = Config.getStringProperty(AWS_S3_BUCKET_NAME_PROP, null);
        this.pathEncryptionMode =
                PathEncryptionMode.valueOf(Config.getStringProperty(AWS_S3_PATH_ENCRYPTION_MODE_PROP, PathEncryptionMode.SHA256.name()));
        this.sha256 = Try.of(() -> MessageDigest.getInstance(Encryptor.SHA256_ALGORITHM)).getOrElseThrow(e -> new DotRuntimeException(e.getMessage(), e));
        this.storage = !UtilMethods.isSet(accessKey) || !UtilMethods.isSet(secretAccessKey) ?
                new AWSS3Storage(new DefaultAWSCredentialsProviderChain()) :
                new AWSS3Storage(new AWSS3Configuration.Builder().accessKey(accessKey).secretKey(secretAccessKey).endPoint(null).region(region).build());
    }

    @SuppressWarnings("unused")
    public AmazonS3StoragePersistenceAPIImpl(final Storage storage) {
        this.storage = storage;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean existsGroup(final String groupName) throws DotDataException {
        if (this.groups.contains(groupName)) {
            return true;
        }
        boolean objectExists = this.storage.existsBucket(this.bucketName);
        if (objectExists) {
            objectExists = !this.storage.listObjects(this.bucketName,
                    METADATA_GROUP_NAME).getObjectSummaries().isEmpty();
            if (!objectExists) {
                Logger.debug(this, () -> String.format("Group '%s' does not exist", groupName));
            }
        } else {
            Logger.debug(this, () -> String.format("Bucket '%s' does not exist", this.bucketName));
        }
        this.groups.add(groupName);
        return objectExists;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean existsObject(final String groupName, final String objectPath) throws DotDataException {
        final String correctedPath = transformReadPath(groupName, objectPath);
        final boolean exists = this.storage.existsBucket(this.bucketName) && !this.storage.listObjects(this.bucketName,
                correctedPath).getObjectSummaries().isEmpty();
        Logger.debug(this, () -> String.format("Object '%s' in group '%s' exists? %s", correctedPath, groupName, exists));
        return exists;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean createGroup(final String groupName) throws DotDataException {
        if (!this.existsGroup(groupName)) {
            Logger.debug(this, () -> String.format("Creating group with name '%s'", groupName));
            this.storage.createFolder(this.bucketName, groupName);
        }
        this.groups.add(groupName);
        return true;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions) throws DotDataException {
        // Extra options Map is not being used for now
        return createGroup(groupName);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public int deleteGroup(final String groupName) throws DotDataException {
        this.storage.deleteFolder(this.bucketName, groupName);
        return 0;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {
        this.storage.deleteFile(this.bucketName, groupName + path);
        return true;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public boolean deleteObjectReference(final String groupName, final String path) throws DotDataException {
        return this.deleteObjectAndReferences(this.bucketName, groupName + path);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public List<String> listGroups() {
        return this.storage.listBuckets().stream().map(Bucket::getName).collect(Collectors.toList());
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Object pushFile(final String groupName, final String path, final File file,
                           final Map<String, Serializable> extraMeta) throws DotDataException {
        final String pathForS3 = transformWritePath(groupName, path, file.getName());
        final Upload upload = this.storage.uploadFile(this.bucketName, pathForS3,
                file);
        try {
            return lockManager.tryLock("s3_" + groupName + path, () -> {

                    Logger.debug(this, () -> String.format("Pushing file '%s' to group '%s' with " +
                            "path '%s' [ %s ]", file.getName(), groupName, pathForS3, path));
                    final UploadResult result =
                            Try.of(upload::waitForUploadResult).getOrElseThrow(e -> new DotDataException(e.getMessage(), e));
                    Logger.debug(this, () -> String.format("File '%s' in group '%s' with path '%s' " +
                            "[ %s ] was pushed successfully!", file.getName(), groupName,
                            pathForS3, path));
                    return result.getETag();

                }
            );
        } catch (final Throwable e) {
            throw new DotRuntimeException(String.format("Failed to push file '%s' to S3 group " +
                    "'%s': %s", path, groupName, ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Object pushObject(final String groupName, final String path, final ObjectWriterDelegate writerDelegate,
                             final Serializable object, final Map<String, Serializable> extraMeta) throws DotDataException {
        final File file = new File(ConfigUtils.getAssetTempPath() + path);
        try {
            this.createTempFile(writerDelegate, object, file);
            return this.pushFile(groupName, path, file, extraMeta);
        } catch (final Exception e) {
            Logger.error(this, String.format("Failed to process push of object '%s' to group " +
                    "'%s': %s", path, groupName, ExceptionUtil.getErrorMessage(e)), e);
            throw new DotDataException(e);
        } finally {
            if (!file.delete()) {
                Logger.debug(this, () -> String.format("Temp File '%s' could not be deleted", path));
            }
        }
    }

    /**
     * Creates a temporary file with the metadata object. This is the actual file that will be
     * uploaded to the AWS S3 bucket.
     *
     * @param writerDelegate The {@link ObjectWriterDelegate} that will be used to write the object
     *                       to the file.
     * @param object         The {@link Serializable} object that will be written to the file.
     * @param file           The {@link File} that will be created.
     *
     * @throws IOException An error occurred when writing the object to the file.
     */
    private void createTempFile(final ObjectWriterDelegate writerDelegate,
                                final Serializable object, final File file) throws IOException {
        final File parentFolder = file.getParentFile();
        if (parentFolder != null && !parentFolder.exists()) {
            if (!parentFolder.mkdirs()) {
                Logger.debug(this, () -> String.format("Failed to create one or more folders in " +
                        "path '%s'", parentFolder.getPath()));
            }
        }
        writeToFile(writerDelegate, object, file);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Future<Object> pushFileAsync(final String groupName, final String path, final File file, final Map<String, Serializable> extraMeta) {
        final String pathForS3 = transformWritePath(groupName, path, file.getName());
        Logger.debug(this, () -> String.format("Async pushing file '%s' to group '%s' with path " +
                "'%s' [ %s ]", file.getName(), groupName, pathForS3, path));
        final Upload upload = this.storage.uploadFile(this.bucketName, pathForS3, file);
        return new UploadFuture<>(upload, file);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Future<Object> pushObjectAsync(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate,
                                          final Serializable object, final Map<String, Serializable> extraMeta) {
        final File file = new File(ConfigUtils.getAssetPath() + path);
        return this.pushFileAsync(bucketName, path, file, extraMeta);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public File pullFile(final String groupName, final String path) throws DotDataException {
        final File file = fileRepositoryManager.getOrCreateFile(path);
        final String pathForS3 = transformReadPath(groupName, path);
        Logger.debug(this, () -> String.format("Pulling file '%s' from group '%s' with path '%s' " +
                "[ %s ]", file.getName(), groupName, pathForS3, path));
        final Download download = this.storage.downloadFile(this.bucketName, pathForS3, file);
        Try.run(download::waitForCompletion).getOrElseThrow(e-> new DotDataException(e.getMessage(), e));
        Logger.debug(this, () -> String.format("File '%s' in group '%s' with path '%s' [ %s ] " +
                "was pulled successfully!", file.getName(), groupName, pathForS3, path));
        return file;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Object pullObject(final String groupName, final String path, final ObjectReaderDelegate readerDelegate) throws DotDataException {
        Object object = null;
        final File file = pullFile(groupName, path);

        if (null != file) {
            object = Try.of(() -> {
                        try (final InputStream inputStream = Files.newInputStream(file.toPath())) {
                            return readerDelegate.read(inputStream);
                        }
                    }).getOrNull();
        }

        return object;
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Future<File> pullFileAsync(final String groupName, final String path) {
        final File file = fileRepositoryManager.getOrCreateFile(path);
        final String pathForS3 = transformReadPath(groupName, path);
        Logger.debug(this, () -> String.format("Async pulling file '%s' from group '%s' with path '%s' [ %s ]", file.getName(), groupName, pathForS3, path));
        final Download download = this.storage.downloadFile(groupName, pathForS3, file);
        return new DownloadFuture<>(download, file, aFile -> aFile);
    }

    @Override
    @EnterpriseFeature(licenseLevel = LicenseLevel.PLATFORM, errorMsg = INVALID_LICENSE)
    public Future<Object> pullObjectAsync(final String groupName, final String path, final ObjectReaderDelegate readerDelegate) {
        final File file = fileRepositoryManager.getOrCreateFile(path);
        final Download download = this.storage.downloadFile(groupName, path, file);
        final Function<File, Object> toObjectFunction = aFile -> {

            try (final InputStream inputStream = Files.newInputStream(aFile.toPath())) {
                return readerDelegate.read(inputStream);
            } catch (final Exception e) {
                throw new DotRuntimeException(String.format("Failed to async pull object '%s' " +
                        "from group '%s': %s", path, groupName, ExceptionUtil.getErrorMessage(e))
                        , e);
            }
        };

        return new DownloadFuture<>(download, file, toObjectFunction);
    }

    /**
     * Takes the asset path from a dotCMS object/file and transforms it to the path used
     * specifically for AWS S3 so that the metadata file can be read. Depending on the configuration
     * for this Metadata Provider, the path can be transformed to a SHA256 hash or left as is.
     * <p>For reading a file in S3, the path must be composed of:
     * <ul>
     *     <li>No leading slash.</li>
     *     <li>The group name at the beginning.</li>
     *     <li>The path -- either encrypted or as is -- and the file name at the end.</li>
     * </ul>
     * </p>
     *
     * @param groupName The group name, configured via the
     *                  {@link StoragePersistenceProvider#METADATA_GROUP_NAME} configuration
     *                  variable.
     * @param path      The dotCMS asset path to the metadata file.
     *
     * @return The transformed read path.
     */
    private String transformReadPath(final String groupName, final String path) {
        String s3Path;
        final String fileName = path.lastIndexOf(FORWARD_SLASH) > 0 ?
                path.substring(path.lastIndexOf(FORWARD_SLASH) + 1) : path;
        final String correctedPath = path.substring(1).replace(fileName, BLANK);
        if (this.pathEncryptionMode == PathEncryptionMode.SHA256) {
            final String hexString = EncryptorFactory.getInstance().getEncryptor().encryptString(correctedPath, this.sha256);
            s3Path = groupName + FORWARD_SLASH + hexString + FORWARD_SLASH + fileName;
        } else {
            s3Path = groupName + FORWARD_SLASH + correctedPath + fileName;
        }
        return s3Path;
    }

    /**
     * Takes the asset path from a dotCMS object/file and transforms it to the path used
     * specifically for AWS S3 so that the metadata file can be written. Depending on the
     * configuration for this Metadata Provider, the path can be transformed to a SHA256 hash or
     * left as is.
     * <p>For writing a file in S3, the path must be composed of:
     * <ul>
     *     <li>No leading slash.</li>
     *     <li>The group name at the beginning.</li>
     *     <li>The path -- either encrypted or as is -- and NO file name at the end.</li>
     * </ul>
     * </p>
     *
     * @param groupName The group name, configured via the
     *                  {@link StoragePersistenceProvider#METADATA_GROUP_NAME} configuration
     *                  variable.
     * @param path      The dotCMS asset path to the metadata file.
     * @param fileName  The name of the file that will be written to the S3 bucket.
     *
     * @return The transformed write path.
     */
    private String transformWritePath(final String groupName, final String path,
                                      final String fileName) {
        String s3Path;
        final String correctedPath = path.substring(1).replace(fileName, BLANK);
        if (this.pathEncryptionMode == PathEncryptionMode.SHA256) {
            final String hexString = EncryptorFactory.getInstance().getEncryptor().encryptString(correctedPath, this.sha256);
            s3Path = groupName + FORWARD_SLASH + hexString;
        } else {
            s3Path = groupName + FORWARD_SLASH + correctedPath.substring(0,
                    correctedPath.length() - 1);
        }
        return s3Path;
    }

    /**
     * This is a special version of the {@link Future} class that allows you to asynchronously
     * upload a file to S3 and get the ETag of the uploaded file as a result.
     *
     * @param <T> The object type that is expected to be uploaded.
     */
    private static class UploadFuture<T> implements Future<T> {

        private final Upload upload;
        private final File file;

        public UploadFuture(final Upload upload, final File file) {
            this.upload = upload;
            this.file = file;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            try {
                this.upload.abort();
                return true;
            } finally {
                deleteFile();
            }
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
        public T get() throws ExecutionException {
            try {
                final UploadResult result = Try.of(upload::waitForUploadResult).getOrElseThrow(e -> new ExecutionException(e.getMessage(), e));
                return (T) result.getETag();
            } finally {
                deleteFile();
            }
        }

        @Override
        public T get(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException, ExecutionException {
            final Callable<T> objectCallable = () -> {
                if (upload.getState() == Transfer.TransferState.Completed) {
                    try {
                        return (T) upload.waitForUploadResult().getETag();
                    } finally {
                        deleteFile();
                    }
                }
                throw new TimeoutException("S3 Upload timed out");
            };
            return DotConcurrentFactory.getScheduledThreadPoolExecutor().schedule(objectCallable, timeout, unit).get();
        }

        /**
         * Deletes the file that is trying to be uploaded. This is done in case the upload has
         * either finished correctly or incorrectly, or if the process was aborted.
         */
        private void deleteFile() {
            if (!this.file.delete()) {
                Logger.debug(this, () -> String.format("Temp File '%s' could not be deleted",
                        this.file.getName()));
            }
        }

    }

    /**
     * This is a special version of the {@link Future} class that allows you to asynchronously
     * download a file from S3 and get the file as a result.
     *
     * @param <T> The object type that is expected to be downloaded.
     */
    private static class DownloadFuture<T> implements Future<T> {

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
            Try.run(this.download::abort).getOrElseThrow(e -> new DotRuntimeException(e.getMessage(), e));
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
        public T get() throws ExecutionException {
            Try.run(download::waitForCompletion).getOrElseThrow(e -> new ExecutionException(e.getMessage(), e));
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

    @Override
    public String toString() {
        return "AmazonS3StoragePersistenceAPIImpl{" +
                "groups=" + groups +
                ", bucketName='" + bucketName + '\'' +
                '}';
    }

}
